package dev.gotenberg.facturx.postcheck;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AfRelationshipCheckTest {

    @TempDir
    Path tmp;

    private static final Profile BASIC_WL = new Profile("BASIC WL", "BASIC WL", "urn:factur-x.eu:1p0:basicwl");
    private static final Profile MINIMUM  = new Profile("MINIMUM",  "MINIMUM",  "urn:factur-x.eu:1p0:minimum");

    @Test
    void passesWhenRelationshipIsData() throws IOException {
        CheckResult r = run(Fixtures.valid().build(tmp), BASIC_WL);
        assertTrue(r.ok(), () -> r.message());
    }

    @Test
    void failsWhenRelationshipIsNotInAllowedSet() throws IOException {
        CheckResult r = run(Fixtures.wrongAfRelationship().build(tmp), BASIC_WL);
        assertFalse(r.ok());
        assertTrue(r.message().contains("Unspecified"), r.message());
    }

    @Test
    void failsWhenRelationshipIsMissing() throws IOException {
        CheckResult r = run(Fixtures.missingAfRelationship().build(tmp), BASIC_WL);
        assertFalse(r.ok());
        assertTrue(r.message().contains("missing"), r.message());
    }

    @Test
    void failsWhenSourceUsedAboveMinimum() throws IOException {
        Path pdf = Fixtures.valid().fileName("source.pdf").afRelationship("Source").build(tmp);
        CheckResult r = run(pdf, BASIC_WL);
        assertFalse(r.ok());
        assertTrue(r.message().contains("must be Data"), r.message());
    }

    @Test
    void allowsSourceForMinimumProfile() throws IOException {
        Path pdf = Fixtures.valid().fileName("min_source.pdf").afRelationship("Source").build(tmp);
        CheckResult r = run(pdf, MINIMUM);
        assertTrue(r.ok(), () -> r.message());
    }

    private static CheckResult run(Path pdf, Profile profile) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            return AfRelationshipCheck.run(FacturxAttachment.resolve(doc), profile);
        }
    }
}
