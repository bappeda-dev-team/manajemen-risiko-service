package cc.kertaskerja.manrisk.security;

import java.util.Set;

/**
 * Placeholder model retained for a future Mahulu authentication integration.
 * It is not constructed or used while Manrisk runs in no-auth mode.
 */
public record RiskPrincipal(String id, String nama, String jabatan, String kodeOpd, Set<String> roles) {
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}
