package cc.kertaskerja.manrisk.service.risiko;

import cc.kertaskerja.manrisk.dto.RisikoPemda.RisikoPemdaReqDTO;
import cc.kertaskerja.manrisk.dto.RisikoPemda.RisikoPemdaResDTO;
import cc.kertaskerja.manrisk.entity.RisikoPemda;
import cc.kertaskerja.manrisk.exception.RiskException;
import cc.kertaskerja.manrisk.repository.RisikoPemdaRepository;
import cc.kertaskerja.manrisk.repository.VerifikasiRisikoRepository;
import cc.kertaskerja.manrisk.security.RiskPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RisikoPemdaService {
    private static final String SCOPE = "pemda";
    private static final Set<String> SUPPORTED_TYPES = Set.of(
          "identifikasi", "analisis", "pengendalian", "pemantauan", "hasil-pemantauan");

    private final RisikoPemdaRepository risikoPemdaRepository;
    private final VerifikasiRisikoRepository verifikasiRisikoRepository;

    public List<RisikoPemdaResDTO> getAllRisiko() {
        return risikoPemdaRepository.findAll().stream().map(this::toResDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<RisikoPemdaResDTO> getRisikoLaporan(int tahun, List<String> kodeSasaran) {
        int selectedYear = RisikoLaporanFilter.year(tahun);
        List<String> selectedCodes = RisikoLaporanFilter.sasaran(kodeSasaran);
        List<RisikoPemda> records = selectedCodes.isEmpty()
              ? risikoPemdaRepository.findVerifiedForLaporan(selectedYear)
              : risikoPemdaRepository.findVerifiedForLaporanBySasaran(
                    selectedYear, selectedCodes);
        return records.stream().map(this::toResDTO).toList();
    }

    public RisikoPemdaResDTO getRisikoByKodeSasaranPemda(String kodeSasaranPemda, String type) {
        String normalizedType = normalizedType(type);
        String normalizedKode = requiredPath(kodeSasaranPemda, "Kode sasaran Pemda tidak valid.");
        List<RisikoPemda> records = risikoPemdaRepository
              .findByKodeSasaranPemdaOrderByIdAsc(normalizedKode);

        if (records.isEmpty()) {
            return RisikoPemdaResDTO.builder()
                  .scope(SCOPE)
                  .kodeSasaranPemda(normalizedKode)
                  .risiko(List.of())
                  .build();
        }

        RisikoPemda first = records.get(0);
        return RisikoPemdaResDTO.builder()
              .scope(SCOPE)
              .tahun(first.getTahun())
              .kodeSasaranPemda(first.getKodeSasaranPemda())
              .risiko(records.stream().map(record -> toRisikoItem(record, normalizedType)).toList())
              .build();
    }

    public RisikoPemdaResDTO getRisikoByKodeRisiko(String kodeRisiko) {
        String normalizedKode = requiredPath(kodeRisiko, "Kode risiko tidak valid.");
        return risikoPemdaRepository.findByKodeRisiko(normalizedKode)
              .map(this::toResDTO)
              .orElseThrow(() -> notFound("Risiko Pemda tidak ditemukan dengan kode: " + normalizedKode));
    }

    @Transactional
    public RisikoPemdaResDTO createRisiko(RisikoPemdaReqDTO request) {
        RisikoPemda saved = risikoPemdaRepository.saveAndFlush(toEntity(request));
        saved.setKodeRisiko(formatKodeRisiko(saved.getId()));
        return toResDTO(risikoPemdaRepository.save(saved));
    }

    @Transactional
    public RisikoPemdaResDTO createRisiko(RisikoPemdaReqDTO request, RiskPrincipal actor) {
        requireLevel3(actor);
        validateRevision(request.getRevisiDari(), actor);
        RisikoPemda saved = toEntity(request);
        saved.setKodeOpd(actor.kodeOpd());
        saved.setCreatedBy(actor.id());
        saved.setCreatedName(actor.nama());
        saved.setCreatedJabatan(actor.jabatan());
        saved = risikoPemdaRepository.saveAndFlush(saved);
        saved.setKodeRisiko(formatKodeRisiko(saved.getId()));
        return toResDTO(risikoPemdaRepository.save(saved));
    }

    @Transactional
    public RisikoPemdaResDTO updateRisiko(Long id, RisikoPemdaReqDTO request) {
        RisikoPemda existing = findById(id);
        if (!Objects.equals(existing.getTahun(), request.getTahun())
              || !Objects.equals(normalized(existing.getKodeSasaranPemda()), normalized(request.getKodeSasaranPemda()))) {
            throw new RiskException(409, "RISK_REFERENCE_IMMUTABLE",
                  "Tahun dan kode sasaran Pemda tidak dapat diubah.");
        }

        applyMutableFields(existing, request);
        return toResDTO(risikoPemdaRepository.save(existing));
    }

    @Transactional
    public RisikoPemdaResDTO updateRisiko(Long id, RisikoPemdaReqDTO request, RiskPrincipal actor) {
        RisikoPemda existing = ownedEditable(id, actor, false);
        if (!Objects.equals(existing.getTahun(), request.getTahun())
              || !Objects.equals(normalized(existing.getKodeSasaranPemda()), normalized(request.getKodeSasaranPemda()))) {
            throw new RiskException(409, "RISK_REFERENCE_IMMUTABLE", "Tahun dan kode sasaran Pemda tidak dapat diubah.");
        }
        applyMutableFields(existing, request);
        return toResDTO(risikoPemdaRepository.save(existing));
    }

    @Transactional
    public void deleteRisiko(Long id) {
        risikoPemdaRepository.delete(findById(id));
    }

    @Transactional
    public void deleteRisiko(Long id, RiskPrincipal actor) {
        RisikoPemda existing = ownedEditable(id, actor, true);
        existing.setDeletedAt(java.time.OffsetDateTime.now());
        risikoPemdaRepository.save(existing);
    }

    private RisikoPemda findById(Long id) {
        if (id == null || id < 1) {
            throw new RiskException(400, "RISK_INVALID_INPUT", "ID risiko tidak valid.");
        }
        return risikoPemdaRepository.findById(id)
              .orElseThrow(() -> notFound("Risiko Pemda tidak ditemukan dengan ID: " + id));
    }

    private RisikoPemdaResDTO.RisikoItem toRisikoItem(RisikoPemda risiko, String type) {
        RisikoPemdaResDTO.RisikoItem.RisikoItemBuilder builder = RisikoPemdaResDTO.RisikoItem.builder()
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

    private RisikoPemdaResDTO toResDTO(RisikoPemda risiko) {
        return RisikoPemdaResDTO.builder()
              .scope(SCOPE)
              .id(risiko.getId())
              .kodeRisiko(risiko.getKodeRisiko())
              .tahun(risiko.getTahun())
              .kodeSasaranPemda(risiko.getKodeSasaranPemda())
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

    private RisikoPemda toEntity(RisikoPemdaReqDTO request) {
        RisikoPemda risiko = RisikoPemda.builder()
              .tahun(request.getTahun())
              .kodeSasaranPemda(normalized(request.getKodeSasaranPemda()))
              .revisiDari(normalized(request.getRevisiDari()))
              .build();
        applyMutableFields(risiko, request);
        return risiko;
    }

    private void applyMutableFields(RisikoPemda risiko, RisikoPemdaReqDTO request) {
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

    private String formatKodeRisiko(Long id) {
        return String.format("RSK-PEM-%04d", id);
    }

    private String normalizedType(String type) {
        String value = type == null ? "" : type.trim().toLowerCase(java.util.Locale.ROOT);
        if (!SUPPORTED_TYPES.contains(value)) {
            throw new RiskException(400, "RISK_TYPE_INVALID", "Tipe tab risiko tidak dikenali.");
        }
        return value;
    }

    private String requiredPath(String value, String message) {
        String normalized = normalized(value);
        if (normalized == null || normalized.isEmpty() || normalized.length() > 128) {
            throw new RiskException(400, "RISK_INVALID_INPUT", message);
        }
        return normalized;
    }

    private String normalized(String value) {
        return value == null ? null : value.trim();
    }

    private RiskException notFound(String message) {
        return new RiskException(404, "RISK_NOT_FOUND", message);
    }

    private RisikoPemda ownedEditable(Long id, RiskPrincipal actor, boolean deleting) {
        requireLevel3(actor);
        RisikoPemda risk = findById(id);
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

    private void validateRevision(String revisiDari, RiskPrincipal actor) {
        String kode = normalized(revisiDari);
        if (kode == null) return;
        RisikoPemda parent = risikoPemdaRepository.findByKodeRisiko(kode)
              .orElseThrow(() -> new RiskException(422, "RISK_REVISION_INVALID", "Risiko asal revisi tidak ditemukan."));
        if (!actor.id().equals(parent.getCreatedBy()) || !Objects.equals(actor.kodeOpd(), parent.getKodeOpd())
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
