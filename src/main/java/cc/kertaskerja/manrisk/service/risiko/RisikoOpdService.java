package cc.kertaskerja.manrisk.service.risiko;

import cc.kertaskerja.manrisk.dto.RisikoOpd.RisikoOpdReqDTO;
import cc.kertaskerja.manrisk.dto.RisikoOpd.RisikoOpdResDTO;
import cc.kertaskerja.manrisk.entity.RisikoOpd;
import cc.kertaskerja.manrisk.exception.RiskException;
import cc.kertaskerja.manrisk.repository.RisikoOpdRepository;
import cc.kertaskerja.manrisk.repository.VerifikasiRisikoRepository;
import cc.kertaskerja.manrisk.security.RiskPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RisikoOpdService {
    private static final String SCOPE = "opd";
    private static final Set<String> SUPPORTED_TYPES = Set.of(
          "identifikasi", "analisis", "pengendalian", "pemantauan", "hasil-pemantauan");
    private static final Pattern LEGACY_KODE_RISIKO = Pattern.compile("RSK-(\\d{4,})");

    private final RisikoOpdRepository risikoOpdRepository;
    private final VerifikasiRisikoRepository verifikasiRisikoRepository;

    public List<RisikoOpdResDTO> getAllRisiko() {
        return risikoOpdRepository.findAll().stream().map(this::toResDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<RisikoOpdResDTO> getRisikoLaporan(int tahun, String kodeOpd, List<String> kodeSasaran) {
        int selectedYear = RisikoLaporanFilter.year(tahun);
        String selectedOpd = RisikoLaporanFilter.opd(kodeOpd);
        List<String> selectedCodes = RisikoLaporanFilter.sasaran(kodeSasaran);
        List<RisikoOpd> records = selectedCodes.isEmpty()
              ? risikoOpdRepository.findVerifiedForLaporan(selectedOpd, selectedYear)
              : risikoOpdRepository.findVerifiedForLaporanBySasaran(
                    selectedOpd, selectedYear, selectedCodes);
        return records.stream().map(this::toResDTO).toList();
    }

    public RisikoOpdResDTO getRisikoByKodeSasaranOpd(String kodeSasaranOpd, String type) {
        String normalizedType = normalizedType(type);
        String normalizedKodeSasaran = requiredPath(kodeSasaranOpd, "Kode sasaran OPD tidak valid.");
        List<RisikoOpd> records = risikoOpdRepository
              .findByKodeSasaranOpdOrderByIdAsc(normalizedKodeSasaran);

        if (records.isEmpty()) {
            return RisikoOpdResDTO.builder()
                  .scope(SCOPE)
                  .kodeSasaranOpd(normalizedKodeSasaran)
                  .risiko(List.of())
                  .build();
        }

        RisikoOpd first = records.getFirst();
        return RisikoOpdResDTO.builder()
              .scope(SCOPE)
              .kodeOpd(first.getKodeOpd())
              .tahun(first.getTahun())
              .kodeSasaranOpd(first.getKodeSasaranOpd())
              .risiko(records.stream().map(record -> toRisikoItem(record, normalizedType)).toList())
              .build();
    }

    public RisikoOpdResDTO getRisikoByKodeRisiko(String kodeRisiko) {
        String canonicalKode = canonicalKodeRisiko(kodeRisiko);
        return risikoOpdRepository.findByKodeRisiko(canonicalKode)
              .map(this::toResDTO)
              .orElseThrow(() -> notFound("Risiko OPD tidak ditemukan dengan kode: " + canonicalKode));
    }

    @Transactional
    public RisikoOpdResDTO createRisiko(RisikoOpdReqDTO request) {
        RisikoOpd saved = risikoOpdRepository.saveAndFlush(toEntity(request));
        saved.setKodeRisiko(formatKodeRisiko(saved.getId()));
        return toResDTO(risikoOpdRepository.save(saved));
    }

    @Transactional
    public RisikoOpdResDTO createRisiko(RisikoOpdReqDTO request, RiskPrincipal actor) {
        requireLevel3(actor);
        if (!Objects.equals(normalized(request.getKodeOpd()), actor.kodeOpd())) {
            throw new RiskException(403, "RISK_ACCESS_DENIED", "Kode OPD harus sesuai OPD pemohon.");
        }
        validateRevision(request.getRevisiDari(), actor, normalized(request.getKodeOpd()));
        RisikoOpd saved = toEntity(request);
        if (normalized(request.getRevisiDari()) != null) saved.setRevisiDari(canonicalKodeRisiko(request.getRevisiDari()));
        saved.setCreatedBy(actor.id());
        saved.setCreatedName(actor.nama());
        saved.setCreatedJabatan(actor.jabatan());
        saved = risikoOpdRepository.saveAndFlush(saved);
        saved.setKodeRisiko(formatKodeRisiko(saved.getId()));
        return toResDTO(risikoOpdRepository.save(saved));
    }

    @Transactional
    public RisikoOpdResDTO updateRisiko(Long id, RisikoOpdReqDTO request) {
        RisikoOpd existing = findById(id);
        if (!Objects.equals(normalized(existing.getKodeOpd()), normalized(request.getKodeOpd()))
              || !Objects.equals(existing.getTahun(), request.getTahun())
              || !Objects.equals(normalized(existing.getKodeSasaranOpd()),
              normalized(request.getKodeSasaranOpd()))) {
            throw new RiskException(409, "RISK_REFERENCE_IMMUTABLE",
                  "Kode OPD, tahun, dan kode sasaran OPD tidak dapat diubah.");
        }

        applyMutableFields(existing, request);
        return toResDTO(risikoOpdRepository.save(existing));
    }

    @Transactional
    public RisikoOpdResDTO updateRisiko(Long id, RisikoOpdReqDTO request, RiskPrincipal actor) {
        RisikoOpd existing = ownedEditable(id, actor, false);
        if (!Objects.equals(normalized(existing.getKodeOpd()), normalized(request.getKodeOpd()))
              || !Objects.equals(existing.getTahun(), request.getTahun())
              || !Objects.equals(normalized(existing.getKodeSasaranOpd()), normalized(request.getKodeSasaranOpd()))) {
            throw new RiskException(409, "RISK_REFERENCE_IMMUTABLE", "Kode OPD, tahun, dan kode sasaran OPD tidak dapat diubah.");
        }
        applyMutableFields(existing, request);
        return toResDTO(risikoOpdRepository.save(existing));
    }

    @Transactional
    public void deleteRisiko(Long id) {
        risikoOpdRepository.delete(findById(id));
    }

    @Transactional
    public void deleteRisiko(Long id, RiskPrincipal actor) {
        RisikoOpd existing = ownedEditable(id, actor, true);
        existing.setDeletedAt(java.time.OffsetDateTime.now());
        risikoOpdRepository.save(existing);
    }

    private RisikoOpd findById(Long id) {
        if (id == null || id < 1) {
            throw new RiskException(400, "RISK_INVALID_INPUT", "ID risiko tidak valid.");
        }
        return risikoOpdRepository.findById(id)
              .orElseThrow(() -> notFound("Risiko OPD tidak ditemukan dengan ID: " + id));
    }

    private RisikoOpdResDTO.RisikoItem toRisikoItem(RisikoOpd risiko, String type) {
        RisikoOpdResDTO.RisikoItem.RisikoItemBuilder builder = RisikoOpdResDTO.RisikoItem.builder()
              .id(risiko.getId())
              .kodeRisiko(risiko.getKodeRisiko())
              .type(type)
              .permasalahan(risiko.getPermasalahan())
              .sebabPermasalahan(risiko.getSebabPermasalahan())
              .pernyataanRisiko(risiko.getPernyataanRisiko())
              .pengendalianYangSudahAda(risiko.getPengendalianYangSudahAda());

        if (!"identifikasi".equals(type)) {
            builder.skalaKemungkinan(risiko.getSkalaKemungkinan())
                  .skalaDampak(risiko.getSkalaDampak())
                  .pihakTerkenaRisiko(risiko.getPihakTerkenaRisiko())
                  .rencanaTindakPengendalian(risiko.getRencanaTindakPengendalian())
                  .metodePemantauan(risiko.getMetodePemantauan())
                  .penanggungjawabPemantauan(risiko.getPenanggungjawabPemantauan())
                  .keterangan(risiko.getKeterangan())
                  .realisasiTindakPengendalian(risiko.getRealisasiTindakPengendalian())
                  .dapatTerkendali(risiko.getDapatTerkendali())
                  .dampak(risiko.getDampak())
                  .catatan(risiko.getCatatan())
                  .perangkatYangMenangani(risiko.getPerangkatYangMenangani())
                  .kodePerangkatYangMenangani(risiko.getKodePerangkatYangMenangani())
                  .risikoTerjadi(risiko.getRisikoTerjadi())
                  .waktuTerjadi(risiko.getWaktuTerjadi())
                  .createdAt(risiko.getCreatedAt())
                  .updatedAt(risiko.getUpdatedAt());
        }

        return builder.build();
    }

    private RisikoOpdResDTO toResDTO(RisikoOpd risiko) {
        return RisikoOpdResDTO.builder()
              .scope(SCOPE)
              .id(risiko.getId())
              .kodeOpd(risiko.getKodeOpd())
              .kodeRisiko(risiko.getKodeRisiko())
              .tahun(risiko.getTahun())
              .kodeSasaranOpd(risiko.getKodeSasaranOpd())
              .permasalahan(risiko.getPermasalahan())
              .sebabPermasalahan(risiko.getSebabPermasalahan())
              .pernyataanRisiko(risiko.getPernyataanRisiko())
              .skalaKemungkinan(risiko.getSkalaKemungkinan())
              .skalaDampak(risiko.getSkalaDampak())
              .pihakTerkenaRisiko(risiko.getPihakTerkenaRisiko())
              .rencanaTindakPengendalian(risiko.getRencanaTindakPengendalian())
              .metodePemantauan(risiko.getMetodePemantauan())
              .penanggungjawabPemantauan(risiko.getPenanggungjawabPemantauan())
              .keterangan(risiko.getKeterangan())
              .realisasiTindakPengendalian(risiko.getRealisasiTindakPengendalian())
              .dapatTerkendali(risiko.getDapatTerkendali())
              .dampak(risiko.getDampak())
              .catatan(risiko.getCatatan())
              .perangkatYangMenangani(risiko.getPerangkatYangMenangani())
              .kodePerangkatYangMenangani(risiko.getKodePerangkatYangMenangani())
              .pengendalianYangSudahAda(risiko.getPengendalianYangSudahAda())
              .risikoTerjadi(risiko.getRisikoTerjadi())
              .waktuTerjadi(risiko.getWaktuTerjadi())
              .createdAt(risiko.getCreatedAt())
              .updatedAt(risiko.getUpdatedAt())
              .build();
    }

    private RisikoOpd toEntity(RisikoOpdReqDTO request) {
        RisikoOpd risiko = RisikoOpd.builder()
              .kodeOpd(normalized(request.getKodeOpd()))
              .tahun(request.getTahun())
              .kodeSasaranOpd(normalized(request.getKodeSasaranOpd()))
              .revisiDari(normalized(request.getRevisiDari()))
              .build();
        applyMutableFields(risiko, request);
        return risiko;
    }

    private void applyMutableFields(RisikoOpd risiko, RisikoOpdReqDTO request) {
        RisikoKejadianValidator.validate(request.getRisikoTerjadi(), request.getWaktuTerjadi());
        risiko.setPermasalahan(normalized(request.getPermasalahan()));
        risiko.setSebabPermasalahan(normalized(request.getSebabPermasalahan()));
        risiko.setPernyataanRisiko(normalized(request.getPernyataanRisiko()));
        risiko.setSkalaKemungkinan(request.getSkalaKemungkinan());
        risiko.setSkalaDampak(request.getSkalaDampak());
        risiko.setPihakTerkenaRisiko(normalized(request.getPihakTerkenaRisiko()));
        risiko.setRencanaTindakPengendalian(normalized(request.getRencanaTindakPengendalian()));
        risiko.setMetodePemantauan(normalized(request.getMetodePemantauan()));
        risiko.setPenanggungjawabPemantauan(normalized(request.getPenanggungjawabPemantauan()));
        risiko.setKeterangan(normalized(request.getKeterangan()));
        risiko.setRealisasiTindakPengendalian(normalized(request.getRealisasiTindakPengendalian()));
        risiko.setDapatTerkendali(normalized(request.getDapatTerkendali()));
        risiko.setDampak(normalized(request.getDampak()));
        risiko.setCatatan(normalized(request.getCatatan()));
        risiko.setPerangkatYangMenangani(normalized(request.getPerangkatYangMenangani()));
        risiko.setKodePerangkatYangMenangani(normalized(request.getKodePerangkatYangMenangani()));
        risiko.setPengendalianYangSudahAda(normalized(request.getPengendalianYangSudahAda()));
        risiko.setRisikoTerjadi(request.getRisikoTerjadi());
        risiko.setWaktuTerjadi(request.getWaktuTerjadi());
    }

    private String normalizedType(String type) {
        String value = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_TYPES.contains(value)) {
            throw new RiskException(400, "RISK_TYPE_INVALID", "Tipe tab risiko tidak dikenali.");
        }
        return value;
    }

    private String canonicalKodeRisiko(String kodeRisiko) {
        String value = requiredPath(kodeRisiko, "Kode risiko tidak valid.");
        Matcher matcher = LEGACY_KODE_RISIKO.matcher(value);
        return matcher.matches() ? "RSK-OPD-" + matcher.group(1) : value;
    }

    private String requiredPath(String value, String message) {
        String normalized = normalized(value);
        if (normalized == null || normalized.isEmpty()) {
            throw new RiskException(400, "RISK_INVALID_INPUT", message);
        }
        return normalized;
    }

    private String formatKodeRisiko(Long id) {
        return String.format("RSK-OPD-%04d", id);
    }

    private String normalized(String value) {
        return value == null ? null : value.trim();
    }

    private RiskException notFound(String message) {
        return new RiskException(404, "RISK_NOT_FOUND", message);
    }

    private RisikoOpd ownedEditable(Long id, RiskPrincipal actor, boolean deleting) {
        requireLevel3(actor);
        RisikoOpd risk = findById(id);
        if (!actor.id().equals(risk.getCreatedBy()) || risk.getDeletedAt() != null) {
            throw new RiskException(404, "RISK_NOT_FOUND", "Risiko tidak ditemukan.");
        }
        var verification = verifikasiRisikoRepository.findByKodeRisiko(risk.getKodeRisiko());
        if (verification.isEmpty()) return risk;
        String status = verification.get().getStatus();
        boolean allowed = deleting ? "REJECTED".equals(status) : "VERIFIED".equals(status);
        if (!allowed) throw new RiskException(409, "RISK_LOCKED", "Risiko tidak dapat diubah pada status saat ini.");
        return risk;
    }

    private void validateRevision(String revisiDari, RiskPrincipal actor, String kodeOpd) {
        String kode = normalized(revisiDari);
        if (kode == null) return;
        RisikoOpd parent = risikoOpdRepository.findByKodeRisiko(canonicalKodeRisiko(kode))
              .orElseThrow(() -> new RiskException(422, "RISK_REVISION_INVALID", "Risiko asal revisi tidak ditemukan."));
        if (!actor.id().equals(parent.getCreatedBy()) || !Objects.equals(kodeOpd, parent.getKodeOpd())
              || verifikasiRisikoRepository.findByKodeRisiko(parent.getKodeRisiko()).filter(v -> "REJECTED".equals(v.getStatus())).isEmpty()) {
            throw new RiskException(422, "RISK_REVISION_INVALID", "Risiko asal revisi harus risiko ditolak milik pemohon pada OPD yang sama.");
        }
    }

    private void requireLevel3(RiskPrincipal actor) {
        if (!actor.hasRole("level_3")) {
            throw new RiskException(403, "RISK_ACCESS_DENIED", "Akun tidak memiliki akses untuk membuat risiko.");
        }
    }
}
