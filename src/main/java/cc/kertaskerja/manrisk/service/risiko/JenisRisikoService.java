package cc.kertaskerja.manrisk.service.risiko;

import cc.kertaskerja.manrisk.dto.JenisRisikoRequest;
import cc.kertaskerja.manrisk.dto.JenisRisikoResponse;
import cc.kertaskerja.manrisk.entity.JenisRisiko;
import cc.kertaskerja.manrisk.exception.RiskException;
import cc.kertaskerja.manrisk.repository.JenisRisikoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JenisRisikoService {
    private final JenisRisikoRepository repository;

    @Transactional(readOnly = true)
    public List<JenisRisikoResponse> findAll() {
        return repository.findAllByOrderByIdAsc().stream().map(JenisRisikoService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public JenisRisikoResponse findById(Long id) {
        return toResponse(requireById(id));
    }

    @Transactional
    public JenisRisikoResponse create(JenisRisikoRequest request) {
        String nama = normalizedName(request);
        if (repository.existsByNamaIgnoreCase(nama)) {
            throw duplicate();
        }

        JenisRisiko entity = new JenisRisiko();
        entity.setNama(nama);
        return save(entity);
    }

    @Transactional
    public JenisRisikoResponse update(Long id, JenisRisikoRequest request) {
        JenisRisiko entity = requireById(id);
        String nama = normalizedName(request);
        if (repository.existsByNamaIgnoreCaseAndIdNot(nama, id)) {
            throw duplicate();
        }

        entity.setNama(nama);
        return save(entity);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(requireById(id));
    }

    private JenisRisiko requireById(Long id) {
        return repository.findById(id).orElseThrow(() ->
              new RiskException(404, "JENIS_RISIKO_NOT_FOUND", "Jenis risiko tidak ditemukan."));
    }

    private JenisRisikoResponse save(JenisRisiko entity) {
        try {
            return toResponse(repository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            throw new RiskException(409, "JENIS_RISIKO_DUPLICATE", "Jenis risiko sudah ada.");
        }
    }

    private static String normalizedName(JenisRisikoRequest request) {
        String nama = request == null || request.nama() == null ? "" : request.nama().strip();
        if (nama.isEmpty() || nama.length() > 120) {
            throw new RiskException(400, "JENIS_RISIKO_INVALID_INPUT", "Nama jenis risiko wajib diisi dan maksimal 120 karakter.");
        }
        return nama;
    }

    private static RiskException duplicate() {
        return new RiskException(409, "JENIS_RISIKO_DUPLICATE", "Jenis risiko sudah ada.");
    }

    private static JenisRisikoResponse toResponse(JenisRisiko entity) {
        return new JenisRisikoResponse(entity.getId(), entity.getNama());
    }
}
