package dev.gotenberg.facturx.postcheck;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubtypeCheckTest {

    @TempDir
    Path tmp;

    @Test
    void passesWhenSubtypeIsTextXml() throws IOException {
        CheckResult r = run(Fixtures.valid().build(tmp));
        assertTrue(r.ok(), () -> "expected pass, got: " + r.message());
    }

    @Test
    void failsWhenSubtypeIsWrong() throws IOException {
        CheckResult r = run(Fixtures.wrongSubtype().build(tmp));
        assertFalse(r.ok());
        assertTrue(r.message().contains("application/octet-stream"), r.message());
    }

    @Test
    void failsWhenSubtypeIsMissing() throws IOException {
        CheckResult r = run(Fixtures.missingSubtype().build(tmp));
        assertFalse(r.ok());
        assertTrue(r.message().contains("Subtype is null"), r.message());
    }

    @Test
    void failsWhenAttachmentIsMissing() throws IOException {
        CheckResult r = run(Fixtures.noAttachment().build(tmp));
        assertFalse(r.ok());
        assertEquals("factur-x.xml not found in EmbeddedFiles", r.message());
    }

    private static CheckResult run(Path pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            return SubtypeCheck.run(FacturxAttachment.resolve(doc));
        }
    }
}
