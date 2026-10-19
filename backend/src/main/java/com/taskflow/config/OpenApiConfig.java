package com.taskflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI taskFlowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TaskFlow API")
                        .description("Production-style distributed background job queue and processing platform. Features priority scheduling, atomic PostgreSQL claiming, exponential retry backoff with jitter, dead-letter tracking, and real-time observability.")
                        .version("1.0.0")
                        .license(new License().name("Apache 2.0").url("https://spring.io"))
                        .contact(new Contact().name("TaskFlow Team").url("https://github.com/taskflow")));
    }
}
