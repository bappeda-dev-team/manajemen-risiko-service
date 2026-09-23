package cc.kertaskerja.manrisk.exception;

import lombok.Getter;

@Getter
public class RiskException extends RuntimeException {
    private final int status;
    private final String code;

    public RiskException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
