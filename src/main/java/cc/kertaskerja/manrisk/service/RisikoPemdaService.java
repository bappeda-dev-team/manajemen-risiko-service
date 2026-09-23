package cc.kertaskerja.manrisk.service;

import cc.kertaskerja.manrisk.dto.RisikoOpd.RisikoOpdReqDTO;
import cc.kertaskerja.manrisk.dto.RisikoOpd.RisikoOpdResDTO;
import cc.kertaskerja.manrisk.dto.RisikoPemda.RisikoPemdaResDTO;
import cc.kertaskerja.manrisk.entity.RisikoOpd;
import cc.kertaskerja.manrisk.entity.RisikoPemda;
import cc.kertaskerja.manrisk.repository.RisikoPemdaRepository;
import cc.kertaskerja.manrisk.service.external.ExternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RisikoPemdaService {

    private final RisikoPemdaRepository risikoRepository;
    private final ExternalService externalService;

    public List<RisikoPemdaResDTO> getAllRisiko() {
        return risikoRepository.findAll()
              .stream()
              .map(this::toResDTO)
              .collect(Collectors.toList());
    }

    private RisikoPemdaResDTO toResDTO(RisikoPemda risiko) {
        return RisikoPemdaResDTO.builder()
              .id(risiko.getId())
              .kodeRisiko(risiko.getKodeRisiko())
              .tahun(risiko.getTahun())
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

    private RisikoOpd toEntity(RisikoOpdReqDTO reqDTO) {
        return RisikoOpd.builder()
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
