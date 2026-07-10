package com.hlju.learning.domain.rag;

import jakarta.validation.constraints.NotBlank;

public record RetrievalRequest(
        @NotBlank String query,
        String materialId,
        String subjectPreset,
        Integer topK
) {
}
