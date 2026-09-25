package cc.kertaskerja.manrisk.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;

public class CommaSeparatedListValidator implements ConstraintValidator<CommaSeparatedList, String> {
    private int maxItems;

    @Override
    public void initialize(CommaSeparatedList constraintAnnotation) {
        maxItems = constraintAnnotation.maxItems();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        String[] items = value.split(",", -1);
        return items.length <= maxItems
              && Arrays.stream(items).allMatch(item -> !item.isBlank());
    }
}
