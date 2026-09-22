package cc.kertaskerja.manrisk.service.risiko;

import cc.kertaskerja.manrisk.dto.Risiko.RisikoReqDTO;
import cc.kertaskerja.manrisk.dto.Risiko.RisikoResDTO;
import cc.kertaskerja.manrisk.entity.Risiko;
import cc.kertaskerja.manrisk.exception.ResourceNotFoundException;
import cc.kertaskerja.manrisk.repository.RisikoRepository;
import cc.kertaskerja.manrisk.service.risiko.external.ExternalService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RisikoService {

    private final RisikoRepository risikoRepository;
    private final ExternalService externalService;

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
              .orElseThrow(() -> new ResourceNotFoundException("Risiko not found with kodeRisiko: " + kodeRisiko));

        return toResDTO(risiko);
    }

    @Transactional
    public RisikoResDTO createRisiko(RisikoReqDTO reqDTO) {
        Risiko risiko = toEntity(reqDTO);

        String kodeOpd = risiko.getKodeOpd();
        Integer tahun = risiko.getTahun();
        String kodeSasaranOpd = risiko.getKodeSasaranOpd();

        SasaranData sasaranData = findSasaranInExternal(kodeOpd, tahun, kodeSasaranOpd);

        if (sasaranData != null) {
            // ID dari database bersifat unik dan tidak kembali ke nomor lama
            // ketika record dihapus. Kode risiko diturunkan dari ID tersebut.
            Risiko saved = risikoRepository.saveAndFlush(risiko);
            saved.setKodeRisiko(formatKodeRisiko(saved.getId()));
            return toResDTO(risikoRepository.save(saved));
        } else {
            throw new ResourceNotFoundException("Kode Sasaran OPD tidak ditemukan: " + kodeSasaranOpd);
        }
    }

    public RisikoResDTO updateRisiko(Long id, RisikoReqDTO reqDTO) {
        Risiko existing = risikoRepository.findById(id)
              .orElseThrow(() -> new ResourceNotFoundException("Risiko not found with id: " + id));

        existing.setKodeOpd(reqDTO.getKodeOpd());
        existing.setTahun(reqDTO.getTahun());
        existing.setKodeSasaranOpd(reqDTO.getKodeSasaranOpd());
        existing.setPermasalahan(reqDTO.getPermasalahan());
        existing.setSebabPermasalahan(reqDTO.getSebabPermasalahan());
        existing.setPernyataanRisiko(reqDTO.getPernyataanRisiko());
        existing.setSkalaKemungkinan(reqDTO.getSkalaKemungkinan());
        existing.setSkalaDampak(reqDTO.getSkalaDampak());
        existing.setPihakTerkenaRisiko(reqDTO.getPihakTerkenaRisiko());
        existing.setRencanaTindakPengendalian(reqDTO.getRencanaTindakPengendalian());
        existing.setMetodePemantauan(reqDTO.getMetodePemantauan());
        existing.setPenanggungjawabPemantauan(reqDTO.getPenanggungjawabPemantauan());
        existing.setKeterangan(reqDTO.getKeterangan());
        existing.setRealisasiTindakPengendalian(reqDTO.getRealisasiTindakPengendalian());
        existing.setDapatTerkendali(reqDTO.getDapatTerkendali());
        existing.setDampak(reqDTO.getDampak());
        existing.setCatatan(reqDTO.getCatatan());
        existing.setPerangkatYangMenangani(reqDTO.getPerangkatYangMenangani());
        existing.setKodePerangkatYangMenangani(reqDTO.getKodePerangkatYangMenangani());

        return toResDTO(risikoRepository.save(existing));
    }

    public void deleteRisiko(Long id) {
        Risiko existing = risikoRepository.findById(id)
              .orElseThrow(() -> new ResourceNotFoundException("Risiko not found with id: " + id));
        risikoRepository.delete(existing);
    }

    private String formatKodeRisiko(Long id) {
        return String.format("RSK-%04d", id);
    }

    private SasaranData fetchSasaranData(List<Risiko> risikoList, String kodeSasaranOpd) {
        Set<String> tried = new HashSet<>();
        for (Risiko risiko : risikoList) {
            String kodeOpd = risiko.getKodeOpd();
            if (kodeOpd == null || kodeOpd.isBlank()) continue;

            String key = kodeOpd + "|" + risiko.getTahun();
            if (!tried.add(key)) continue;

            SasaranData data = findSasaranInExternal(kodeOpd, risiko.getTahun(), kodeSasaranOpd);
            if (data != null) {
                return data;
            }
        }
        return new SasaranData(null, null, null);
    }

    private SasaranData findSasaranInExternal(String kodeOpd, Integer tahun, String kodeSasaranOpd) {
        try {
            JsonNode root = externalService.getTujuanSasaran(kodeOpd, tahun);
            if (root == null || !root.hasNonNull("data")) return null;

            JsonNode tujuanOpds = root.path("data").path("tujuan_opds");
            if (!tujuanOpds.isArray()) return null;

            for (JsonNode tujuan : tujuanOpds) {
                JsonNode sasaranOpds = tujuan.path("sasaran_opds");
                if (!sasaranOpds.isArray()) continue;

                for (JsonNode sasaran : sasaranOpds) {
                    if (kodeSasaranOpd.equals(sasaran.path("kode_sasaran_opd").asText(null))) {
                        return new SasaranData(
                              sasaran.path("sasaran_opd").asText(null),
                              sasaran.path("periode").asText(null),
                              parseIndikators(sasaran.path("indikators"))
                        );
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private List<RisikoResDTO.Indikator> parseIndikators(JsonNode indikatorsNode) {
        if (indikatorsNode == null || !indikatorsNode.isArray()) {
            return null;
        }

        List<RisikoResDTO.Indikator> indikators = new ArrayList<>();
        for (JsonNode node : indikatorsNode) {
            indikators.add(RisikoResDTO.Indikator.builder()
                  .id(node.path("id").isNumber() ? node.path("id").asLong() : null)
                  .kodeIndikator(node.path("kode_indikator").asText(null))
                  .indikator(node.path("indikator").asText(null))
                  .rumusPerhitungan(node.path("rumus_perhitungan").asText(null))
                  .sumberData(node.path("sumber_data").asText(null))
                  .definisiOperasional(node.path("definisi_operasional").asText(null))
                  .tahunAktif(node.path("tahun_aktif").isNumber() ? node.path("tahun_aktif").asInt() : null)
                  .targets(parseTargets(node.path("targets")))
                  .build());
        }
        return indikators;
    }

    private List<RisikoResDTO.Target> parseTargets(JsonNode targetsNode) {
        if (targetsNode == null || !targetsNode.isArray()) {
            return null;
        }

        List<RisikoResDTO.Target> targets = new ArrayList<>();
        for (JsonNode node : targetsNode) {
            targets.add(RisikoResDTO.Target.builder()
                  .id(node.path("id").isNumber() ? node.path("id").asLong() : null)
                  .kodeTarget(node.path("kode_target").asText(null))
                  .tahun(node.path("tahun").isNumber() ? node.path("tahun").asInt() : null)
                  .target(node.path("target").isNumber() ? node.path("target").numberValue() : null)
                  .satuan(node.path("satuan").asText(null))
                  .build());
        }
        return targets;
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

    private record SasaranData(String sasaranOpd, String periode, List<RisikoResDTO.Indikator> indikators) {}

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
              .kodeOpd(reqDTO.getKodeOpd())
              .tahun(reqDTO.getTahun())
              .kodeSasaranOpd(reqDTO.getKodeSasaranOpd())
              .permasalahan(reqDTO.getPermasalahan())
              .sebabPermasalahan(reqDTO.getSebabPermasalahan())
              .pernyataanRisiko(reqDTO.getPernyataanRisiko())
              .skalaKemungkinan(reqDTO.getSkalaKemungkinan())
              .skalaDampak(reqDTO.getSkalaDampak())
              .pihakTerkenaRisiko(reqDTO.getPihakTerkenaRisiko())
              .rencanaTindakPengendalian(reqDTO.getRencanaTindakPengendalian())
              .metodePemantauan(reqDTO.getMetodePemantauan())
              .penanggungjawabPemantauan(reqDTO.getPenanggungjawabPemantauan())
              .keterangan(reqDTO.getKeterangan())
              .realisasiTindakPengendalian(reqDTO.getRealisasiTindakPengendalian())
              .dapatTerkendali(reqDTO.getDapatTerkendali())
              .dampak(reqDTO.getDampak())
              .catatan(reqDTO.getCatatan())
              .perangkatYangMenangani(reqDTO.getPerangkatYangMenangani())
              .kodePerangkatYangMenangani(reqDTO.getKodePerangkatYangMenangani())
              .build();
    }
}
