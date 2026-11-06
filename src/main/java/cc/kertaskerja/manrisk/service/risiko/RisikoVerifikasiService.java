package cc.kertaskerja.manrisk.service.risiko;

import cc.kertaskerja.manrisk.dto.review.ReviewDecisionRequest;
import cc.kertaskerja.manrisk.dto.review.SubmitVerificationRequest;
import cc.kertaskerja.manrisk.entity.RisikoOpd;
import cc.kertaskerja.manrisk.entity.RisikoOperasional;
import cc.kertaskerja.manrisk.entity.RisikoPemda;
import cc.kertaskerja.manrisk.entity.VerifikasiRisiko;
import cc.kertaskerja.manrisk.exception.RiskException;
import cc.kertaskerja.manrisk.repository.RisikoOpdRepository;
import cc.kertaskerja.manrisk.repository.RisikoOperasionalRepository;
import cc.kertaskerja.manrisk.repository.RisikoPemdaRepository;
import cc.kertaskerja.manrisk.repository.VerifikasiRisikoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RisikoVerifikasiService {
    private static final String DRAFT = "DRAFT";
    private static final String SUBMITTED = "SUBMITTED";
    private static final String VERIFIED = "VERIFIED";
    private static final String REJECTED = "REJECTED";

    private final RisikoOperasionalRepository operasionalRepository;
    private final RisikoOpdRepository opdRepository;
    private final RisikoPemdaRepository pemdaRepository;
    private final VerifikasiRisikoRepository verifikasiRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> daftarPengajuan() {
        List<RiskRow> rows = new ArrayList<>();
        operasionalRepository.findAll().stream().filter(r -> r.getDeletedAt() == null).forEach(r -> rows.add(operasional(r)));
        opdRepository.findAll().stream().filter(r -> r.getDeletedAt() == null).forEach(r -> rows.add(opd(r)));
        pemdaRepository.findAll().stream().filter(r -> r.getDeletedAt() == null).forEach(r -> rows.add(pemda(r)));
        rows.sort(Comparator.comparing(RiskRow::createdAt, Comparator.nullsLast(Comparator.reverseOrder()))
              .thenComparing(RiskRow::kodeRisiko));
        List<Map<String, Object>> daftar = rows.stream().map(this::summary).toList();
        return Map.of("daftar_pengajuan", daftar);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detailPengajuan(String kodeRisiko) {
        RiskRow row = find(kodeRisiko);
        if (row.deleted()) notFound(kodeRisiko);
        return detail(row);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> daftarVerifikasi() {
        List<RiskRow> rows = new ArrayList<>();
        operasionalRepository.findAll().stream().filter(r -> r.getDeletedAt() == null).forEach(r -> rows.add(operasional(r)));
        opdRepository.findAll().stream().filter(r -> r.getDeletedAt() == null).forEach(r -> rows.add(opd(r)));
        pemdaRepository.findAll().stream().filter(r -> r.getDeletedAt() == null).forEach(r -> rows.add(pemda(r)));
        List<Map<String, Object>> daftar = rows.stream()
              .filter(r -> verification(r).isPresent())
              .sorted(Comparator.comparing((RiskRow r) -> verification(r).orElseThrow().getCreatedAt()).reversed()
                    .thenComparing(RiskRow::kodeRisiko))
              .map(this::summary).toList();
        return Map.of("daftar_verifikasi", daftar);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detailVerifikasi(String kodeRisiko) {
        RiskRow row = find(kodeRisiko);
        if (row.deleted() || verification(row).isEmpty()) notFound(kodeRisiko);
        return detail(row);
    }

    @Transactional
    public Map<String, Object> submit(SubmitVerificationRequest request) {
        RiskRow row = find(request.getKodeRisiko());
        if (row.deleted()) notFound(request.getKodeRisiko());
        if (verification(row).isPresent()) conflict("RISK_ALREADY_SUBMITTED", "Risiko sudah pernah diajukan.");
        validateComplete(row);
        row = setPemohon(row, normalized(request.getPemohon().getId()), normalized(request.getPemohon().getNama()));
        VerifikasiRisiko verification = VerifikasiRisiko.builder()
              .kodeRisiko(row.kodeRisiko()).status(SUBMITTED).createdAt(OffsetDateTime.now()).build();
        switch (row.scope()) {
            case "OPERASIONAL" -> verification.setRisikoOperasional((RisikoOperasional) row.source());
            case "OPD" -> verification.setRisikoOpd((RisikoOpd) row.source());
            case "PEMDA" -> verification.setRisikoPemda((RisikoPemda) row.source());
            default -> throw new IllegalStateException("Jenis risiko tidak dikenal.");
        }
        try {
            verifikasiRepository.saveAndFlush(verification);
        } catch (DataIntegrityViolationException exception) {
            conflict("RISK_ALREADY_SUBMITTED", "Risiko sudah pernah diajukan.");
        }
        return detail(row, verification);
    }

    private RiskRow setPemohon(RiskRow row, String id, String nama) {
        if (id == null || nama == null) invalid("Identitas pemohon wajib diisi.");
        switch (row.scope()) {
            case "OPERASIONAL" -> {
                RisikoOperasional risk = (RisikoOperasional) row.source();
                risk.setCreatedBy(id);
                risk.setCreatedName(nama);
            }
            case "OPD" -> {
                RisikoOpd risk = (RisikoOpd) row.source();
                risk.setCreatedBy(id);
                risk.setCreatedName(nama);
            }
            case "PEMDA" -> {
                RisikoPemda risk = (RisikoPemda) row.source();
                risk.setCreatedBy(id);
                risk.setCreatedName(nama);
            }
            default -> throw new IllegalStateException("Jenis risiko tidak dikenal.");
        }
        return new RiskRow(row.scope(), row.id(), row.kodeRisiko(), row.kodeOpd(), row.tahun(), row.pernyataanRisiko(),
              id, nama, row.createdJabatan(), row.revisiDari(), row.deleted(), row.createdAt(), row.source());
    }

    @Transactional
    public Map<String, Object> decide(String kodeRisiko, ReviewDecisionRequest request) {
        String target = normalizedDecision(request.getStatus());
        String catatan = normalized(request.getCatatan());
        if (REJECTED.equals(target) && catatan == null) invalid("Catatan penolakan wajib diisi.");
        if (catatan != null && catatan.length() > 2000) invalid("Catatan maksimal 2.000 karakter.");
        VerifikasiRisiko verification = verifikasiRepository.findLockedByKodeRisiko(normalizedKode(kodeRisiko))
              .orElseThrow(() -> new RiskException(404, "RISK_NOT_FOUND", "Risiko tidak ditemukan."));
        RiskRow row = fromVerification(verification);
        if (row.deleted()) notFound(kodeRisiko);
        if (!SUBMITTED.equals(verification.getStatus())) conflict("RISK_DECISION_ALREADY_MADE", "Keputusan verifikasi sudah diambil.");
        verification.setStatus(target);
        verification.setVerifikatorId(normalized(request.getVerifikator().getId()));
        verification.setVerifikatorNama(normalized(request.getVerifikator().getNama()));
        verification.setCatatan(catatan);
        verifikasiRepository.save(verification);
        return detail(row, verification);
    }

    private Map<String, Object> summary(RiskRow row) {
        Map<String, Object> result = new LinkedHashMap<>();
        Optional<VerifikasiRisiko> verification = verification(row);
        result.put("jenis", row.scope());
        result.put("id", row.id());
        result.put("kode_risiko", row.kodeRisiko());
        result.put("tahun", row.tahun());
        result.put("pernyataan_risiko", row.pernyataanRisiko());
        result.put("status", verification.map(VerifikasiRisiko::getStatus).orElse(DRAFT));
        result.put("revisi_dari", row.revisiDari());
        result.put("verifikasi", verification.map(this::verificationView).orElse(null));
        result.put("pemohon", person(row));
        return result;
    }

    private Map<String, Object> detail(RiskRow row) {
        return detail(row, verification(row).orElse(null));
    }

    private Map<String, Object> detail(RiskRow row, VerifikasiRisiko verification) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", verification == null ? DRAFT : verification.getStatus());
        result.put("verifikasi", verification == null ? null : verificationView(verification));
        result.put("pemohon", person(row));
        result.put("revisi_dari", revisionView(row));
        result.put("risiko", riskView(row));
        return result;
    }

    private Map<String, Object> verificationView(VerifikasiRisiko verification) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", verification.getId());
        result.put("created_at", verification.getCreatedAt());
        result.put("verifikator", verification.getVerifikatorId() == null ? null : Map.of(
              "id", verification.getVerifikatorId(), "nama", verification.getVerifikatorNama()));
        result.put("catatan", verification.getCatatan());
        return result;
    }

    private Map<String, Object> person(RiskRow row) {
        if (row.createdBy() == null) return null;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", row.createdBy());
        result.put("nama", row.createdName());
        result.put("jabatan", row.createdJabatan());
        return result;
    }

    private Object revisionView(RiskRow row) {
        if (row.revisiDari() == null) return null;
        try {
            RiskRow parent = find(row.revisiDari());
            Optional<VerifikasiRisiko> verification = verification(parent);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("kode_risiko", parent.kodeRisiko());
            result.put("status", verification.map(VerifikasiRisiko::getStatus).orElse(DRAFT));
            result.put("catatan", verification.map(VerifikasiRisiko::getCatatan).orElse(null));
            return result;
        } catch (RiskException ignored) {
            return Map.of("kode_risiko", row.revisiDari());
        }
    }

    private Map<String, Object> riskView(RiskRow row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", row.id());
        result.put("kode_risiko", row.kodeRisiko());
        result.put("jenis", row.scope());
        result.put("kode_opd", row.kodeOpd());
        result.put("tahun", row.tahun());
        result.put("pernyataan_risiko", row.pernyataanRisiko());
        if (row.source() instanceof RisikoOpd risk) {
            result.put("kode_sasaran_opd", risk.getKodeSasaranOpd());
            businessFields(result, risk.getPermasalahan(), risk.getSebabPermasalahan(), risk.getPengendalianYangSudahAda(),
                  risk.getSkalaKemungkinan(), risk.getSkalaDampak(), risk.getRencanaTindakPengendalian(), risk.getMetodePemantauan(), risk.getPenanggungjawabPemantauan());
        } else if (row.source() instanceof RisikoPemda risk) {
            result.put("kode_sasaran_pemda", risk.getKodeSasaranPemda());
            businessFields(result, risk.getPermasalahan(), risk.getSebabPermasalahan(), risk.getPengendalianYangSudahAda(),
                  risk.getSkalaKemungkinan(), risk.getSkalaDampak(), risk.getRencanaTindakPengendalian(), risk.getMetodePemantauan(), risk.getPenanggungjawabPemantauan());
        } else if (row.source() instanceof RisikoOperasional risk) {
            result.put("kode_rekin", risk.getKodeRekin());
            result.put("pegawai_id", risk.getPegawaiId());
            businessFields(result, risk.getPermasalahan(), risk.getSebabPermasalahan(), risk.getPengendalianYangSudahAda(),
                  risk.getSkalaKemungkinan(), risk.getSkalaDampak(), risk.getRencanaTindakPengendalian(), risk.getMetodePemantauan(), risk.getPenanggungjawabPemantauan());
        }
        return result;
    }

    private static void businessFields(Map<String, Object> result, String permasalahan, String sebab, String pengendalian,
                                       Integer kemungkinan, Integer dampak, String rtp, String metode, String penanggungjawab) {
        result.put("permasalahan", permasalahan);
        result.put("sebab_permasalahan", sebab);
        result.put("pengendalian_yang_sudah_ada", pengendalian);
        result.put("skala_kemungkinan", kemungkinan);
        result.put("skala_dampak", dampak);
        result.put("rencana_tindak_pengendalian", rtp);
        result.put("metode_pemantauan", metode);
        result.put("penanggungjawab_pemantauan", penanggungjawab);
    }

    private Optional<VerifikasiRisiko> verification(RiskRow row) {
        return verifikasiRepository.findByKodeRisiko(row.kodeRisiko());
    }

    private RiskRow fromVerification(VerifikasiRisiko verification) {
        if (verification.getRisikoOperasional() != null) return operasional(verification.getRisikoOperasional());
        if (verification.getRisikoOpd() != null) return opd(verification.getRisikoOpd());
        if (verification.getRisikoPemda() != null) return pemda(verification.getRisikoPemda());
        throw new IllegalStateException("Verifikasi tidak memiliki risiko.");
    }

    private RiskRow find(String kodeRisiko) {
        String kode = normalizedKode(kodeRisiko);
        Optional<RisikoOperasional> operasional = operasionalRepository.findByKodeRisiko(kode);
        if (operasional.isPresent()) return operasional(operasional.get());
        Optional<RisikoOpd> opd = opdRepository.findByKodeRisiko(kode);
        if (opd.isPresent()) return opd(opd.get());
        Optional<RisikoPemda> pemda = pemdaRepository.findByKodeRisiko(kode);
        if (pemda.isPresent()) return pemda(pemda.get());
        throw new RiskException(404, "RISK_NOT_FOUND", "Risiko tidak ditemukan.");
    }

    private static RiskRow operasional(RisikoOperasional risk) {
        return new RiskRow("OPERASIONAL", risk.getId(), risk.getKodeRisiko(), risk.getKodeOpd(), risk.getTahun(), risk.getPernyataanRisiko(),
              risk.getCreatedBy(), risk.getCreatedName(), risk.getCreatedJabatan(), risk.getRevisiDari(), risk.getDeletedAt() != null, risk.getCreatedAt(), risk);
    }

    private static RiskRow opd(RisikoOpd risk) {
        return new RiskRow("OPD", risk.getId(), risk.getKodeRisiko(), risk.getKodeOpd(), risk.getTahun(), risk.getPernyataanRisiko(),
              risk.getCreatedBy(), risk.getCreatedName(), risk.getCreatedJabatan(), risk.getRevisiDari(), risk.getDeletedAt() != null, risk.getCreatedAt(), risk);
    }

    private static RiskRow pemda(RisikoPemda risk) {
        return new RiskRow("PEMDA", risk.getId(), risk.getKodeRisiko(), risk.getKodeOpd(), risk.getTahun(), risk.getPernyataanRisiko(),
              risk.getCreatedBy(), risk.getCreatedName(), risk.getCreatedJabatan(), risk.getRevisiDari(), risk.getDeletedAt() != null, risk.getCreatedAt(), risk);
    }

    private static void validateComplete(RiskRow row) {
        boolean complete = hasText(row.pernyataanRisiko());
        if (row.source() instanceof RisikoOpd risk) complete &= risk.getSkalaKemungkinan() != null && risk.getSkalaDampak() != null
              && hasText(risk.getRencanaTindakPengendalian()) && hasText(risk.getKodePerangkatYangMenangani());
        if (row.source() instanceof RisikoPemda risk) complete &= risk.getSkalaKemungkinan() != null && risk.getSkalaDampak() != null
              && hasText(risk.getRencanaTindakPengendalian()) && hasText(risk.getKodePerangkatYangMenangani());
        if (row.source() instanceof RisikoOperasional risk) complete &= risk.getSkalaKemungkinan() != null && risk.getSkalaDampak() != null
              && hasText(risk.getRencanaTindakPengendalian()) && hasText(risk.getKodePerangkatYangMenangani());
        if (!complete) throw new RiskException(422, "RISK_SUBMISSION_INCOMPLETE", "Lengkapi data risiko sebelum mengajukan.");
    }

    private static String normalizedDecision(String status) {
        String value = normalized(status);
        if (!VERIFIED.equals(value) && !REJECTED.equals(value)) invalid("Status keputusan harus VERIFIED atau REJECTED.");
        return value;
    }

    private static String normalizedKode(String value) {
        String result = normalized(value);
        if (result == null || result.length() > 255) invalid("Kode risiko tidak valid.");
        return result;
    }

    private static String normalized(String value) {
        if (value == null) return null;
        String result = value.trim();
        return result.isEmpty() ? null : result;
    }

    private static boolean hasText(String value) { return normalized(value) != null; }
    private static void notFound(String kode) { throw new RiskException(404, "RISK_NOT_FOUND", "Risiko tidak ditemukan."); }
    private static void conflict(String code, String message) { throw new RiskException(409, code, message); }
    private static void invalid(String message) { throw new RiskException(422, "RISK_REVIEW_INVALID", message); }

    private record RiskRow(String scope, Long id, String kodeRisiko, String kodeOpd, Integer tahun, String pernyataanRisiko,
                           String createdBy, String createdName, String createdJabatan, String revisiDari, boolean deleted,
                           java.time.LocalDateTime createdAt, Object source) { }
}
