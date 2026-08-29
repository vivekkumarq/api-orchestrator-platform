package com.vivek.platform.apiorchestrator;

import com.vivek.platform.apiorchestrator.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class ApiOrchestratorApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiOrchestratorApplication.class, args);
	}

}
