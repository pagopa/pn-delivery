package it.pagopa.pn.delivery.svc.validation.validators.formal;

import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.internal.notification.MetadataAttachment;
import it.pagopa.pn.delivery.models.internal.notification.NotificationDocument;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import it.pagopa.pn.delivery.svc.validation.context.NotificationContext;
import it.pagopa.pn.delivery.svc.validation.validators.FormalValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class UniqueAttachmentsValidator implements FormalValidator<NotificationContext> {


    @Override
    public ValidationResult validate(NotificationContext context) {

        ArrayList<ProblemError> errors = new ArrayList<>();
        checkIfNotificationHasDuplicateAttachments(context, errors);
        return new ValidationResult(errors);
    }

    private void checkIfNotificationHasDuplicateAttachments(NotificationContext context, ArrayList<ProblemError> errors) {
        if (!hasDistinctAttachments(context.getPayload())) {
            errors.add(ProblemError.builder().detail("Same attachment compares more then once in the same request").code(ErrorCodes.ERROR_CODE_DUPLICATED_ATTACHMENTS.getValue()).element("attachments").build());
        }
    }

    /**
     * Validazio di NewNotificationRequestV25 per verificare l'assenza di duplicati tra gli allegati
     * @param newNotificationRequest
     * @return
     */
    protected boolean hasDistinctAttachments(InternalNotification newNotificationRequest){
        Set<String> uniqueIds = new HashSet<>();

        for (NotificationDocument doc : emptyIfNull(newNotificationRequest.getDocuments())) {
            if (doc.getRef() != null && doc.getDigests() != null) {
                String id = doc.getRef().getKey() + doc.getDigests().getSha256();
                if (!uniqueIds.add(id)) {
                    return false;
                }
            }
        }

        long duplicates = emptyIfNull(newNotificationRequest.getRecipients())
                .stream()
                .map(recipient -> hasRecipientDistinctAttachments(recipient, uniqueIds))
                .filter(res -> !res).count();

        return duplicates==0;
    }

    private boolean hasRecipientDistinctAttachments(NotificationRecipient recipient, Set<String> docIds){
        Set<String> recipientAttachmentIds = new HashSet<>();
        recipientAttachmentIds.addAll(docIds);

        long duplicatedAttachments = emptyIfNull(recipient.getPayments()).stream()
                .filter( payment -> payment.getPagoPa() != null && payment.getPagoPa().getAttachment() != null)
                .map(payment ->{
                    MetadataAttachment att = payment.getPagoPa().getAttachment();
                    if (att.getRef() != null && att.getDigests() != null) {
                        String id = att.getRef().getKey() + att.getDigests().getSha256();

                        if (!recipientAttachmentIds.add(id)) {
                            return false;
                        }
                    }
                    return true;
                }).filter( uniqueAttachment -> !uniqueAttachment)
                .count();

        return duplicatedAttachments == 0;
    }

    public static <T> List<T> emptyIfNull(List<T> list) {
        return list == null ? Collections.<T>emptyList() : list;
    }
}
