package cc.kertaskerja.manrisk.security;

@FunctionalInterface
public interface RisikoSessionValidator {
    RisikoAuthenticatedUser validate(String sessionId);
}
