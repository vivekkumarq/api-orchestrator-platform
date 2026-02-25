package com.vivek.platform.apiorchestrator.service;

import com.vivek.platform.apiorchestrator.api.dto.ExecuteRequest;
import com.vivek.platform.apiorchestrator.api.dto.ExecuteResponse;
import com.vivek.platform.apiorchestrator.domain.RequestHistoryEntity;
import com.vivek.platform.apiorchestrator.repository.RequestHistoryRepository;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Service
public class RequestExecutorService {

    private final RequestHistoryRepository historyRepository;

    public RequestExecutorService(RequestHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    public ExecuteResponse execute(ExecuteRequest req) {
        long start = System.currentTimeMillis();

        HttpClient httpClient = HttpClient.create();
        if (req.getTimeoutMs() != null) {
            httpClient = httpClient.responseTimeout(Duration.ofMillis(req.getTimeoutMs()));
        }

        WebClient client = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        WebClient.RequestBodySpec spec = client.method(HttpMethod.valueOf(req.getMethod()))
                .uri(req.getUrl())
                .headers(h -> {
                    if (req.getHeaders() != null) {
                        req.getHeaders().forEach(h::add);
                    }
                });

        ExecuteResponse res = spec
                .bodyValue(req.getBody() == null ? "" : req.getBody())
                .exchangeToMono(response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> {
                            ExecuteResponse r = new ExecuteResponse();
                            r.setStatus(response.statusCode().value());
                            r.setHeaders(response.headers().asHttpHeaders().toSingleValueMap());
                            r.setBody(body);
                            r.setResponseTimeMs(System.currentTimeMillis() - start);
                            return r;
                        })
                )
                .onErrorResume(ex -> {
                    ExecuteResponse err = new ExecuteResponse();
                    err.setStatus(0);
                    err.setHeaders(Map.of());
                    err.setBody("ERROR: " + ex.getMessage());
                    err.setResponseTimeMs(System.currentTimeMillis() - start);
                    return reactor.core.publisher.Mono.just(err);
                })
                .block();


        RequestHistoryEntity h = new RequestHistoryEntity();
        h.setUrl(req.getUrl());
        h.setMethod(req.getMethod());
        h.setRequestHeaders(String.valueOf(req.getHeaders()));
        h.setRequestBody(req.getBody());
        h.setStatus(res.getStatus());
        h.setResponseHeaders(String.valueOf(res.getHeaders()));
        h.setResponseBody(res.getBody());
        h.setResponseTimeMs(res.getResponseTimeMs());
        h.setCreatedAt(Instant.now());

        historyRepository.save(h);

        return res;
    }
}