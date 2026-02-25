package com.itqgroup.service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI documentFlowAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Document Flow API")
                        .description("API для управления документами с историей статусов")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Разработчик")
                                .email("developer@example.com")
                                .url("https://github.com/gareq1211"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}