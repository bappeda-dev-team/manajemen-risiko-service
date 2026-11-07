package cc.kertaskerja.manrisk.security;

import org.springframework.security.core.context.SecurityContextHolder;

public final class RiskPrincipalContext {
    private RiskPrincipalContext() { }

    public static RiskPrincipal current() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null
              ? null
              : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal instanceof RiskPrincipal value ? value : null;
    }
}
