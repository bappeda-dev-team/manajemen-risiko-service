package cc.kertaskerja.manrisk.service.risiko;

import cc.kertaskerja.manrisk.dto.Risiko.RisikoReqDTO;
import cc.kertaskerja.manrisk.dto.Risiko.RisikoResDTO;
import cc.kertaskerja.manrisk.entity.Risiko;
import cc.kertaskerja.manrisk.exception.BadRequestException;
import cc.kertaskerja.manrisk.exception.ResourceNotFoundException;
import cc.kertaskerja.manrisk.repository.RisikoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
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

    public List<RisikoResDTO> getRisikoByKodeSasaranOpd(String kodeSasaranOpd) {
        return risikoRepository.findByKodeSasaranOpd(kodeSasaranOpd)
              .stream()
              .map(this::toResDTO)
              .collect(Collectors.toList());
    }

    public RisikoResDTO getRisikoById(Long id) {
        Risiko risiko = risikoRepository.findById(id)
              .orElseThrow(() -> new ResourceNotFoundException("Risiko not found with id: " + id));

        return toResDTO(risiko);
    }

    public RisikoResDTO createRisiko(RisikoReqDTO reqDTO) {
        risikoRepository.findByKodeRisiko(reqDTO.getKodeRisiko()).ifPresent(r -> {
            throw new BadRequestException("Risiko with kode_risiko '" + reqDTO.getKodeRisiko() + "' already exists");
        });

        Risiko risiko = toEntity(reqDTO);
        Risiko saved = risikoRepository.save(risiko);

        return toResDTO(saved);
    }

    public RisikoResDTO updateRisiko(Long id, RisikoReqDTO reqDTO) {
        Risiko existing = risikoRepository.findById(id)
              .orElseThrow(() -> new ResourceNotFoundException("Risiko not found with id: " + id));

        existing.setKodeOpd(reqDTO.getKodeOpd());
        existing.setKodeRisiko(reqDTO.getKodeRisiko());
        existing.setTahun(reqDTO.getTahun());
        existing.setKodeSasaranOpd(reqDTO.getKodeSasaranOpd());
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
        existing.setDampat(reqDTO.getDampat());
        existing.setCatatan(reqDTO.getCatatan());

        Risiko saved = risikoRepository.save(existing);

        return toResDTO(saved);
    }

    public void deleteRisiko(Long id) {
        Risiko existing = risikoRepository.findById(id)
              .orElseThrow(() -> new ResourceNotFoundException("Risiko not found with id: " + id));

        risikoRepository.delete(existing);
    }

    private RisikoResDTO toResDTO(Risiko risiko) {
        return RisikoResDTO.builder()
              .id(risiko.getId())
              .kodeOpd(risiko.getKodeOpd())
              .kodeRisiko(risiko.getKodeRisiko())
              .tahun(risiko.getTahun())
              .kodeSasaranOpd(risiko.getKodeSasaranOpd())
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
              .dampak(risiko.getDampat())
              .catatan(risiko.getCatatan())
              .createdAt(risiko.getCreatedAt())
              .updatedAt(risiko.getUpdatedAt())
              .build();
    }

    private Risiko toEntity(RisikoReqDTO reqDTO) {
        return Risiko.builder()
              .kodeOpd(reqDTO.getKodeOpd())
              .kodeRisiko(reqDTO.getKodeRisiko())
              .tahun(reqDTO.getTahun())
              .kodeSasaranOpd(reqDTO.getKodeSasaranOpd())
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
              .dampak(reqDTO.getDampat())
              .catatan(reqDTO.getCatatan())
              .build();
    }
}
