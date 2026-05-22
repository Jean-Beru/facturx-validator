package dev.gotenberg.facturx.postcheck;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class Main {

    private Main() {}

    public static void main(String[] args) {
        Args parsed;
        try {
            parsed = Args.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println("postcheck: " + e.getMessage());
            System.err.println("usage: postcheck [--format=yaml|md|xml] "
                    + "[--mustang-output=<file>] [--mustang-rc=<n>] <pdf-path>");
            System.exit(2);
            return;
        }

        File pdf = new File(parsed.pdf);
        if (!pdf.isFile()) {
            System.err.println("postcheck: file not found: " + pdf);
            System.exit(2);
        }

        MustangReport mustang;
        try {
            mustang = MustangReport.from(parsed.mustangOutput, parsed.mustangRc);
        } catch (IOException e) {
            System.err.println("postcheck: cannot read mustang output: " + e.getMessage());
            System.exit(2);
            return;
        }

        List<CheckResult> results;
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            results = runChecks(doc);
        } catch (IOException e) {
            System.err.println("postcheck: cannot read PDF: " + e.getMessage());
            System.exit(2);
            return;
        }

        ReportWriter.write(System.out, parsed.format, mustang, results);

        boolean postOk = results.stream().allMatch(CheckResult::ok);
        System.exit((mustang.ok() && postOk) ? 0 : 1);
    }

    private static List<CheckResult> runChecks(PDDocument doc) throws IOException {
        PDComplexFileSpecification spec = FacturxAttachment.resolve(doc);
        List<CheckResult> results = new ArrayList<>();
        results.add(SubtypeCheck.run(spec));
        XmpCiiCheck.Result xc = XmpCiiCheck.run(doc, spec);
        results.add(xc.result());
        results.add(AfRelationshipCheck.run(spec, xc.profile()));
        results.add(AfArrayCheck.run(doc, spec));
        return results;
    }

    private static final class Args {
        OutputFormat format = OutputFormat.TEXT;
        Path mustangOutput = null;
        int mustangRc = 0;
        String pdf;

        static Args parse(String[] argv) {
            Args a = new Args();
            for (String arg : argv) {
                if (arg.startsWith("--format=")) {
                    a.format = OutputFormat.parse(value(arg));
                } else if (arg.startsWith("--mustang-output=")) {
                    a.mustangOutput = Path.of(value(arg));
                } else if (arg.startsWith("--mustang-rc=")) {
                    a.mustangRc = Integer.parseInt(value(arg));
                } else if (arg.startsWith("-")) {
                    throw new IllegalArgumentException("unknown option: " + arg);
                } else {
                    if (a.pdf != null) {
                        throw new IllegalArgumentException("only one pdf path allowed");
                    }
                    a.pdf = arg;
                }
            }
            if (a.pdf == null) {
                throw new IllegalArgumentException("missing pdf path");
            }
            return a;
        }

        private static String value(String arg) {
            int eq = arg.indexOf('=');
            return arg.substring(eq + 1);
        }
    }
}
