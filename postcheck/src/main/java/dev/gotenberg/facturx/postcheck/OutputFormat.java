package dev.gotenberg.facturx.postcheck;

public enum OutputFormat {
    TEXT, YAML, MD, XML;

    public static OutputFormat parse(String s) {
        if (s == null) return TEXT;
        return switch (s.toLowerCase()) {
            case "yaml" -> YAML;
            case "md", "markdown" -> MD;
            case "xml" -> XML;
            default -> throw new IllegalArgumentException("unknown format: " + s);
        };
    }
}
