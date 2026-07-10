package com.hlju.learning.mapper;

import com.hlju.learning.domain.po.MaterialChunkPo;
import com.hlju.learning.domain.po.MaterialPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MaterialMapper {
    void upsertMaterial(@Param("po") MaterialPo po);

    MaterialPo findMaterial(@Param("materialId") String materialId);

    List<MaterialPo> findAllMaterials();

    void deleteChunks(@Param("materialId") String materialId);

    void insertChunk(@Param("po") MaterialChunkPo po);

    List<MaterialChunkPo> findChunks(@Param("materialId") String materialId);
}
