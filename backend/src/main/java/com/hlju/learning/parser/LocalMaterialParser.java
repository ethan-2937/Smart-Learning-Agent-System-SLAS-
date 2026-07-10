package com.hlju.learning.parser;

import com.hlju.learning.domain.material.ParsedMaterial;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class LocalMaterialParser implements MaterialParser {
    @Override
    public ParsedMaterial parse(Path path, String originalFileName) {
        String lowerName = originalFileName.toLowerCase(Locale.ROOT);
        try {
            if (lowerName.endsWith(".pdf")) {
                return new ParsedMaterial(originalFileName, parsePdf(path));
            }
            if (lowerName.endsWith(".docx")) {
                return new ParsedMaterial(originalFileName, parseDocx(path));
            }
            if (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls")) {
                return new ParsedMaterial(originalFileName, parseWorkbook(path));
            }
            return new ParsedMaterial(originalFileName, Files.readString(path, StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalArgumentException("教材解析失败：" + ex.getMessage(), ex);
        }
    }

    private String parsePdf(Path path) throws IOException {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String parseDocx(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path); XWPFDocument document = new XWPFDocument(inputStream)) {
            return document.getParagraphs().stream()
                    .map(paragraph -> paragraph.getText() == null ? "" : paragraph.getText())
                    .filter(text -> !text.isBlank())
                    .collect(Collectors.joining("\n"));
        }
    }

    private String parseWorkbook(Path path) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (InputStream inputStream = Files.newInputStream(path); Workbook workbook = WorkbookFactory.create(inputStream)) {
            for (Sheet sheet : workbook) {
                builder.append("# ").append(sheet.getSheetName()).append('\n');
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        builder.append(cell).append('\t');
                    }
                    builder.append('\n');
                }
            }
        }
        return builder.toString();
    }
}
