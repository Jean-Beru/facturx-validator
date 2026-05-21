package dev.gotenberg.facturx.postcheck;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;

public final class SubtypeCheck {

    public static final String NAME = "EmbeddedFile.Subtype";
    private static final String EXPECTED = "text/xml";

    private SubtypeCheck() {}

    public static CheckResult run(PDComplexFileSpecification spec) {
        if (spec == null) {
            return CheckResult.fail(NAME, FacturxAttachment.FILE_NAME + " not found in EmbeddedFiles");
        }
        CheckResult fileCheck = checkStream("/F", spec.getEmbeddedFile());
        if (!fileCheck.ok()) {
            return fileCheck;
        }
        return checkStream("/UF", spec.getEmbeddedFileUnicode());
    }

    private static CheckResult checkStream(String key, PDEmbeddedFile stream) {
        if (stream == null) {
            return CheckResult.pass(NAME);
        }
        String sub = stream.getCOSObject().getNameAsString(COSName.SUBTYPE);
        if (!EXPECTED.equals(sub)) {
            return CheckResult.fail(NAME, key + " Subtype is " + sub + ", expected " + EXPECTED);
        }
        return CheckResult.pass(NAME);
    }
}
