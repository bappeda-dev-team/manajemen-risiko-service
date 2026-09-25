package cc.kertaskerja.manrisk.service.risiko;

import cc.kertaskerja.manrisk.dto.RisikoOperasional.RisikoOperasionalReqDTO;
import cc.kertaskerja.manrisk.dto.RisikoOperasional.RisikoOperasionalResDTO;
import cc.kertaskerja.manrisk.entity.RisikoOperasional;
import cc.kertaskerja.manrisk.exception.RiskException;
import cc.kertaskerja.manrisk.repository.RisikoOperasionalRepository;
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

    public List<RisikoOperasionalResDTO> getAllRisiko() {
        return repository.findAll().stream().map(this::toResDTO).toList();
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
    public void deleteRisiko(Long id) {
        repository.delete(findById(id));
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
}
