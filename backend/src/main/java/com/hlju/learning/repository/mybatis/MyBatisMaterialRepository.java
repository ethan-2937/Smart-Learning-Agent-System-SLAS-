package com.hlju.learning.repository.mybatis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.hlju.learning.domain.material.MaterialChunk;
import com.hlju.learning.domain.material.MaterialRecord;
import com.hlju.learning.domain.material.MaterialStatus;
import com.hlju.learning.domain.material.SubjectPreset;
import com.hlju.learning.domain.po.MaterialChunkPo;
import com.hlju.learning.domain.po.MaterialPo;
import com.hlju.learning.mapper.MaterialMapper;
import com.hlju.learning.repository.MaterialRepository;
import com.hlju.learning.util.JsonCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "app.repository.provider", havingValue = "mybatis")
public class MyBatisMaterialRepository implements MaterialRepository {
    private final MaterialMapper mapper;
    private final JsonCodec jsonCodec;

    public MyBatisMaterialRepository(MaterialMapper mapper, JsonCodec jsonCodec) {
        this.mapper = mapper;
        this.jsonCodec = jsonCodec;
    }

    @Override
    public void saveMaterial(MaterialRecord record) {
        mapper.upsertMaterial(toPo(record));
    }

    @Override
    public Optional<MaterialRecord> findMaterial(String materialId) {
        return Optional.ofNullable(mapper.findMaterial(materialId)).map(this::toDomain);
    }

    @Override
    public List<MaterialRecord> findAllMaterials() {
        return mapper.findAllMaterials().stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void replaceChunks(String materialId, List<MaterialChunk> chunks) {
        mapper.deleteChunks(materialId);
        chunks.stream().map(this::toPo).forEach(mapper::insertChunk);
    }

    @Override
    public List<MaterialChunk> findChunks(String materialId) {
        return mapper.findChunks(materialId).stream().map(this::toDomain).toList();
    }

    private MaterialPo toPo(MaterialRecord record) {
        MaterialPo po = new MaterialPo();
        po.setMaterialId(record.materialId());
        po.setTitle(record.title());
        po.setSubjectPreset(record.subjectPreset().name());
        po.setOriginalFileName(record.originalFileName());
        po.setContentType(record.contentType());
        po.setStoragePath(record.storagePath());
        po.setStatus(record.status().name());
        po.setChunkCount(record.chunkCount());
        po.setErrorMessage(record.errorMessage());
        po.setCreatedAt(toLocal(record.createdAt()));
        po.setUpdatedAt(toLocal(record.updatedAt()));
        return po;
    }

    private MaterialRecord toDomain(MaterialPo po) {
        return new MaterialRecord(po.getMaterialId(), po.getTitle(), SubjectPreset.valueOf(po.getSubjectPreset()),
                po.getOriginalFileName(), po.getContentType(), po.getStoragePath(), MaterialStatus.valueOf(po.getStatus()),
                po.getChunkCount() == null ? 0 : po.getChunkCount(), po.getErrorMessage(), toInstant(po.getCreatedAt()), toInstant(po.getUpdatedAt()));
    }

    private MaterialChunkPo toPo(MaterialChunk chunk) {
        MaterialChunkPo po = new MaterialChunkPo();
        po.setChunkId(chunk.chunkId());
        po.setMaterialId(chunk.materialId());
        po.setChapterId(chunk.chapterId());
        po.setChapterTitle(chunk.chapterTitle());
        po.setChunkIndex(chunk.chunkIndex());
        po.setPageNo(chunk.pageNo());
        po.setSourceLabel(chunk.sourceLabel());
        po.setText(chunk.text());
        po.setKeywordsJson(jsonCodec.toJson(chunk.keywords()));
        po.setCreatedAt(toLocal(chunk.createdAt()));
        return po;
    }

    private MaterialChunk toDomain(MaterialChunkPo po) {
        List<String> keywords = jsonCodec.readList(po.getKeywordsJson(), new TypeReference<>() {});
        return new MaterialChunk(po.getChunkId(), po.getMaterialId(), po.getChapterId(), po.getChapterTitle(),
                po.getChunkIndex() == null ? 0 : po.getChunkIndex(), po.getPageNo(), po.getSourceLabel(), po.getText(),
                keywords, toInstant(po.getCreatedAt()));
    }

    private LocalDateTime toLocal(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? Instant.now() : value.toInstant(ZoneOffset.UTC);
    }
}
