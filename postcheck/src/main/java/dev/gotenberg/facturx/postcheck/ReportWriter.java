package dev.gotenberg.facturx.postcheck;

import java.io.PrintStream;
import java.util.List;

public final class ReportWriter {

    private ReportWriter() {}

    public static void write(PrintStream out, OutputFormat format,
                             MustangReport mustang, List<CheckResult> checks) {
        boolean postOk = checks.stream().allMatch(CheckResult::ok);
        boolean overall = mustang.ok() && postOk;
        switch (format) {
            case YAML -> writeYaml(out, mustang, checks, postOk, overall);
            case MD   -> writeMd(out, mustang, checks, postOk, overall);
            case XML  -> writeXml(out, mustang, checks, postOk, overall);
            case TEXT -> writeText(out, checks);
        }
    }

    private static void writeText(PrintStream out, List<CheckResult> checks) {
        for (CheckResult r : checks) {
            if (r.ok()) {
                out.println("PASS " + r.name());
            } else {
                out.println("FAIL " + r.name() + ": " + r.message());
            }
        }
    }

    private static void writeYaml(PrintStream out, MustangReport mustang,
                                  List<CheckResult> checks, boolean postOk, boolean overall) {
        out.println("status: " + status(overall));
        out.println("mustang:");
        out.println("  status: " + status(mustang.ok()));
        if (mustang.errors().isEmpty()) {
            out.println("  errors: []");
        } else {
            out.println("  errors:");
            for (String e : mustang.errors()) {
                out.println("    - " + yamlScalar(e));
            }
        }
        out.println("postcheck:");
        out.println("  status: " + status(postOk));
        out.println("  checks:");
        for (CheckResult r : checks) {
            out.println("    - name: " + yamlScalar(r.name()));
            out.println("      status: " + status(r.ok()));
            if (!r.ok() && r.message() != null) {
                out.println("      message: " + yamlScalar(r.message()));
            }
        }
    }

    private static void writeMd(PrintStream out, MustangReport mustang,
                                List<CheckResult> checks, boolean postOk, boolean overall) {
        out.println("# Factur-X Validation: " + status(overall).toUpperCase());
        out.println();
        out.println("## Mustang: " + status(mustang.ok()).toUpperCase());
        out.println();
        if (mustang.errors().isEmpty()) {
            out.println("No errors reported.");
        } else {
            for (String e : mustang.errors()) {
                out.println("- " + e.replace("\n", " "));
            }
        }
        out.println();
        out.println("## Postcheck: " + status(postOk).toUpperCase());
        out.println();
        out.println("| Check | Status | Message |");
        out.println("|---|---|---|");
        for (CheckResult r : checks) {
            String msg = r.message() == null ? "" : r.message().replace("|", "\\|").replace("\n", " ");
            out.println("| " + r.name() + " | " + status(r.ok()).toUpperCase() + " | " + msg + " |");
        }
    }

    private static void writeXml(PrintStream out, MustangReport mustang,
                                 List<CheckResult> checks, boolean postOk, boolean overall) {
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<validation status=\"" + status(overall) + "\">");
        out.println("  <mustang status=\"" + status(mustang.ok()) + "\">");
        for (String e : mustang.errors()) {
            out.println("    <error>" + xmlEscape(e) + "</error>");
        }
        out.println("  </mustang>");
        out.println("  <postcheck status=\"" + status(postOk) + "\">");
        for (CheckResult r : checks) {
            if (r.ok()) {
                out.println("    <check name=\"" + xmlEscape(r.name()) + "\" status=\"pass\"/>");
            } else {
                out.println("    <check name=\"" + xmlEscape(r.name()) + "\" status=\"fail\">"
                        + xmlEscape(r.message() == null ? "" : r.message()) + "</check>");
            }
        }
        out.println("  </postcheck>");
        out.println("</validation>");
    }

    private static String status(boolean ok) {
        return ok ? "pass" : "fail";
    }

    private static String yamlScalar(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    private static String xmlEscape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
