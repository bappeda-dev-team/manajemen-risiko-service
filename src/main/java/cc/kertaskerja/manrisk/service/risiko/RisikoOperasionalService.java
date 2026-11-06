package cc.kertaskerja.manrisk.service.risiko;

import cc.kertaskerja.manrisk.dto.RisikoOperasional.RisikoOperasionalReqDTO;
import cc.kertaskerja.manrisk.dto.RisikoOperasional.RisikoOperasionalResDTO;
import cc.kertaskerja.manrisk.entity.RisikoOperasional;
import cc.kertaskerja.manrisk.exception.RiskException;
import cc.kertaskerja.manrisk.repository.RisikoOperasionalRepository;
import cc.kertaskerja.manrisk.repository.VerifikasiRisikoRepository;
import cc.kertaskerja.manrisk.security.RiskPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RisikoOperasionalService {
    private static final String SCOPE = "operasional";
    private static final Set<String> SUPPORTED_TYPES = Set.of(
          "identifikasi", "analisis", "pengendalian", "pemantauan", "hasil-pemantauan");

    private final RisikoOperasionalRepository repository;
    private final VerifikasiRisikoRepository verifikasiRisikoRepository;

    public List<RisikoOperasionalResDTO> getAllRisiko() {
        return repository.findAll().stream().map(this::toResDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<RisikoOperasionalResDTO> getRisikoLaporan(int tahun, String kodeOpd) {
        return repository.findVerifiedForLaporan(RisikoLaporanFilter.opd(kodeOpd), RisikoLaporanFilter.year(tahun))
              .stream().map(this::toResDTO).toList();
    }

    public RisikoOperasionalResDTO getRisikoByKodeRekin(String kodeRekin, String type) {
        String normalizedType = normalizedType(type);
        String normalizedKodeRekin = requiredCode(kodeRekin, "Kode rencana kinerja tidak valid.");
        List<RisikoOperasional> records = repository.findByKodeRekinOrderByIdAsc(normalizedKodeRekin);
        if (records.isEmpty()) {
            return RisikoOperasionalResDTO.builder()
                  .scope(SCOPE)
                  .kodeRekin(normalizedKodeRekin)
                  .risiko(List.of())
                  .build();
        }

        RisikoOperasional first = records.getFirst();
        return RisikoOperasionalResDTO.builder()
              .scope(SCOPE)
              .tahun(first.getTahun())
              .kodeRekin(first.getKodeRekin())
              .kodeOpd(first.getKodeOpd())
              .pegawaiId(first.getPegawaiId())
              .risiko(records.stream().map(record -> toRisikoItem(record, normalizedType)).toList())
              .build();
    }

    public RisikoOperasionalResDTO getRisikoByKodeRisiko(String kodeRisiko) {
        String normalizedKodeRisiko = requiredCode(kodeRisiko, "Kode risiko tidak valid.");
        return repository.findByKodeRisiko(normalizedKodeRisiko)
              .map(this::toResDTO)
              .orElseThrow(() -> notFound("Risiko Operasional tidak ditemukan dengan kode: " + normalizedKodeRisiko));
    }

    @Transactional
    public RisikoOperasionalResDTO createRisiko(RisikoOperasionalReqDTO request) {
        RisikoOperasional saved = repository.saveAndFlush(toEntity(request));
        saved.setKodeRisiko(formatKodeRisiko(saved.getId()));
        return toResDTO(repository.save(saved));
    }

    @Transactional
    public RisikoOperasionalResDTO createRisiko(RisikoOperasionalReqDTO request, RiskPrincipal actor) {
        requireLevel3(actor);
        if (!Objects.equals(normalized(request.getKodeOpd()), actor.kodeOpd())) {
            throw new RiskException(403, "RISK_ACCESS_DENIED", "Kode OPD harus sesuai OPD pemohon.");
        }
        validateRevision(request.getRevisiDari(), actor, normalized(request.getKodeOpd()));
        RisikoOperasional saved = toEntity(request);
        saved.setCreatedBy(actor.id());
        saved.setCreatedName(actor.nama());
        saved.setCreatedJabatan(actor.jabatan());
        saved = repository.saveAndFlush(saved);
        saved.setKodeRisiko(formatKodeRisiko(saved.getId()));
        return toResDTO(repository.save(saved));
    }

    @Transactional
    public RisikoOperasionalResDTO updateRisiko(Long id, RisikoOperasionalReqDTO request) {
        RisikoOperasional existing = findById(id);
        if (!Objects.equals(existing.getTahun(), request.getTahun())
              || !Objects.equals(normalized(existing.getKodeRekin()), normalized(request.getKodeRekin()))
              || !Objects.equals(normalized(existing.getKodeOpd()), normalized(request.getKodeOpd()))
              || !Objects.equals(normalized(existing.getPegawaiId()), normalized(request.getPegawaiId()))) {
            throw new RiskException(409, "RISK_REFERENCE_IMMUTABLE",
                  "Tahun, kode rencana kinerja, kode OPD, dan pegawai tidak dapat diubah.");
        }
        applyMutableFields(existing, request);
        return toResDTO(repository.save(existing));
    }

    @Transactional
    public RisikoOperasionalResDTO updateRisiko(Long id, RisikoOperasionalReqDTO request, RiskPrincipal actor) {
        RisikoOperasional existing = ownedEditable(id, actor, false);
        if (!Objects.equals(existing.getTahun(), request.getTahun())
              || !Objects.equals(normalized(existing.getKodeRekin()), normalized(request.getKodeRekin()))
              || !Objects.equals(normalized(existing.getKodeOpd()), normalized(request.getKodeOpd()))
              || !Objects.equals(normalized(existing.getPegawaiId()), normalized(request.getPegawaiId()))) {
            throw new RiskException(409, "RISK_REFERENCE_IMMUTABLE", "Tahun, kode rencana kinerja, kode OPD, dan pegawai tidak dapat diubah.");
        }
        applyMutableFields(existing, request);
        return toResDTO(repository.save(existing));
    }

    @Transactional
    public void deleteRisiko(Long id) {
        repository.delete(findById(id));
    }

    @Transactional
    public void deleteRisiko(Long id, RiskPrincipal actor) {
        RisikoOperasional existing = ownedEditable(id, actor, true);
        existing.setDeletedAt(java.time.OffsetDateTime.now());
        repository.save(existing);
    }

    private RisikoOperasional findById(Long id) {
        if (id == null || id < 1) {
            throw new RiskException(400, "RISK_INVALID_INPUT", "ID risiko tidak valid.");
        }
        return repository.findById(id)
              .orElseThrow(() -> notFound("Risiko Operasional tidak ditemukan dengan ID: " + id));
    }

    private RisikoOperasionalResDTO.RisikoItem toRisikoItem(RisikoOperasional risiko, String type) {
        RisikoOperasionalResDTO.RisikoItem.RisikoItemBuilder builder = RisikoOperasionalResDTO.RisikoItem.builder()
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

    private RisikoOperasionalResDTO toResDTO(RisikoOperasional risiko) {
        return RisikoOperasionalResDTO.builder()
              .scope(SCOPE)
              .id(risiko.getId())
              .kodeRisiko(risiko.getKodeRisiko())
              .tahun(risiko.getTahun())
              .kodeRekin(risiko.getKodeRekin())
              .kodeOpd(risiko.getKodeOpd())
              .pegawaiId(risiko.getPegawaiId())
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

    private RisikoOperasional toEntity(RisikoOperasionalReqDTO request) {
        RisikoOperasional risiko = RisikoOperasional.builder()
              .tahun(request.getTahun())
              .kodeRekin(normalized(request.getKodeRekin()))
              .kodeOpd(normalized(request.getKodeOpd()))
              .pegawaiId(normalized(request.getPegawaiId()))
              .revisiDari(normalized(request.getRevisiDari()))
              .build();
        applyMutableFields(risiko, request);
        return risiko;
    }

    private void applyMutableFields(RisikoOperasional risiko, RisikoOperasionalReqDTO request) {
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

    private String requiredCode(String value, String message) {
        String normalized = normalized(value);
        if (normalized == null || normalized.isEmpty() || normalized.length() > 128) {
            throw new RiskException(400, "RISK_INVALID_INPUT", message);
        }
        return normalized;
    }

    private String formatKodeRisiko(Long id) {
        return String.format("RSK-OPR-%04d", id);
    }

    private String normalized(String value) {
        return value == null ? null : value.trim();
    }

    private RiskException notFound(String message) {
        return new RiskException(404, "RISK_NOT_FOUND", message);
    }

    private RisikoOperasional ownedEditable(Long id, RiskPrincipal actor, boolean deleting) {
        requireLevel3(actor);
        RisikoOperasional risk = findById(id);
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
        RisikoOperasional parent = repository.findByKodeRisiko(kode)
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
