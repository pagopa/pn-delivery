package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.delivery.LocalStackTestConfig;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationCostEntity;
import it.pagopa.pn.delivery.models.InternalNotificationCost;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static it.pagopa.pn.delivery.middleware.notificationdao.entities.RecipientTypeEntity.PF;
import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        NotificationEntityDao.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO,
        "pn.delivery.notification-dao.table-name=Notifications",
        "pn.delivery.notification-cost-dao.table-name=NotificationsCost",
        "pn.delivery.notification-metadata-dao.table-name=NotificationsMetadata",
        "pn.delivery.notification-qr-dao.table-name=NotificationsQR",
        "pn.delivery.max-recipients-count=0",
        "pn.delivery.max-attachments-count=0"
})
@SpringBootTest
@Import(LocalStackTestConfig.class)
class NotificationEntityCostDynamoTestIT {

    @Autowired
    private NotificationCostEntityDao notificationCostEntityDao;

    @Test
    void deleteWithCheckIunOKTest() {
        String iun = "a-iun";
        String paTaxId = "paTAxId";
        String noticeCode = "a-notice-code";
        NotificationCostEntity entity = new NotificationCostEntity();
        entity.setIun(iun);
        entity.setRecipientIdx(1);
        entity.setRecipientType(PF.getValue());
        entity.setCreditorTaxIdNoticeCode(paTaxId + "##" + noticeCode);

        notificationCostEntityDao.put(entity);

        Optional<InternalNotificationCost> notificationByPaymentInfo = notificationCostEntityDao.getNotificationByPaymentInfo(paTaxId, noticeCode);
        assertThat(notificationByPaymentInfo).isPresent();

        notificationCostEntityDao.deleteWithCheckIun(paTaxId, noticeCode, iun);

        notificationByPaymentInfo = notificationCostEntityDao.getNotificationByPaymentInfo(paTaxId, noticeCode);
        assertThat(notificationByPaymentInfo).isEmpty();

    }

    @Test
    void deleteWithCheckIunWithCheckIunFailedTest() {
        String iun = "a-iun-2";
        String paTaxId = "paTAxId-2";
        String noticeCode = "a-notice-code-2";
        NotificationCostEntity entity = new NotificationCostEntity();
        entity.setIun(iun);
        entity.setRecipientIdx(1);
        entity.setRecipientType(PF.getValue());
        entity.setCreditorTaxIdNoticeCode(paTaxId + "##" + noticeCode);

        notificationCostEntityDao.put(entity);

        Optional<InternalNotificationCost> notificationByPaymentInfo = notificationCostEntityDao.getNotificationByPaymentInfo(paTaxId, noticeCode);
        assertThat(notificationByPaymentInfo).isPresent();

        notificationCostEntityDao.deleteWithCheckIun(paTaxId, noticeCode, "another-iun");

        notificationByPaymentInfo = notificationCostEntityDao.getNotificationByPaymentInfo(paTaxId, noticeCode);
        assertThat(notificationByPaymentInfo).isPresent();

    }

    @Test
    void deleteWithCheckIunWithRecordNotPresentTest() {
        String iun = "a-iun-3";
        String paTaxId = "paTAxId-3";
        String noticeCode = "a-notice-code-3";
        NotificationCostEntity entity = new NotificationCostEntity();
        entity.setIun(iun);
        entity.setRecipientIdx(1);
        entity.setRecipientType(PF.getValue());
        entity.setCreditorTaxIdNoticeCode(paTaxId + "##" + noticeCode);

        Optional<InternalNotificationCost> notificationByPaymentInfo = notificationCostEntityDao.getNotificationByPaymentInfo(paTaxId, noticeCode);
        assertThat(notificationByPaymentInfo).isEmpty();

        notificationCostEntityDao.deleteWithCheckIun(paTaxId, noticeCode, iun);

        notificationByPaymentInfo = notificationCostEntityDao.getNotificationByPaymentInfo(paTaxId, noticeCode);
        assertThat(notificationByPaymentInfo).isEmpty();

    }
}
