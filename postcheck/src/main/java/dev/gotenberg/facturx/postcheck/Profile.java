package dev.gotenberg.facturx.postcheck;

public record Profile(String xmpLevel, String ciiLevel, String urn) {

    public boolean isMinimum() {
        return "MINIMUM".equals(ciiLevel);
    }
}
