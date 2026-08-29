package com.vivek.platform.apiorchestrator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String DESCRIPTION =
            "Execute HTTP requests against third-party APIs with variable substitution, response "
            + "assertions, value extraction for request chaining, retry with backoff, and persisted "
            + "history. Saved requests are organised into collections and can be imported from / "
            + "exported to Postman v2.1 collection JSON.";

    @Bean
    public OpenAPI apiOrchestratorOpenApi() {
        return new OpenAPI().info(new Info()
                .title("API Orchestrator")
                .version("v1")
                .description(DESCRIPTION)
                .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")));
    }
}
