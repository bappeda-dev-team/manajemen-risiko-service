package cc.kertaskerja.manrisk.exception;

public class UnauthenticationException extends RuntimeException {

    public UnauthenticationException(String message) {
        super(message);
    }

    public UnauthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}