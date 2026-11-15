package cc.kertaskerja.manrisk.service.risiko;

import cc.kertaskerja.manrisk.dto.JenisRisikoRequest;
import cc.kertaskerja.manrisk.dto.JenisRisikoResponse;
import cc.kertaskerja.manrisk.entity.JenisRisiko;
import cc.kertaskerja.manrisk.exception.RiskException;
import cc.kertaskerja.manrisk.repository.JenisRisikoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JenisRisikoServiceTest {
    @Mock
    private JenisRisikoRepository repository;

    @InjectMocks
    private JenisRisikoService service;

    @Test
    void createTrimsNameBeforeSaving() {
        when(repository.saveAndFlush(any(JenisRisiko.class))).thenAnswer(invocation -> {
            JenisRisiko entity = invocation.getArgument(0);
            entity.setId(8L);
            return entity;
        });

        JenisRisikoResponse result = service.create(new JenisRisikoRequest("  Risiko Baru  "));

        assertEquals(8L, result.id());
        assertEquals("Risiko Baru", result.nama());
        verify(repository).existsByNamaIgnoreCase("Risiko Baru");
    }

    @Test
    void createRejectsExistingNameIgnoringCase() {
        when(repository.existsByNamaIgnoreCase("risiko strategis")).thenReturn(true);

        RiskException exception = assertThrows(RiskException.class,
              () -> service.create(new JenisRisikoRequest(" risiko strategis ")));

        assertEquals(409, exception.getStatus());
        assertEquals("JENIS_RISIKO_DUPLICATE", exception.getCode());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void updateChangesNameWithoutChangingId() {
        JenisRisiko entity = new JenisRisiko();
        entity.setId(3L);
        entity.setNama("Nama Lama");
        when(repository.findById(3L)).thenReturn(Optional.of(entity));
        when(repository.saveAndFlush(entity)).thenReturn(entity);

        JenisRisikoResponse result = service.update(3L, new JenisRisikoRequest(" Nama Baru "));

        assertEquals(3L, result.id());
        assertEquals("Nama Baru", result.nama());
        verify(repository).existsByNamaIgnoreCaseAndIdNot("Nama Baru", 3L);
    }

    @Test
    void deleteRejectsUnknownId() {
        RiskException exception = assertThrows(RiskException.class, () -> service.delete(99L));

        assertEquals(404, exception.getStatus());
        assertEquals("JENIS_RISIKO_NOT_FOUND", exception.getCode());
    }
}
