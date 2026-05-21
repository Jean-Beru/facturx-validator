package dev.gotenberg.facturx.postcheck;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;

import java.util.Set;

public final class AfRelationshipCheck {

    public static final String NAME = "FileSpec.AFRelationship";
    private static final Set<String> ALLOWED = Set.of("Data", "Source", "Alternative");
    private static final String REQUIRED = "Data";

    private AfRelationshipCheck() {}

    public static CheckResult run(PDComplexFileSpecification spec, Profile profile) {
        if (spec == null) {
            return CheckResult.fail(NAME, FacturxAttachment.FILE_NAME + " not found in EmbeddedFiles");
        }
        String rel = spec.getCOSObject().getNameAsString(COSName.getPDFName("AFRelationship"));
        if (rel == null) {
            return CheckResult.fail(NAME, "AFRelationship is missing");
        }
        if (!ALLOWED.contains(rel)) {
            return CheckResult.fail(NAME, "AFRelationship=" + rel + " not in " + ALLOWED);
        }
        boolean isMinimum = profile != null && profile.isMinimum();
        if (!isMinimum && !REQUIRED.equals(rel)) {
            return CheckResult.fail(NAME, "AFRelationship=" + rel + ", must be " + REQUIRED);
        }
        return CheckResult.pass(NAME);
    }
}
