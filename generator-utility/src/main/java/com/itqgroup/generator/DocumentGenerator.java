package com.itqgroup.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class DocumentGenerator {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static String baseUrl;
    private static int totalDocuments;
    private static int batchSize;
    private static int threads;
    private static String authorsFile;

    public static void main(String[] args) {
        if (args.length < 1) {
            log.error("Usage: java -jar document-generator.jar <config-file>");
            System.exit(1);
        }

        try {
            // Загружаем конфигурацию
            loadConfig(args[0]);

            List<String> authors = loadAuthors(authorsFile);

            log.info("=========================================");
            log.info("Document Generator Started");
            log.info("=========================================");
            log.info("Target URL: {}", baseUrl);
            log.info("Total documents to create: {}", totalDocuments);
            log.info("Batch size: {}", batchSize);
            log.info("Threads: {}", threads);
            log.info("Authors loaded: {}", authors.size());
            log.info("=========================================");

            // Создаем пул потоков
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            AtomicInteger createdCount = new AtomicInteger(0);
            AtomicInteger failedCount = new AtomicInteger(0);

            long startTime = System.currentTimeMillis();

            // Запускаем генерацию
            for (int i = 0; i < totalDocuments; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        String author = authors.get(index % authors.size());
                        String title = generateTitle(index);

                        boolean success = createDocument(author, title);

                        if (success) {
                            int created = createdCount.incrementAndGet();
                            if (created % 10 == 0 || created == totalDocuments) {
                                log.info("Progress: {}/{} documents created ({}%)",
                                        created, totalDocuments,
                                        (created * 100 / totalDocuments));
                            }
                        } else {
                            failedCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        log.error("Error creating document {}: {}", index, e.getMessage());
                        failedCount.incrementAndGet();
                    }
                });
            }

            // Завершаем работу
            executor.shutdown();
            boolean terminated = executor.awaitTermination(10, TimeUnit.MINUTES);

            long duration = System.currentTimeMillis() - startTime;

            log.info("=========================================");
            log.info("Generation completed");
            log.info("Total time: {} ms", duration);
            log.info("Documents created: {}", createdCount.get());
            log.info("Documents failed: {}", failedCount.get());
            log.info("Average speed: {} docs/sec",
                    createdCount.get() / (duration / 1000.0));
            log.info("=========================================");

        } catch (Exception e) {
            log.error("Fatal error: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    private static void loadConfig(String configFile) throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            props.load(fis);
        }

        baseUrl = props.getProperty("service.url", "http://localhost:8080");
        totalDocuments = Integer.parseInt(props.getProperty("total.documents", "100"));
        batchSize = Integer.parseInt(props.getProperty("batch.size", "10"));
        threads = Integer.parseInt(props.getProperty("threads", "5"));
        authorsFile = props.getProperty("authors.file", "authors.txt");
    }

    private static List<String> loadAuthors(String filename) throws IOException {
        List<String> authors = new ArrayList<>();
        try (var reader = java.nio.file.Files.newBufferedReader(Path.of(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    authors.add(line.trim());
                }
            }
        }

        if (authors.isEmpty()) {
            // Если файл пустой, используем тестовые имена
            authors.add("John Doe");
            authors.add("Jane Smith");
            authors.add("Bob Johnson");
            authors.add("Alice Brown");
            authors.add("Charlie Wilson");
        }

        return authors;
    }

    private static String generateTitle(int index) {
        String[] templates = {
                "Project Report %d",
                "Meeting Notes %d",
                "Technical Specification %d",
                "User Manual %d",
                "Design Document %d",
                "Test Plan %d",
                "Release Notes %d",
                "API Documentation %d",
                "System Architecture %d",
                "Requirements Analysis %d"
        };

        String template = templates[index % templates.length];
        return String.format(template, index + 1);
    }

    private static boolean createDocument(String author, String title) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            String requestBody = String.format(
                    "{\"author\":\"%s\",\"title\":\"%s\"}",
                    escapeJson(author),
                    escapeJson(title)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/documents"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 201 || response.statusCode() == 200;

        } catch (Exception e) {
            log.error("Failed to create document: {}", e.getMessage());
            return false;
        }
    }

    private static String escapeJson(String s) {
        return s.replace("\"", "\\\"");
    }
}