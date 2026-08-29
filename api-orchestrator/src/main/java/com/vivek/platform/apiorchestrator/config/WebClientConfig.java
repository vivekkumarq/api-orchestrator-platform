package com.vivek.platform.apiorchestrator.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

/**
 * One shared {@link WebClient} for the whole application.
 *
 * <p>The original implementation built a fresh {@code HttpClient} and {@code WebClient} inside
 * every call, which allocated a new connection pool per request and never reused a connection.
 * A single pooled client is both faster and leak-free; per-request timeouts are applied by the
 * executor with {@code Mono#timeout} rather than by rebuilding the client.
 */
@Configuration
public class WebClientConfig {

    @Bean
    public ConnectionProvider orchestratorConnectionProvider() {
        return ConnectionProvider.builder("api-orchestrator")
                .maxConnections(100)
                .maxIdleTime(Duration.ofSeconds(30))
                .pendingAcquireTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Bean
    public WebClient orchestratorWebClient(AppProperties properties, ConnectionProvider provider) {
        HttpClient httpClient = HttpClient.create(provider)
                .followRedirect(true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000);

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                // Bound the in-memory buffer so a hostile or simply large response cannot OOM us.
                .codecs(c -> c.defaultCodecs().maxInMemorySize(properties.getExecutor().getMaxResponseBytes()))
                .build();
    }
}
