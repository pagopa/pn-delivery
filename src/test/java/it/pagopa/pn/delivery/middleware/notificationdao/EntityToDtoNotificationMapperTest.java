package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.FullSentNotificationV21;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.*;
import it.pagopa.pn.delivery.models.InternalNotification;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;


class EntityToDtoNotificationMapperTest {

    private EntityToDtoNotificationMapper mapper;

    @BeforeEach
    void setup() {
        this.mapper = new EntityToDtoNotificationMapper();
    }

    @Test
    void throwEmptyDocument(){
        NotificationEntity notificationEntity = newNotificationEntity();
        notificationEntity.setPhysicalCommunicationType(null);
        assertThrows(PnInternalException.class, () -> mapper.entity2Dto(notificationEntity));
    }

    @Test
    void entity2DtoSuccess() {
        // Given
        NotificationEntity notificationEntity = newNotificationEntity();

        // When
        InternalNotification internalNotification = mapper.entity2Dto(notificationEntity);

        // Then
        Assertions.assertNotNull( internalNotification );
        Assertions.assertEquals( "noticeCode" ,internalNotification.getRecipients().get( 0 ).getPayments().get(0).getPagoPa().getNoticeCode() );
        Assertions.assertNotNull( internalNotification.getRecipients().get( 0 ).getPayments().get(0).getPagoPa() );
        Assertions.assertNotNull( internalNotification.getRecipients().get( 1 ).getPayments().get(0).getPagoPa() );
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
                                        .f24(
                                                F24PaymentEntity.builder()
                                                        .title("title")
                                                        .applyCost(false)
                                                        .metadataAttachment(
                                                                MetadataAttachmentEntity.builder()
                                                                        .contentType("application/pdf")
                                                                        .digests(NotificationAttachmentDigestsEntity.builder()
                                                                                .sha256("Zsg9Nyzj13UPzkyaQlnA7wbgTfBaZmH02OVyiRjpydE=")
                                                                                .build())
                                                                        .ref(NotificationAttachmentBodyRefEntity.builder()
                                                                                .key("key")
                                                                                .versionToken("versionToken")
                                                                                .build())
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                        .noticeCode("noticeCode")
                                        .creditorTaxId("creditorTaxId")
                                        .applyCost(false)
                                        .pagoPaForm(PagoPaPaymentEntity.builder()
                                                .digests(NotificationAttachmentDigestsEntity.builder()
                                                        .sha256("sha256")
                                                        .build())
                                                .contentType("application/pdf")
                                                .ref(NotificationAttachmentBodyRefEntity.builder()
                                                        .key("key")
                                                        .versionToken("versionToken")
                                                        .build())
                                                .build())
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
                                        .f24(
                                                F24PaymentEntity.builder()
                                                        .title("title")
                                                        .applyCost(false)
                                                        .metadataAttachment(
                                                                MetadataAttachmentEntity.builder()
                                                                        .contentType("application/pdf")
                                                                        .digests(NotificationAttachmentDigestsEntity.builder()
                                                                                .sha256("Zsg9Nyzj13UPzkyaQlnA7wbgTfBaZmH02OVyiRjpydE=")
                                                                                .build())
                                                                        .ref(NotificationAttachmentBodyRefEntity.builder()
                                                                                .key("key")
                                                                                .versionToken("versionToken")
                                                                                .build())
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                        .noticeCode("noticeCode")
                                        .creditorTaxId("creditorTaxId")
                                        .applyCost(false)
                                        .pagoPaForm(PagoPaPaymentEntity.builder()
                                                        .contentType("application/pdf")
                                                        .digests(NotificationAttachmentDigestsEntity.builder()
                                                                .sha256("sha256")
                                                                .build())
                                                        .ref(NotificationAttachmentBodyRefEntity.builder()
                                                                .key("key")
                                                                .versionToken("versionToken")
                                                                .build())
                                                        .build()
                                                )
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
                .physicalCommunicationType(FullSentNotificationV21.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890)
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .senderPaId( "pa_02" )
                .group( "Group_1" )
                .sentAt( Instant.now() )
                .notificationFeePolicy( NotificationFeePolicy.FLAT_RATE )
                .recipients( List.of(notificationRecipientEntity, notificationRecipientEntity1) )
                .version( 1 )
                //.recipientsJson(Collections.emptyMap())
                .build();
    }

}