package com.studyanchor.tutor.infrastructure;

import com.studyanchor.tutor.domain.DomainException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** FR-02 AC4: turning uploaded files into text. PDFs are built in memory, so no test files are needed. */
class UploadedFileTextTest {

    private final UploadedFileText text = new UploadedFileText();

    /** A one-page PDF, with the given line of text or (null) with no text at all, like a scanned page. */
    static byte[] pdf(String line) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            if (line != null) {
                try (PDPageContentStream s = new PDPageContentStream(doc, page)) {
                    s.beginText();
                    s.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    s.newLineAtOffset(72, 700);
                    s.showText(line);
                    s.endText();
                }
            }
            doc.save(out);
            return out.toByteArray();
        }
    }

    @Test
    void FR02_textAndMarkdownReadAsUtf8() {
        assertThat(text.extract("notes.md", "# Kafka – logs".getBytes(StandardCharsets.UTF_8))).isEqualTo("# Kafka – logs");
        assertThat(text.extract("NOTES.TXT", "Kafka".getBytes(StandardCharsets.UTF_8))).isEqualTo("Kafka");
    }

    @Test
    void FR02_pdfTextIsExtracted() throws IOException {
        assertThat(text.extract("lecture.pdf", pdf("Kafka is a distributed log"))).contains("Kafka is a distributed log");
        assertThat(text.extract("scan.pdf", pdf(null))).as("scanned page: Material rejects it").isBlank();
    }

    @Test
    void FR02_unreadablePdfIsRejected() {
        assertThatThrownBy(() -> text.extract("broken.pdf", "not a pdf".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(DomainException.class).hasMessageContaining("Could not read the PDF");
    }

    @Test
    void FR02_otherFileTypesRejected() {
        byte[] bytes = "x".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> text.extract("slides.docx", bytes)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> text.extract("README", bytes)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> text.extract(null, bytes)).isInstanceOf(DomainException.class);
    }
}
