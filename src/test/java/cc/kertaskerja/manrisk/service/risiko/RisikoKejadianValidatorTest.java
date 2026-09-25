package cc.kertaskerja.manrisk.service.risiko;

import cc.kertaskerja.manrisk.exception.RiskException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RisikoKejadianValidatorTest {
    @Test
    void acceptsTheThreeValidOccurrenceCombinations() {
        assertDoesNotThrow(() -> RisikoKejadianValidator.validate(true, LocalDate.now()));
        assertDoesNotThrow(() -> RisikoKejadianValidator.validate(false, null));
        assertDoesNotThrow(() -> RisikoKejadianValidator.validate(null, null));
    }

    @Test
    void rejectsInconsistentOrFutureOccurrenceValues() {
        assertInvalid(true, null);
        assertInvalid(false, LocalDate.now());
        assertInvalid(null, LocalDate.now());
        assertInvalid(true, LocalDate.now().plusDays(1));
    }

    private void assertInvalid(Boolean risikoTerjadi, LocalDate waktuTerjadi) {
        RiskException error = assertThrows(RiskException.class,
              () -> RisikoKejadianValidator.validate(risikoTerjadi, waktuTerjadi));
        assertEquals("RISK_OCCURRENCE_INVALID", error.getCode());
    }
}
