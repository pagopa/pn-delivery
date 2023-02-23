package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.delivery.LocalStackTestConfig;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.FullSentNotification;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewNotificationRequest;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.*;
import it.pagopa.pn.delivery.models.InternalNotificationCost;
import it.pagopa.pn.delivery.models.InternalNotificationQR;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        NotificationEntityDao.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO,
        "pn.delivery.notification-dao.table-name=Notifications",
        "pn.delivery.notification-cost-dao.table-name=NotificationsCost",
        "pn.delivery.notification-metadata-dao.table-name=NotificationsMetadata",
        "pn.delivery.notification-qr-dao.table-name=NotificationsQR"
    })
@SpringBootTest
@Import(LocalStackTestConfig.class)
class NotificationEntityDaoDynamoTestIT {

    @Autowired
    private NotificationEntityDao notificationEntityDao;

    @Autowired
    private NotificationDaoDynamo notificationDao;

    @Autowired
    private NotificationCostEntityDao notificationCostEntityDao;

    @Autowired
    private NotificationQREntityDao notificationQREntityDao;

    @Test
    void putSuccess() throws PnIdConflictException {
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
        removeFromNotificationQRDb( notificationToInsert.getIun() );

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
        //GIVEN
        putSuccess();

        //WHEN
        Optional<InternalNotificationCost> result = notificationCostEntityDao.getNotificationByPaymentInfo( "creditorTaxId", "noticeCode" );

        //THEN
        Assertions.assertNotNull( result );
        Assertions.assertEquals( "IUN_01" , result.get().getIun() );
    }

    @Test
    void getNotificationByQR() {
        //GIVEN
        putSuccess();

        //WHEN
        String token = notificationQREntityDao.getQRByIun("IUN_01").get("recipientTaxId");
        Optional<InternalNotificationQR> result = notificationQREntityDao.getNotificationByQR( token );


        //THEN
        Assertions.assertNotNull( result );
        Assertions.assertEquals( token, result.get().getAarQRCodeValue() );
    }

    
    @Test
    void getRequestIdByPaProtocolNumberAndIdempotenceToken() {
        //GIVEN
        putSuccess();

        //WHEN
        Optional<String> requestId = notificationDao.getRequestId( "pa_02", "protocol_01", "idempotenceToken" );

        //THEN
        Assertions.assertNotNull( requestId );
        Assertions.assertEquals( "SVVOXzAx", requestId.get() );
    }



    private NotificationEntity newNotification() {
        NotificationRecipientEntity notificationRecipientEntity = NotificationRecipientEntity.builder()
                .recipientType(RecipientTypeEntity.PF)
                .recipientId("recipientTaxId")
                .digitalDomicile(NotificationDigitalAddressEntity.builder()
                        .address("address@pec.it")
                        .type(DigitalAddressTypeEntity.PEC)
                        .build())
                .denomination("recipientDenomination")
                .payment(NotificationPaymentInfoEntity.builder()
                        .creditorTaxId("creditorTaxId")
                        .noticeCode("noticeCode")
                        .noticeCodeAlternative("noticeCode_opt")
                        .pagoPaForm(PaymentAttachmentEntity.builder()
                                .contentType("application/pdf")
                                .digests(AttachmentDigestsEntity.builder()
                                        .sha256("sha256")
                                        .build())
                                .ref(AttachmentRefEntity.builder()
                                        .key("key")
                                        .versionToken("versionToken")
                                        .build())
                                .build())
                        .build())
                .physicalAddress(NotificationPhysicalAddressEntity.builder()
                        .address("address")
                        .addressDetails("addressDetail")
                        .zip("zip")
                        .at("at")
                        .municipality("municipality")
                        .province("province")
                        .municipalityDetails("municipalityDetails")
                        .build())
                .build();
        NotificationRecipientEntity notificationRecipientEntity1 = NotificationRecipientEntity.builder()
                .recipientType(RecipientTypeEntity.PF)
                .payment(NotificationPaymentInfoEntity.builder()
                        .creditorTaxId("77777777777")
                        .noticeCode("002720356512737953")
                        .build())
                .physicalAddress(NotificationPhysicalAddressEntity.builder()
                        .foreignState("Svizzera")
                        .address("via canton ticino")
                        .at("presso")
                        .addressDetails("19")
                        .municipality("cCantonticino")
                        .province("cantoni")
                        .zip("00100")
                        .municipalityDetails("frazione1")
                        .build())
                .recipientId( "fakeRecipientId" )
                .build();
        return NotificationEntity.builder()
                .iun("IUN_01")
                .notificationAbstract( "Abstract" )
                .idempotenceToken( "idempotenceToken" )
                .paNotificationId("protocol_01")
                .subject("Subject 01")
                .physicalCommunicationType(FullSentNotification.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890)
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .senderPaId( "pa_02" )
                .group( "Group_1" )
                .sentAt( Instant.now() )
                .notificationFeePolicy( NotificationFeePolicy.FLAT_RATE )
                .recipients( List.of(notificationRecipientEntity, notificationRecipientEntity1) )
                //.recipientsJson(Collections.emptyMap())
                .build();
    }

    private void removeItemFromDb(Key key) {
        notificationEntityDao.delete( key );
    }

    private void removeFromNotificationCostDb( Key key ){
        notificationCostEntityDao.delete( key );
    }

    private void removeFromNotificationQRDb(String iun) {
      
      notificationQREntityDao.getQRByIun("IUN_01").values().forEach(token ->{
        Key QRkey = Key.builder()
            .partitionValue(token)
            .build();
        
          notificationQREntityDao.delete( QRkey );
      });
      
 
    }
}
