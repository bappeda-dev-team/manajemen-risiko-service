package cc.kertaskerja.manrisk.security;

import java.util.List;

public record RisikoAuthenticatedUser(
      String username,
      String firstName,
      String kodeOpd,
      String nip,
      List<String> roles) {

    public String callerId() {
        if (username != null && !username.isBlank()) return username;
        return nip;
    }
}
