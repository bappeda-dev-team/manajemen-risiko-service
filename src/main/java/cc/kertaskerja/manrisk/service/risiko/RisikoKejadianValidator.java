package cc.kertaskerja.manrisk.service.risiko;

import cc.kertaskerja.manrisk.exception.RiskException;

import java.time.LocalDate;

final class RisikoKejadianValidator {
    private RisikoKejadianValidator() {
    }

    static void validate(Boolean risikoTerjadi, LocalDate waktuTerjadi) {
        boolean consistent = (Boolean.TRUE.equals(risikoTerjadi) && waktuTerjadi != null)
              || (Boolean.FALSE.equals(risikoTerjadi) && waktuTerjadi == null)
              || (risikoTerjadi == null && waktuTerjadi == null);
        if (!consistent) {
            invalid("Waktu terjadi wajib diisi hanya ketika risiko terjadi.");
        }
        if (waktuTerjadi != null && waktuTerjadi.isAfter(LocalDate.now())) {
            invalid("Waktu terjadi tidak boleh setelah hari ini.");
        }
    }

    private static void invalid(String message) {
        throw new RiskException(400, "RISK_OCCURRENCE_INVALID", message);
    }
}
