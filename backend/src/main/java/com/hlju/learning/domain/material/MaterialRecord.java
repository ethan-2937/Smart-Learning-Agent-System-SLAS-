package com.hlju.learning.domain.material;

import java.time.Instant;

public record MaterialRecord(
        String materialId,
        String title,
        SubjectPreset subjectPreset,
        String originalFileName,
        String contentType,
        String storagePath,
        MaterialStatus status,
        int chunkCount,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
    public MaterialRecord withStatus(MaterialStatus nextStatus, int nextChunkCount, String nextErrorMessage) {
        return new MaterialRecord(materialId, title, subjectPreset, originalFileName, contentType, storagePath,
                nextStatus, nextChunkCount, nextErrorMessage, createdAt, Instant.now());
    }
}
