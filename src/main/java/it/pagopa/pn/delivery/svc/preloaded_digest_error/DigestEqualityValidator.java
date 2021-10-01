package it.pagopa.pn.delivery.svc.preloaded_digest_error;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class DigestEqualityValidator implements ConstraintValidator<DigestEquality, DigestEqualityBean> {

    @Override
    public boolean isValid(DigestEqualityBean bean, ConstraintValidatorContext constraintValidatorContext) {
        String expectedSha256 = bean.getExpected().getSha256();
        String actualSha256 = bean.getActual().getSha256();

        boolean isValid = expectedSha256.equalsIgnoreCase( actualSha256 );

        if ( !isValid ) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext
                    .buildConstraintViolationWithTemplate( "Attachment " + bean.getKey() + " has sha256 " + actualSha256 + " instead of " + expectedSha256 )
                    .addPropertyNode( bean.getKey() )
                    .addConstraintViolation();
        }
        return isValid;
    }
}
