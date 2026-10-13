package com.studyanchor.tutor.infrastructure;

import com.studyanchor.tutor.domain.DomainException;
import com.studyanchor.tutor.domain.MaterialText;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Implements FR-02 AC4. Adapter: reads .txt and .md files as UTF-8 and extracts the text of .pdf files with
 * Apache PDFBox. Other file types, and PDFs that are broken or password-protected, are rejected (400).
 */
@Component
class UploadedFileText implements MaterialText {

    @Override
    public String extract(String fileName, byte[] bytes) {
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (name.endsWith(".txt") || name.endsWith(".md")) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        if (name.endsWith(".pdf")) {
            return pdfText(bytes);
        }
        throw new DomainException("Upload a .txt, .md or .pdf file, not " + fileName);
    }

    private static String pdfText(byte[] bytes) {
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(doc);
        } catch (IOException e) {
            throw new DomainException("Could not read the PDF (broken or password-protected): " + e.getMessage());
        }
    }
}
