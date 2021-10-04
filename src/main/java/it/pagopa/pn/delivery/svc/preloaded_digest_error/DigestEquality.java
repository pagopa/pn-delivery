package it.pagopa.pn.delivery.svc.preloaded_digest_error;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Constraint( validatedBy = DigestEqualityValidator.class )
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(DigestEquality.List.class)
@Documented
public @interface DigestEquality {

    String message() default "{it.pagopa.pn.delivery.svc.preloaded_digest_error.DigestEquality.message}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE })
    @Documented
    @interface List {
        DigestEquality[] value();
    }
}
