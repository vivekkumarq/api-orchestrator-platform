package com.vivek.platform.apiorchestrator.api;

import com.vivek.platform.apiorchestrator.api.dto.ExecuteRequest;
import com.vivek.platform.apiorchestrator.api.dto.ExecuteResponse;
import com.vivek.platform.apiorchestrator.repository.RequestHistoryRepository;
import com.vivek.platform.apiorchestrator.domain.RequestHistoryEntity;
import com.vivek.platform.apiorchestrator.service.RequestExecutorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/requests")
public class RequestController {

    private final RequestExecutorService executorService;
    private final RequestHistoryRepository historyRepository;

    public RequestController(RequestExecutorService executorService,
                             RequestHistoryRepository historyRepository) {
        this.executorService = executorService;
        this.historyRepository = historyRepository;
    }

    @PostMapping("/execute")
    public ExecuteResponse execute(@RequestBody @Valid ExecuteRequest request) {
        return executorService.execute(request);
    }

    @GetMapping("/history")
    public List<RequestHistoryEntity> history() {
        return historyRepository.findAll();
    }
}