# Опциональные улучшения

## 1. Обработка запросов с 5000+ ID

### Проблема
При обработке более 1000 ID в одном запросе возникают следующие проблемы:
- HTTP соединение может оборваться по таймауту (обычно 30-60 секунд)
- Большая нагрузка на память при обработке
- Долгое выполнение транзакций в БД
- Возможность частичного успеха сложно отследить

### Решение 1: Асинхронная обработка с Job ID

```java
@PostMapping("/documents/bulk-approve-async")
public ResponseEntity<JobResponse> bulkApproveAsync(@RequestBody BulkActionRequest request) {
    String jobId = UUID.randomUUID().toString();
    
    // Сохраняем задачу в БД или Redis
    jobService.submitJob(jobId, request);
    
    // Отправляем в очередь на обработку
    messageQueue.send(new BulkApproveJob(jobId, request));
    
    return ResponseEntity.accepted(new JobResponse(jobId, "Job submitted"));
}

@GetMapping("/documents/job/{jobId}")
public ResponseEntity<JobResult> getJobResult(@PathVariable String jobId) {
    JobResult result = jobService.getResult(jobId);
    if (result == null) {
        return ResponseEntity.accepted(new JobResponse(jobId, "Still processing"));
    }
    return ResponseEntity.ok(result);
}

Решение 2: Chunking (разбиение на пачки)
public List<ApproveResult> approveLargeList(List<Long> ids, String userId) {
    List<ApproveResult> allResults = new ArrayList<>();
    
    // Разбиваем на пачки по 100 ID
    int batchSize = 100;
    for (int i = 0; i < ids.size(); i += batchSize) {
        int end = Math.min(i + batchSize, ids.size());
        List<Long> chunk = ids.subList(i, end);
        
        // Обрабатываем пачку
        allResults.addAll(approveDocuments(chunk, userId));
    }
    return allResults;
}

Решение 3: Оптимизация БД
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50  # Увеличиваем пул соединений
      connection-timeout: 60000  # Увеличиваем таймаут до 60 секунд
  
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50  # Batch insert для истории
        order_inserts: true
        order_updates: true

Рекомендация для 5000+ ID

Использовать комбинацию подходов:
1.Асинхронная обработка с Job ID для немедленного ответа
2.Chunking внутри асинхронного обработчика
3.Batch операции в БД для скорости
4.WebSocket или polling для получения результата

2. Вынос реестра утверждений в отдельную систему

Архитектурные варианты
Вариант 1: Отдельная БД (Database per Service)
# application.yml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/document_flow}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}
    
registry:
  datasource:
    url: jdbc:postgresql://registry-host:5432/approval_registry
    username: registry_user
    password: ${REGISTRY_PASSWORD}
@Configuration
public class RegistryDataSourceConfig {
    
    @Bean(name = "registryDataSource")
    @ConfigurationProperties(prefix = "registry.datasource")
    public DataSource registryDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean(name = "registryEntityManager")
    public LocalContainerEntityManagerFactoryBean registryEntityManager(
            EntityManagerFactoryBuilder builder,
            @Qualifier("registryDataSource") DataSource dataSource) {
        
        return builder
                .dataSource(dataSource)
                .packages(ApprovalRegistry.class)
                .persistenceUnit("registry")
                .build();
    }
    
    @Bean(name = "registryTransactionManager")
    public PlatformTransactionManager registryTransactionManager(
            @Qualifier("registryEntityManager") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory.getObject());
    }
}

Вариант 2: Отдельный HTTP-сервис (Microservice)

@Service
@Slf4j
public class RegistryServiceClient {
    
    private final RestTemplate restTemplate;
    private final String registryUrl;
    
    public RegistryServiceClient(@Value("${registry.service.url}") String registryUrl) {
        this.restTemplate = new RestTemplate();
        this.registryUrl = registryUrl;
    }
    
    public boolean registerApproval(Long documentId, String userId) {
        try {
            RegistryRequest request = new RegistryRequest(documentId, userId);
            
            ResponseEntity<RegistryResponse> response = restTemplate.postForEntity(
                registryUrl + "/api/registry/entries",
                request,
                RegistryResponse.class
            );
            
            return response.getStatusCode() == HttpStatus.CREATED;
            
        } catch (RestClientException e) {
            log.error("Failed to call registry service for document {}", documentId, e);
            return false;
        }
    }
    
    @Retryable(
        value = {NetworkException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public boolean registerWithRetry(Long documentId, String userId) {
        return registerApproval(documentId, userId);
    }
}

Обеспечение транзакционности (Saga Pattern)
При выносе реестра теряется транзакционность БД. Решение - паттерн Saga:

Choreography (через события)

1. Document Service утверждает документ (НО НЕ КОММИТИТ)
2. Публикует событие DocumentApprovedEvent в Kafka/RabbitMQ
3. Registry Service получает событие и создаёт запись
4. Registry Service публикует RegistryEntryCreatedEvent
5. Document Service получает событие и коммитит транзакцию
6. При ошибке RegistryService публикует RegistryFailedEvent
7. Document Service откатывает статус документа

Orchestration (через оркестратор)

@Component
public class ApprovalOrchestrator {
    
    @Transactional
    public void approveDocument(Long documentId, String userId) {
        // Шаг 1: Создаём сагу
        String sagaId = startSaga("APPROVE_DOCUMENT");
        
        try {
            // Шаг 2: Обновляем статус документа (не коммитим!)
            documentService.prepareApprove(documentId, userId);
            
            // Шаг 3: Вызываем registry service
            boolean registrySuccess = registryClient.registerApproval(documentId, userId);
            
            if (!registrySuccess) {
                throw new RegistryException("Failed to register");
            }
            
            // Шаг 4: Коммитим обе транзакции
            completeSaga(sagaId);
            
        } catch (Exception e) {
            // Шаг 5: Откат
            rollbackSaga(sagaId);
            documentService.rollbackApprove(documentId);
            throw e;
        }
    }
}

# application.yml
registry:
  service:
    url: http://registry-service:8081
    timeout: 5000
    retry:
      max-attempts: 3
      backoff-delay: 1000
    circuit-breaker:
      failure-threshold: 5
      timeout: 10000

Мониторинг

@Component
public class RegistryHealthIndicator implements HealthIndicator {
    
    private final RegistryServiceClient registryClient;
    
    @Override
    public Health health() {
        try {
            boolean isHealthy = registryClient.ping();
            if (isHealthy) {
                return Health.up().build();
            }
            return Health.down().withDetail("error", "Registry unavailable").build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}