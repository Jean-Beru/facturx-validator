package dev.gotenberg.facturx.postcheck;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;

import java.io.IOException;

public final class FacturxAttachment {

    public static final String FILE_NAME = "factur-x.xml";

    private FacturxAttachment() {}

    public static PDComplexFileSpecification resolve(PDDocument doc) throws IOException {
        PDDocumentNameDictionary names = doc.getDocumentCatalog().getNames();
        if (names == null) {
            return null;
        }
        PDEmbeddedFilesNameTreeNode tree = names.getEmbeddedFiles();
        if (tree == null) {
            return null;
        }
        return (PDComplexFileSpecification) tree.getValue(FILE_NAME);
    }
}
