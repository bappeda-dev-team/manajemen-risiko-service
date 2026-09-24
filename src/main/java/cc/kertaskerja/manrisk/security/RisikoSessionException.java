package cc.kertaskerja.manrisk.security;

public class RisikoSessionException extends RuntimeException {
    public enum Reason { UNAUTHORIZED, UNAVAILABLE }

    private final Reason reason;

    public RisikoSessionException(Reason reason) {
        this.reason = reason;
    }

    public RisikoSessionException(Reason reason, Throwable cause) {
        super(cause);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
