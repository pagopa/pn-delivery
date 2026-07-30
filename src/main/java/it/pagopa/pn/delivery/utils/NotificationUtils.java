package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.models.InformalNotificationDetail;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.LegalNotificationDetail;
import it.pagopa.pn.delivery.models.internal.notification.F24Payment;
import it.pagopa.pn.delivery.models.internal.notification.NotificationPaymentInfo;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationUtils {

    public static NotificationStatusV26 getNotificationLastStatus(String notificationRequestId,
                                                                  LegalNotificationDetail legalNotificationDetail) {
        NotificationStatusV26 lastStatus;
        if ( !CollectionUtils.isEmpty( legalNotificationDetail.getNotificationStatusHistory() )) {
            lastStatus = legalNotificationDetail.getNotificationStatusHistory().get(
                    legalNotificationDetail.getNotificationStatusHistory().size() - 1 ).getStatus();
        } else {
            log.debug( "No status history for notificationRequestId={}", notificationRequestId);
            lastStatus = NotificationStatusV26.IN_VALIDATION;
        }
        return lastStatus;
    }

    public static InformalNotificationStatusV1 getInformalNotificationLastStatus(String notificationRequestId,
                                                                                 InformalNotificationDetail informalNotificationDetail) {
        InformalNotificationStatusV1 lastStatus;
        if ( !CollectionUtils.isEmpty( informalNotificationDetail.getNotificationStatusHistory() )) {
            lastStatus = informalNotificationDetail.getNotificationStatusHistory().get(
                    informalNotificationDetail.getNotificationStatusHistory().size() - 1 ).getStatus();
        } else {
            log.debug( "No status history for notificationRequestId={}", notificationRequestId);
            lastStatus = InformalNotificationStatusV1.IN_VALIDATION;
        }
        return lastStatus;
    }

    public static List<NotificationRequestRefusedProblemError> getLegalNotificationRefusedErrors(TimelineElementV28 timelineElement) {
        List<NotificationRefusedErrorV27> refusalReasons = timelineElement.getDetails().getRefusalReasons();
        return refusalReasons.stream().map(
                reason -> NotificationRequestRefusedProblemError.builder()
                        .code( reason.getErrorCode() )
                        .detail( reason.getDetail() )
                        .recIndex( reason.getRecIndex() )
                        .build()
        ).toList();
    }

    public static List<NotificationRequestRefusedProblemError> getInformalNotificationRefusedErrors(InformalTimelineElementV1 timelineElement) {
        List<NotificationRefusedErrorV27> refusalReasons = timelineElement.getDetails().getRefusalReasons();
        return refusalReasons.stream().map(
                reason -> NotificationRequestRefusedProblemError.builder()
                        .code( reason.getErrorCode() )
                        .detail( reason.getDetail() )
                        .recIndex( reason.getRecIndex() )
                        .build()
        ).toList();
    }

    public static boolean isNotificationCancelled(LegalNotificationDetail legalNotificationDetail) {
        var cancellationRequestCategory = TimelineElementCategoryV28.NOTIFICATION_CANCELLATION_REQUEST;
        if (CollectionUtils.isEmpty(legalNotificationDetail.getTimeline())) {
            return false;
        }
        Optional<TimelineElementV28> cancellationRequestTimeline = legalNotificationDetail.getTimeline().stream()
                .filter(timelineElement -> cancellationRequestCategory.equals(timelineElement.getCategory()))
                .findFirst();
        boolean cancellationTimelineIsPresent = cancellationRequestTimeline.isPresent();
        if (cancellationTimelineIsPresent) {
            log.warn("Notification with iun: {} has a request for cancellation", legalNotificationDetail.getNotification().getIun());
        }
        return cancellationTimelineIsPresent;
    }

    public static void removeDocuments(InternalNotification notification) {
        notification.setDocumentsAvailable(false);
        notification.setDocuments(Collections.emptyList());
        if (CollectionUtils.isEmpty(notification.getRecipients())) {
            return;
        }
        for (NotificationRecipient recipient : notification.getRecipients()) {
            List<NotificationPaymentInfo> payments = recipient.getPayments();
            if (!CollectionUtils.isEmpty(payments)) {
                payments.forEach(NotificationUtils::removePaymentAttachment);
            }
        }
    }

    private static void removePaymentAttachment(NotificationPaymentInfo notificationPaymentInfo) {
        it.pagopa.pn.delivery.models.internal.notification.PagoPaPayment pagoPaPayment = notificationPaymentInfo.getPagoPa();
        if (Objects.nonNull(pagoPaPayment)) {
            // rimuovo allegato di pagamento pagoPA
            pagoPaPayment.setAttachment(null);
        }
        F24Payment f24Payment = notificationPaymentInfo.getF24();
        if (Objects.nonNull(f24Payment)) {
            // rimuovo oggetto di pagamento F24
            notificationPaymentInfo.setF24(null);
        }
    }

}
