package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.FullSentNotification;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewNotificationRequest;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.*;
import it.pagopa.pn.delivery.models.NotificationCost;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.time.Instant;
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
    private NotificationDaoDynamo notificationDao;

    @Autowired
    private NotificationCostEntityDao notificationCostEntityDao;

    @Test
    void putSuccess() throws IdConflictException {
        //Given
        NotificationEntity notificationToInsert = newNotification();

        String controlIdempotenceToken = getControlIdempotenceToken( notificationToInsert );

        Key key = Key.builder()
                .partitionValue(notificationToInsert.getIun())
                .build();
        Key controlIdempotenceKey = Key.builder()
                .partitionValue( controlIdempotenceToken )
                .build();
        Key costKey1 = Key.builder()
                .partitionValue( "creditorTaxId##noticeCode" )
                .build();
        Key costKey2 = Key.builder()
                .partitionValue( "77777777777##002720356512737953" )
                .build();
        Key costKey3 = Key.builder()
                .partitionValue("creditorTaxId##noticeCode_opt")
                .build();

        removeItemFromDb( key );
        removeItemFromDb( controlIdempotenceKey );
        removeFromNotificationCostDb( costKey1 );
        removeFromNotificationCostDb( costKey2 );
        removeFromNotificationCostDb( costKey3 );

        //When
        notificationEntityDao.putIfAbsent( notificationToInsert );

        //Then
        Optional<NotificationEntity> elementFromDb = notificationEntityDao.get( key );
        Optional<NotificationEntity> controlIdempotenceTokenElementFromDb = notificationEntityDao.get( controlIdempotenceKey );

        Assertions.assertTrue( elementFromDb.isPresent() );
        Assertions.assertTrue( controlIdempotenceTokenElementFromDb.isPresent() );
        Assertions.assertEquals( notificationToInsert, elementFromDb.get() );
        Assertions.assertEquals( controlIdempotenceToken, controlIdempotenceTokenElementFromDb.get().getIun() );

    }

    private String getControlIdempotenceToken(NotificationEntity notificationToInsert) {
        return notificationToInsert.getSenderPaId()
                + "##" + notificationToInsert.getPaNotificationId()
                + "##" + notificationToInsert.getIdempotenceToken();
    }

    @Test
    void getNotificationByPayment() {
        Optional<NotificationCost> result = notificationCostEntityDao.getNotificationByPaymentInfo( "creditorTaxId", "noticeCode" );

        Assertions.assertNotNull( result );
        Assertions.assertEquals( "IUN_01" , result.get().getIun() );
    }

    @Test
    void getRequestIdByPaProtocolNumberAndIdempotenceToken() {
        Optional<String> requestId = notificationDao.getRequestId( "pa_02", "protocol_01", "idempotenceToken" );

        Assertions.assertNotNull( requestId );
        Assertions.assertEquals( "SVVOXzAx", requestId.get() );
    }



    private NotificationEntity newNotification() {
        return NotificationEntity.builder()
                .iun("IUN_01")
                ._abstract( "Abstract" )
                .idempotenceToken( "idempotenceToken" )
                .paNotificationId("protocol_01")
                .subject("Subject 01")
                .physicalCommunicationType(FullSentNotification.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890)
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .senderPaId( "pa_02" )
                .group( "Group_1" )
                .sentAt( Instant.now() )
                .notificationFeePolicy( NewNotificationRequest.NotificationFeePolicyEnum.FLAT_RATE )
                .recipients( List.of(NotificationRecipientEntity.builder()
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
                                        .noticeCodeAlternative( "noticeCode_opt" )
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
                                        .physicalAddress( NotificationPhysicalAddressEntity.builder()
                                                .address( "address" )
                                                .addressDetails( "addressDetail" )
                                                .zip( "zip" )
                                                .at( "at" )
                                                .municipality( "municipality" )
                                                .province( "province" )
                                                .municipalityDetails( "municipalityDetails" )
                                                .build() )
                        .build(),
                        NotificationRecipientEntity.builder()
                                .payment( NotificationPaymentInfoEntity.builder()
                                        .creditorTaxId( "77777777777" )
                                        .noticeCode( "002720356512737953" )
                                        .build() )
                                .physicalAddress( NotificationPhysicalAddressEntity.builder()
                                        .foreignState( "Svizzera" )
                                        .address( "via canton ticino" )
                                        .at( "presso" )
                                        .addressDetails( "19" )
                                        .municipality( "cCantonticino" )
                                        .province( "cantoni" )
                                        .zip( "00100" )
                                        .municipalityDetails( "frazione1" )
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
