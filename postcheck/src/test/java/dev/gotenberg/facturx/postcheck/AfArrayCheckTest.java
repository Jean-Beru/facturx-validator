package dev.gotenberg.facturx.postcheck;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AfArrayCheckTest {

    @TempDir
    Path tmp;

    @Test
    void passesWhenAfArrayReferencesAttachment() throws IOException {
        CheckResult r = run(Fixtures.valid().build(tmp));
        assertTrue(r.ok(), () -> r.message());
    }

    @Test
    void failsWhenAfArrayIsMissing() throws IOException {
        CheckResult r = run(Fixtures.noAfArray().build(tmp));
        assertFalse(r.ok());
        assertTrue(r.message().contains("/AF array missing"), r.message());
    }

    private static CheckResult run(Path pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            return AfArrayCheck.run(doc, FacturxAttachment.resolve(doc));
        }
    }
}
