package dev.gotenberg.facturx.postcheck;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XmpCiiCheckTest {

    @TempDir
    Path tmp;

    @Test
    void passesWhenXmpAndCiiAgree() throws IOException {
        XmpCiiCheck.Result r = run(Fixtures.valid().build(tmp));
        assertTrue(r.result().ok(), () -> r.result().message());
        assertNotNull(r.profile());
        assertEquals("BASIC WL", r.profile().ciiLevel());
    }

    @Test
    void failsWhenLevelsDiffer() throws IOException {
        XmpCiiCheck.Result r = run(Fixtures.xmpCiiMismatch().build(tmp));
        assertFalse(r.result().ok());
        assertTrue(r.result().message().contains("EN 16931"), r.result().message());
    }

    @Test
    void failsWhenUrnIsUnknown() throws IOException {
        XmpCiiCheck.Result r = run(Fixtures.unknownUrn().build(tmp));
        assertFalse(r.result().ok());
        assertTrue(r.result().message().contains("urn:nope"), r.result().message());
    }

    @Test
    void failsWhenXmpIsMissing() throws IOException {
        XmpCiiCheck.Result r = run(Fixtures.noXmp().build(tmp));
        assertFalse(r.result().ok());
        assertTrue(r.result().message().contains("XMP"), r.result().message());
    }

    private static XmpCiiCheck.Result run(Path pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            return XmpCiiCheck.run(doc, FacturxAttachment.resolve(doc));
        }
    }
}
