package cc.kertaskerja.manrisk.common.validation;

import cc.kertaskerja.manrisk.dto.Risiko.RisikoReqDTO;
import cc.kertaskerja.manrisk.dto.RisikoOperasional.RisikoOperasionalReqDTO;
import cc.kertaskerja.manrisk.dto.RisikoPemda.RisikoPemdaReqDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiOpdRequestValidationTest {
    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void acceptsOneThroughFiveOpdCodesForEveryRiskRequest() {
        for (int count = 1; count <= 5; count++) {
            for (Object request : requestsWithCodes(codes(count))) {
                assertTrue(violations(request).isEmpty());
            }
        }
    }

    @Test
    void rejectsMoreThanFiveOpdCodesForEveryRiskRequest() {
        for (Object request : requestsWithCodes(codes(6))) {
            assertFalse(violations(request).isEmpty());
        }
    }

    @Test
    void rejectsAnEmptyItemInsideTheOpdCodeList() {
        for (Object request : requestsWithCodes("OPD-1, , OPD-2")) {
            assertFalse(violations(request).isEmpty());
        }
    }

    private static List<Object> requestsWithCodes(String codes) {
        return List.of(
              RisikoReqDTO.builder().kodePerangkatYangMenangani(codes).build(),
              RisikoPemdaReqDTO.builder().kodePerangkatYangMenangani(codes).build(),
              RisikoOperasionalReqDTO.builder().kodePerangkatYangMenangani(codes).build()
        );
    }

    private static String codes(int count) {
        return IntStream.rangeClosed(1, count)
              .mapToObj(index -> "OPD-" + index)
              .collect(Collectors.joining(", "));
    }

    private static java.util.Set<jakarta.validation.ConstraintViolation<Object>> violations(Object request) {
        return validator.validateProperty(request, "kodePerangkatYangMenangani");
    }
}
