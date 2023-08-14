package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.FullSentNotification;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.*;
import it.pagopa.pn.delivery.models.InternalNotification;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;


class EntityToDtoNotificationMapperTest {

    private EntityToDtoNotificationMapper mapper;

    @BeforeEach
    void setup() {
        this.mapper = new EntityToDtoNotificationMapper();
    }

    @Test
    void entity2DtoSuccess() {
        // Given
        NotificationEntity notificationEntity = newNotificationEntity();

        // When
        InternalNotification internalNotification = mapper.entity2Dto(notificationEntity);

        // Then
        Assertions.assertNotNull( internalNotification );
    }

    private NotificationEntity newNotificationEntity() {
        NotificationRecipientEntity notificationRecipientEntity = NotificationRecipientEntity.builder()
                .recipientType(RecipientTypeEntity.PF)
                .recipientId("recipientTaxId")
                .digitalDomicile(NotificationDigitalAddressEntity.builder()
                        .address("address@pec.it")
                        .type(DigitalAddressTypeEntity.PEC)
                        .build())
                .denomination("recipientDenomination")
                .payments( List.of(
                                NotificationPaymentInfoEntity.builder()
                                        .creditorTaxId("creditorTaxId")
                                        .noticeCode("noticeCode")
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
                                        .build(),
                                NotificationPaymentInfoEntity.builder()
                                        .creditorTaxId("creditorTaxId")
                                        .noticeCode("noticeCode_opt")
                                        .build()
                        )
                )
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
                .payments( List.of(
                                NotificationPaymentInfoEntity.builder()
                                        .creditorTaxId("77777777777")
                                        .noticeCode("002720356512737953")
                                        .build()
                        )
                )
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
                .version( 1 )
                .build();
    }

}