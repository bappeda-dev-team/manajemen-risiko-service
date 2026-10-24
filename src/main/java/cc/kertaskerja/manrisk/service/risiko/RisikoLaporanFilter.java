package cc.kertaskerja.manrisk.service.risiko;

import cc.kertaskerja.manrisk.exception.RiskException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

final class RisikoLaporanFilter {
    private static final int MAX_CODES = 100;
    private static final int MAX_TOTAL_CODE_LENGTH = 4096;

    private RisikoLaporanFilter() {
    }

    static int year(int tahun) {
        if (tahun < 1900 || tahun > 2100) {
            throw invalid("Tahun penilaian harus berada antara 1900 dan 2100.");
        }
        return tahun;
    }

    static String opd(String kodeOpd) {
        if (kodeOpd == null || kodeOpd.isBlank() || kodeOpd.trim().length() > 128) {
            throw invalid("Kode OPD wajib diisi dan maksimal 128 karakter.");
        }
        return kodeOpd.trim();
    }

    // null means that the query parameter was absent; an explicitly empty value is invalid.
    static List<String> sasaran(List<String> rawCodes) {
        if (rawCodes == null) return List.of();
        if (rawCodes.isEmpty() || rawCodes.size() > MAX_CODES) {
            throw invalid("Jumlah kode sasaran harus antara 1 dan " + MAX_CODES + ".");
        }

        LinkedHashSet<String> unique = new LinkedHashSet<>();
        int totalLength = 0;
        for (String rawCode : rawCodes) {
            if (rawCode == null || rawCode.isBlank()) {
                throw invalid("Kode sasaran tidak boleh kosong.");
            }
            String code = rawCode.trim();
            if (code.length() > 128 || code.indexOf(',') >= 0 || code.chars().anyMatch(Character::isISOControl)) {
                throw invalid("Kode sasaran tidak valid atau melebihi 128 karakter.");
            }
            totalLength += code.length();
            if (totalLength > MAX_TOTAL_CODE_LENGTH) {
                throw invalid("Total panjang kode sasaran melebihi batas laporan.");
            }
            unique.add(code);
        }
        return List.copyOf(new ArrayList<>(unique));
    }

    private static RiskException invalid(String message) {
        return new RiskException(400, "RISK_INVALID_INPUT", message);
    }
}
