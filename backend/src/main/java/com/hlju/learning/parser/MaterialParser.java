package com.hlju.learning.parser;

import com.hlju.learning.domain.material.ParsedMaterial;

import java.nio.file.Path;

public interface MaterialParser {
    ParsedMaterial parse(Path path, String originalFileName);
}
