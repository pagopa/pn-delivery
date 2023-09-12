package it.pagopa.pn.delivery.svc.preloaded_digest_error;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationAttachmentDigests;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.hibernate.validator.messageinterpolation.ExpressionLanguageFeatureLevel;
import org.junit.jupiter.api.Test;

import javax.validation.ClockProvider;
import javax.validation.ConstraintValidatorContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class DigestEqualityValidatorTest {


    /**
     * Method under test: {@link DigestEqualityValidator#isValid(DigestEqualityBean, ConstraintValidatorContext)}
     */
    @Test
    void testIsValid2() {
        DigestEqualityValidator digestEqualityValidator = new DigestEqualityValidator();
        NotificationAttachmentDigests expected = new NotificationAttachmentDigests("Sha256");
        DigestEqualityBean bean = new DigestEqualityBean("Key", expected, new NotificationAttachmentDigests("Sha256"));

        ClockProvider clockProvider = mock(ClockProvider.class);
        assertTrue(digestEqualityValidator.isValid(bean,
                new ConstraintValidatorContextImpl(clockProvider, PathImpl.createRootPath(), null,
                        "Constraint Validator Payload", ExpressionLanguageFeatureLevel.DEFAULT,
                        ExpressionLanguageFeatureLevel.DEFAULT)));
    }

    /**
     * Method under test: {@link DigestEqualityValidator#isValid(DigestEqualityBean, ConstraintValidatorContext)}
     */
    @Test
    void testIsValid8() {
        DigestEqualityValidator digestEqualityValidator = new DigestEqualityValidator();
        NotificationAttachmentDigests expected = new NotificationAttachmentDigests(
                "it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationAttachmentDigests");
        DigestEqualityBean bean = new DigestEqualityBean("Key", expected, new NotificationAttachmentDigests("Sha256"));

        PathImpl propertyPath = PathImpl.createRootPath();
        propertyPath.addPropertyNode("Node Name");
        ConstraintValidatorContextImpl constraintValidatorContext = new ConstraintValidatorContextImpl(
                mock(ClockProvider.class), propertyPath, null, "Constraint Validator Payload",
                ExpressionLanguageFeatureLevel.DEFAULT, ExpressionLanguageFeatureLevel.DEFAULT);

        assertFalse(digestEqualityValidator.isValid(bean, constraintValidatorContext));
        assertEquals(1, constraintValidatorContext.getConstraintViolationCreationContexts().size());
    }

}

