package dev.gotenberg.facturx.postcheck;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class Main {

    private Main() {}

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("usage: postcheck <pdf-path>");
            System.exit(2);
        }

        File pdf = new File(args[0]);
        if (!pdf.isFile()) {
            System.err.println("postcheck: file not found: " + pdf);
            System.exit(2);
        }

        int rc;
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDComplexFileSpecification spec = FacturxAttachment.resolve(doc);

            List<CheckResult> results = new ArrayList<>();
            results.add(SubtypeCheck.run(spec));

            XmpCiiCheck.Result xc = XmpCiiCheck.run(doc, spec);
            results.add(xc.result());

            results.add(AfRelationshipCheck.run(spec, xc.profile()));
            results.add(AfArrayCheck.run(doc, spec));

            rc = report(results);
        } catch (IOException e) {
            System.err.println("postcheck: cannot read PDF: " + e.getMessage());
            System.exit(2);
            return;
        }
        System.exit(rc);
    }

    private static int report(List<CheckResult> results) {
        int rc = 0;
        for (CheckResult r : results) {
            if (r.ok()) {
                System.out.println("PASS " + r.name());
            } else {
                System.out.println("FAIL " + r.name() + ": " + r.message());
                rc = 1;
            }
        }
        return rc;
    }
}
