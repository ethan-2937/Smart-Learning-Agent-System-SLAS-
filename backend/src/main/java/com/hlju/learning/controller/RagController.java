package com.hlju.learning.controller;

import com.hlju.learning.domain.rag.RetrievalRequest;
import com.hlju.learning.domain.rag.RetrievalResult;
import com.hlju.learning.service.VectorRetrievalService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RagController {
    private final VectorRetrievalService retrievalService;

    public RagController(VectorRetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    @PostMapping("/api/rag/retrieve")
    public RetrievalResult retrieve(@RequestBody @Valid RetrievalRequest request) {
        return retrievalService.retrieve(request.query(), request.materialId(), request.topK());
    }
}
