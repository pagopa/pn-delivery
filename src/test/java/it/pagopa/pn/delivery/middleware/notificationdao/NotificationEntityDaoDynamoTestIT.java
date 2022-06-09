package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.FullSentNotification;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.*;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.NotificationCost;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        NotificationEntityDao.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO,
        "aws.region-code=us-east-1",
        "aws.profile-name=${PN_AWS_PROFILE_NAME:default}",
        "aws.endpoint-url=http://localhost:4566",
        "pn.delivery.notification-dao.table-name=Notifications",
        "pn.delivery.notification-cost-dao.table-name=NotificationsCost",
        "pn.delivery.notification-metadata-dao.table-name=NotificationsMetadata"
    })
@SpringBootTest
class NotificationEntityDaoDynamoTestIT {

    @Autowired
    private NotificationEntityDao notificationEntityDao;

    @Autowired
    private NotificationCostEntityDao notificationCostEntityDao;

    @Test
    void putSuccess() throws IdConflictException {
        //Given
        NotificationEntity notificationToInsert = newNotification();

        String controlIun = getControlIun(notificationToInsert);

        Key key = Key.builder()
                .partitionValue(notificationToInsert.getIun())
                .build();
        Key controlKey = Key.builder()
                .partitionValue( controlIun )
                .build();
        Key costKey = Key.builder()
                .partitionValue( "creditorTaxId##noticeCode" )
                .build();

        removeItemFromDb( key );
        removeItemFromDb( controlKey );
        removeFromNotificationCostDb( costKey );

        //When
        notificationEntityDao.putIfAbsent( notificationToInsert );

        //Then
        Optional<NotificationEntity> elementFromDb = notificationEntityDao.get( key );
        Optional<NotificationEntity> controlElementFromDb = notificationEntityDao.get( controlKey );

        Assertions.assertTrue( elementFromDb.isPresent() );
        Assertions.assertTrue( controlElementFromDb.isPresent() );
        Assertions.assertEquals( notificationToInsert, elementFromDb.get() );
        Assertions.assertEquals( controlIun, controlElementFromDb.get().getIun() );

    }

    @Test
    void getNotificationByPayment() {
        Optional<NotificationCost> result = notificationCostEntityDao.getNotificationByPaymentInfo( "creditorTaxId", "noticeCode" );

        Assertions.assertNotNull( result );
        Assertions.assertEquals( "IUN_01" , result.get().getIun() );
    }

    @NotNull
    private String getControlIun(NotificationEntity notificationToInsert) {
        return notificationToInsert.getSenderPaId()
                + "##" + notificationToInsert.getPaNotificationId()
                + "##" + notificationToInsert.getCancelledIun();
    }


    private NotificationEntity newNotification() {
        return NotificationEntity.builder()
                .iun("IUN_01")
                .paNotificationId("protocol_01")
                .subject("Subject 01")
                .physicalCommunicationType(FullSentNotification.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890)
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .senderPaId( "pa_02" )
                .group( "Group_1" )
                .recipients( Collections.singletonList(NotificationRecipientEntity.builder()
                                .recipientType( RecipientTypeEntity.PF )
                                .recipientId( "recipientTaxId" )
                                .digitalDomicile(NotificationDigitalAddressEntity.builder()
                                        .address( "address@pec.it" )
                                        .type( DigitalAddressTypeEntity.PEC )
                                        .build() )
                                .denomination( "recipientDenomination" )
                                .payment( NotificationPaymentInfoEntity.builder()
                                        .creditorTaxId( "creditorTaxId" )
                                        .noticeCode( "noticeCode" )
                                        .pagoPaForm( PaymentAttachmentEntity.builder()
                                                .contentType( "application/pdf" )
                                                .digests( AttachmentDigestsEntity.builder()
                                                        .sha256( "sha256" )
                                                        .build() )
                                                .ref( AttachmentRefEntity.builder()
                                                        .key( "key" )
                                                        .versionToken( "versionToken" )
                                                        .build() )
                                                .build() )
                                        .build() )
                        .build()) )
                //.recipientsJson(Collections.emptyMap())
                .build();
    }

    private void removeItemFromDb(Key key) {
        notificationEntityDao.delete( key );
    }

    private void removeFromNotificationCostDb( Key key ){
        notificationCostEntityDao.delete( key );
    }
}
