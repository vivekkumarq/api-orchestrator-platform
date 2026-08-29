package com.vivek.platform.apiorchestrator.api;

import com.vivek.platform.apiorchestrator.api.dto.ExecuteRequest;
import com.vivek.platform.apiorchestrator.api.dto.ExecuteResponse;
import com.vivek.platform.apiorchestrator.api.dto.HistoryDetailDto;
import com.vivek.platform.apiorchestrator.api.dto.HistorySummaryDto;
import com.vivek.platform.apiorchestrator.service.HistoryService;
import com.vivek.platform.apiorchestrator.service.RequestExecutorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Requests", description = "Execute requests and read the execution history")
@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final RequestExecutorService executorService;
    private final HistoryService historyService;

    public RequestController(RequestExecutorService executorService, HistoryService historyService) {
        this.executorService = executorService;
        this.historyService = historyService;
    }

    @Operation(summary = "Execute a request",
            description = "Resolves {{variables}} against the named environment, performs the HTTP "
                    + "exchange with the configured timeout and retry policy, evaluates assertions, "
                    + "applies extraction specs and records the result in history.")
    @PostMapping("/execute")
    public ExecuteResponse execute(@RequestBody @Valid ExecuteRequest request) {
        return executorService.execute(request);
    }

    @Operation(summary = "List execution history", description = "Newest first, without response bodies.")
    @GetMapping("/history")
    public Page<HistorySummaryDto> history(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "50") int size) {
        return historyService.findPage(page, size);
    }

    @Operation(summary = "Read one history entry", description = "Includes the captured response body.")
    @GetMapping("/history/{id}")
    public HistoryDetailDto historyEntry(@PathVariable UUID id) {
        return historyService.findById(id);
    }

    @Operation(summary = "Clear the execution history")
    @DeleteMapping("/history")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearHistory() {
        historyService.deleteAll();
    }
}
