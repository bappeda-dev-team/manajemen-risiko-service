package cc.kertaskerja.manrisk.service.risiko;

import cc.kertaskerja.manrisk.dto.Risiko.RisikoReqDTO;
import cc.kertaskerja.manrisk.dto.Risiko.RisikoResDTO;
import cc.kertaskerja.manrisk.entity.Risiko;
import cc.kertaskerja.manrisk.exception.RiskException;
import cc.kertaskerja.manrisk.repository.RisikoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RisikoService {

    private final RisikoRepository risikoRepository;

    public List<RisikoResDTO> getAllRisiko() {
        return risikoRepository.findAll()
              .stream()
              .map(this::toResDTO)
              .collect(Collectors.toList());
    }

    public RisikoResDTO getRisikoByKodeSasaranOpd(String kodeSasaranOpd, String type) {
        List<Risiko> risikoList = risikoRepository.findByKodeSasaranOpd(kodeSasaranOpd);

        if (risikoList == null || risikoList.isEmpty()) {
            return RisikoResDTO.builder()
                  .kodeSasaranOpd(kodeSasaranOpd)
                  .risiko(List.of())
                  .build();
        }

        risikoList = new ArrayList<>(risikoList);
        risikoList.sort(Comparator.comparing(Risiko::getId));

        Risiko first = risikoList.get(0);

        return RisikoResDTO.builder()
              .kodeOpd(first.getKodeOpd())
              .kodeRisiko(first.getKodeRisiko())
              .tahun(first.getTahun())
              .kodeSasaranOpd(first.getKodeSasaranOpd())
              .risiko(risikoList.stream()
                    .map(risiko -> toRisikoItem(risiko, type))
                    .collect(Collectors.toList()))
              .build();
    }

    public RisikoResDTO getRisikoByKodeRisiko(String kodeRisiko) {
        Risiko risiko = risikoRepository.findByKodeRisiko(kodeRisiko)
              .orElseThrow(() -> notFound("Risiko tidak ditemukan dengan kode: " + kodeRisiko));

        return toResDTO(risiko);
    }

    @Transactional
    public RisikoResDTO createRisiko(RisikoReqDTO reqDTO) {
        Risiko risiko = toEntity(reqDTO);
        Risiko saved = risikoRepository.saveAndFlush(risiko);
        saved.setKodeRisiko(formatKodeRisiko(saved.getId()));
        return toResDTO(risikoRepository.save(saved));
    }

    public RisikoResDTO updateRisiko(Long id, RisikoReqDTO reqDTO) {
        Risiko existing = risikoRepository.findById(id)
              .orElseThrow(() -> notFound("Risiko tidak ditemukan dengan ID: " + id));

        if (!Objects.equals(normalized(existing.getKodeOpd()), normalized(reqDTO.getKodeOpd()))
              || !Objects.equals(existing.getTahun(), reqDTO.getTahun())
              || !Objects.equals(normalized(existing.getKodeSasaranOpd()), normalized(reqDTO.getKodeSasaranOpd()))) {
            throw new RiskException(409, "RISK_REFERENCE_IMMUTABLE",
                  "Kode OPD, tahun, dan kode sasaran tidak dapat diubah.");
        }

        existing.setPermasalahan(normalized(reqDTO.getPermasalahan()));
        existing.setSebabPermasalahan(normalized(reqDTO.getSebabPermasalahan()));
        existing.setPernyataanRisiko(normalized(reqDTO.getPernyataanRisiko()));
        existing.setSkalaKemungkinan(reqDTO.getSkalaKemungkinan());
        existing.setSkalaDampak(reqDTO.getSkalaDampak());
        existing.setPihakTerkenaRisiko(normalized(reqDTO.getPihakTerkenaRisiko()));
        existing.setRencanaTindakPengendalian(normalized(reqDTO.getRencanaTindakPengendalian()));
        existing.setMetodePemantauan(normalized(reqDTO.getMetodePemantauan()));
        existing.setPenanggungjawabPemantauan(normalized(reqDTO.getPenanggungjawabPemantauan()));
        existing.setKeterangan(normalized(reqDTO.getKeterangan()));
        existing.setRealisasiTindakPengendalian(normalized(reqDTO.getRealisasiTindakPengendalian()));
        existing.setDapatTerkendali(normalized(reqDTO.getDapatTerkendali()));
        existing.setDampak(normalized(reqDTO.getDampak()));
        existing.setCatatan(normalized(reqDTO.getCatatan()));
        existing.setPerangkatYangMenangani(normalized(reqDTO.getPerangkatYangMenangani()));
        existing.setKodePerangkatYangMenangani(normalized(reqDTO.getKodePerangkatYangMenangani()));

        return toResDTO(risikoRepository.save(existing));
    }

    public void deleteRisiko(Long id) {
        Risiko existing = risikoRepository.findById(id)
              .orElseThrow(() -> notFound("Risiko tidak ditemukan dengan ID: " + id));
        risikoRepository.delete(existing);
    }

    private String formatKodeRisiko(Long id) {
        return String.format("RSK-%04d", id);
    }

    private RisikoResDTO.RisikoItem toRisikoItem(Risiko risiko, String type) {
        boolean identifikasi = "identifikasi".equalsIgnoreCase(type);

        RisikoResDTO.RisikoItem.RisikoItemBuilder builder = RisikoResDTO.RisikoItem.builder()
              .id(risiko.getId())
              .kodeRisiko(risiko.getKodeRisiko())
              .type(type)
              .permasalahan(risiko.getPermasalahan())
              .sebabPermasalahan(risiko.getSebabPermasalahan())
              .pernyataanRisiko(risiko.getPernyataanRisiko());

        if (!identifikasi) {
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
                  .createdAt(risiko.getCreatedAt())
                  .updatedAt(risiko.getUpdatedAt());
        }

        return builder.build();
    }

    private RisikoResDTO toResDTO(Risiko risiko) {
        return RisikoResDTO.builder()
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
              .createdAt(risiko.getCreatedAt())
              .updatedAt(risiko.getUpdatedAt())
              .build();
    }

    private Risiko toEntity(RisikoReqDTO reqDTO) {
        return Risiko.builder()
              .kodeOpd(normalized(reqDTO.getKodeOpd()))
              .tahun(reqDTO.getTahun())
              .kodeSasaranOpd(normalized(reqDTO.getKodeSasaranOpd()))
              .permasalahan(normalized(reqDTO.getPermasalahan()))
              .sebabPermasalahan(normalized(reqDTO.getSebabPermasalahan()))
              .pernyataanRisiko(normalized(reqDTO.getPernyataanRisiko()))
              .skalaKemungkinan(reqDTO.getSkalaKemungkinan())
              .skalaDampak(reqDTO.getSkalaDampak())
              .pihakTerkenaRisiko(normalized(reqDTO.getPihakTerkenaRisiko()))
              .rencanaTindakPengendalian(normalized(reqDTO.getRencanaTindakPengendalian()))
              .metodePemantauan(normalized(reqDTO.getMetodePemantauan()))
              .penanggungjawabPemantauan(normalized(reqDTO.getPenanggungjawabPemantauan()))
              .keterangan(normalized(reqDTO.getKeterangan()))
              .realisasiTindakPengendalian(normalized(reqDTO.getRealisasiTindakPengendalian()))
              .dapatTerkendali(normalized(reqDTO.getDapatTerkendali()))
              .dampak(normalized(reqDTO.getDampak()))
              .catatan(normalized(reqDTO.getCatatan()))
              .perangkatYangMenangani(normalized(reqDTO.getPerangkatYangMenangani()))
              .kodePerangkatYangMenangani(normalized(reqDTO.getKodePerangkatYangMenangani()))
              .build();
    }

    private String normalized(String value) {
        return value == null ? null : value.trim();
    }

    private RiskException notFound(String message) {
        return new RiskException(404, "RISK_NOT_FOUND", message);
    }
}
