package cc.kertaskerja.manrisk.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = CommaSeparatedListValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CommaSeparatedList {
    String message() default "maksimal berisi {maxItems} nilai yang dipisahkan koma";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int maxItems();
}
