package dev.gotenberg.facturx.postcheck;

public record CheckResult(String name, boolean ok, String message) {

    public static CheckResult pass(String name) {
        return new CheckResult(name, true, null);
    }

    public static CheckResult fail(String name, String message) {
        return new CheckResult(name, false, message);
    }
}
