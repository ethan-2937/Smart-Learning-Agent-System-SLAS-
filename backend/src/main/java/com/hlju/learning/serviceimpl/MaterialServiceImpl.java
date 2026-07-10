package com.hlju.learning.serviceimpl;

import com.hlju.learning.config.StorageProperties;
import com.hlju.learning.domain.material.MaterialChunk;
import com.hlju.learning.domain.material.MaterialRecord;
import com.hlju.learning.domain.material.MaterialStatus;
import com.hlju.learning.domain.material.ParsedMaterial;
import com.hlju.learning.domain.material.SubjectPreset;
import com.hlju.learning.parser.MaterialParser;
import com.hlju.learning.repository.MaterialRepository;
import com.hlju.learning.service.MaterialService;
import com.hlju.learning.service.VectorRetrievalService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class MaterialServiceImpl implements MaterialService {
    private final StorageProperties storageProperties;
    private final MaterialParser parser;
    private final VectorRetrievalService vectorRetrievalService;
    private final MaterialRepository materialRepository;

    public MaterialServiceImpl(StorageProperties storageProperties, MaterialParser parser,
                               VectorRetrievalService vectorRetrievalService, MaterialRepository materialRepository) {
        this.storageProperties = storageProperties;
        this.parser = parser;
        this.vectorRetrievalService = vectorRetrievalService;
        this.materialRepository = materialRepository;
    }

    @Override
    public MaterialRecord uploadMaterial(MultipartFile file, String title, SubjectPreset subjectPreset) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传教材文件");
        }
        String materialId = UUID.randomUUID().toString();
        String safeName = materialId + "-" + file.getOriginalFilename();
        Path uploadPath = Path.of(storageProperties.uploadDir()).toAbsolutePath().normalize();
        Path targetPath = uploadPath.resolve(safeName).normalize();
        try {
            Files.createDirectories(uploadPath);
            file.transferTo(targetPath);
        } catch (IOException ex) {
            throw new IllegalArgumentException("保存教材文件失败：" + ex.getMessage(), ex);
        }
        Instant now = Instant.now();
        MaterialRecord record = new MaterialRecord(
                materialId,
                title == null || title.isBlank() ? file.getOriginalFilename() : title,
                subjectPreset == null ? SubjectPreset.GENERAL : subjectPreset,
                file.getOriginalFilename(),
                file.getContentType(),
                targetPath.toString(),
                MaterialStatus.UPLOADED,
                0,
                null,
                now,
                now
        );
        materialRepository.saveMaterial(record);
        return record;
    }

    @Override
    public MaterialRecord parseAndIndex(String materialId) {
        MaterialRecord record = getMaterial(materialId);
        materialRepository.saveMaterial(record.withStatus(MaterialStatus.PARSING, record.chunkCount(), null));
        try {
            ParsedMaterial parsed = parser.parse(Path.of(record.storagePath()), record.originalFileName());
            List<MaterialChunk> chunks = chunk(parsed.text(), record);
            materialRepository.replaceChunks(materialId, chunks);
            vectorRetrievalService.indexChunks(chunks);
            MaterialRecord indexed = record.withStatus(MaterialStatus.INDEXED, chunks.size(), null);
            materialRepository.saveMaterial(indexed);
            return indexed;
        } catch (Exception ex) {
            MaterialRecord failed = record.withStatus(MaterialStatus.FAILED, 0, ex.getMessage());
            materialRepository.saveMaterial(failed);
            return failed;
        }
    }

    @Override
    public List<MaterialRecord> listMaterials() {
        return materialRepository.findAllMaterials();
    }

    @Override
    public MaterialRecord getMaterial(String materialId) {
        return materialRepository.findMaterial(materialId)
                .orElseThrow(() -> new IllegalArgumentException("教材不存在：" + materialId));
    }

    @Override
    public List<MaterialChunk> listChunks(String materialId) {
        getMaterial(materialId);
        return materialRepository.findChunks(materialId);
    }

    private List<MaterialChunk> chunk(String text, MaterialRecord record) {
        String normalized = text == null ? "" : text.replace("\r", "").trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("教材内容为空，无法切片");
        }
        int maxLength = 700;
        int overlap = 80;
        List<MaterialChunk> chunks = new ArrayList<>();
        int start = 0;
        int index = 0;
        while (start < normalized.length()) {
            int end = Math.min(normalized.length(), start + maxLength);
            String chunkText = normalized.substring(start, end).trim();
            if (!chunkText.isBlank()) {
                chunks.add(new MaterialChunk(UUID.randomUUID().toString(), record.materialId(), "chapter-default",
                        record.title(), index, null, record.originalFileName() + "#" + index,
                        chunkText, extractKeywords(chunkText), Instant.now()));
                index++;
            }
            if (end >= normalized.length()) break;
            start = Math.max(end - overlap, start + 1);
        }
        return chunks;
    }

    private List<String> extractKeywords(String text) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        for (String token : text.toLowerCase(Locale.ROOT).split("[^a-z0-9\\u4e00-\\u9fa5]+")) {
            if (token.length() >= 3 && keywords.size() < 12) {
                keywords.add(token);
            }
        }
        return new ArrayList<>(keywords);
    }
}
