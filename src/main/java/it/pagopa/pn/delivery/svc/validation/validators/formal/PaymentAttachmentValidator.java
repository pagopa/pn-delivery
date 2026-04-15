package it.pagopa.pn.delivery.svc.validation.validators.formal;

import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewNotificationRequestV25;
import it.pagopa.pn.delivery.models.internal.notification.MetadataAttachment;
import it.pagopa.pn.delivery.models.internal.notification.NotificationPaymentInfo;
import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import it.pagopa.pn.delivery.svc.validation.context.NotificaContext;
import it.pagopa.pn.delivery.svc.validation.validators.FormalValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.validation.ConstraintViolation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class PaymentAttachmentValidator implements FormalValidator<NotificaContext> {

    private static final String APPLICATION_PDF_CONTENT_TYPE = "application/pdf";
    private static final String APPLICATION_JSON_CONTENT_TYPE = "application/json";
    public static final String EXTENSION_PDF = ".pdf";
    public static final String EXTENSION_JSON = ".json";
    public static final String PN_NOTIFICATION_ATTACHMENTS = "PN_NOTIFICATION_ATTACHMENTS";
    public static final String PN_F24_META = "PN_F24_META";

    @Override
    public ValidationResult validate(NotificaContext context) {

        ArrayList<ProblemError> errors = new ArrayList<>();

        context.getPayload().getRecipients().forEach(notificationRecipient ->
                checkCongruenceBetweenContentTypeAndFileKey(notificationRecipient.getPayments(), errors));

        return new ValidationResult(errors);
    }

    private void checkCongruenceBetweenContentTypeAndFileKey(List<NotificationPaymentInfo> payments, ArrayList<ProblemError> errors) {
        Set<ConstraintViolation<NewNotificationRequestV25>> violations = new HashSet<>();
        for (NotificationPaymentInfo paymentItem : payments) {
            // Verifica per pagamenti F24
            if (paymentItem.getF24() != null) {
                MetadataAttachment metadataAttachment = paymentItem.getF24().getMetadataAttachment();
                checkContentType(metadataAttachment.getContentType(), metadataAttachment.getRef().getKey(), errors);
            }
            // Verifica per pagamenti PagoPA
            else if (paymentItem.getPagoPa() != null && paymentItem.getPagoPa().getAttachment() != null) {
                MetadataAttachment attachment = paymentItem.getPagoPa().getAttachment();
                checkContentType(attachment.getContentType(), attachment.getRef().getKey(), errors);
            }
        }
    }

    private void checkContentType(String contentType, String key, ArrayList<ProblemError> errors) {
        if (APPLICATION_PDF_CONTENT_TYPE.equalsIgnoreCase(contentType) && (!key.contains(PN_NOTIFICATION_ATTACHMENTS) || key.endsWith(EXTENSION_JSON) )) {
            errors.add(ProblemError.builder().detail(String.format("Key: %s does not conform to the expected content type: %s", key, contentType)).element("payment").code(ErrorCodes.ERROR_CODE_PAYMENT_ATTACHMENT_CONTENT_TYPE.getValue()).build());
        }
        if (APPLICATION_JSON_CONTENT_TYPE.equalsIgnoreCase(contentType) && (!key.contains(PN_F24_META) || key.endsWith(EXTENSION_PDF))) {
            errors.add(ProblemError.builder().detail(String.format("Key: %s does not conform to the expected content type: %s", key, contentType)).element("payment").code(ErrorCodes.ERROR_CODE_PAYMENT_ATTACHMENT_CONTENT_TYPE.getValue()).build());
        }
    }
}
