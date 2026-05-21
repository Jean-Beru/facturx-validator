package dev.gotenberg.facturx.postcheck;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;

public final class AfArrayCheck {

    public static final String NAME = "Catalog.AF";

    private AfArrayCheck() {}

    public static CheckResult run(PDDocument doc, PDComplexFileSpecification spec) {
        if (spec == null) {
            return CheckResult.fail(NAME, FacturxAttachment.FILE_NAME + " not found in EmbeddedFiles");
        }
        COSBase af = doc.getDocumentCatalog().getCOSObject().getItem(COSName.getPDFName("AF"));
        if (af == null) {
            return CheckResult.fail(NAME, "/AF array missing in Catalog");
        }
        COSBase resolved = (af instanceof COSObject co) ? co.getObject() : af;
        if (!(resolved instanceof COSArray arr)) {
            return CheckResult.fail(NAME, "/AF is not an array");
        }

        long target = spec.getCOSObject().getKey() == null
                ? -1L
                : spec.getCOSObject().getKey().getNumber();
        for (COSBase item : arr) {
            if (item instanceof COSObject ref && ref.getObjectNumber() == target) {
                return CheckResult.pass(NAME);
            }
        }
        return CheckResult.fail(NAME, FacturxAttachment.FILE_NAME + " FileSpec not referenced by /AF");
    }
}
