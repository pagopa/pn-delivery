package it.pagopa.pn.delivery.svc.authorization;

import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.clients.mandate.model.InternalMandateDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;


class CheckAuthComponentTest {

    @Mock
    private PnMandateClientImpl mandateClient;

    private CheckAuthComponent checkAuthComponent;

    @BeforeEach
    void setup() {
        this.mandateClient = Mockito.mock( PnMandateClientImpl.class );
        this.checkAuthComponent = new CheckAuthComponent( mandateClient );
    }

    @Test
    void canAccessIllegalArgumentFailure() {
        String cxType = "PF";
        String cxId = "CX_ID";
        String iun = "IUN";
        Integer recipientIdx = 0;

        InternalNotification notification = newNotification();
        // Given
        ReadAccessAuth readAccessAuth = ReadAccessAuth.newAccessRequest(cxType, cxId, null, iun, recipientIdx);

        // When
        Executable todo = () -> checkAuthComponent.canAccess( readAccessAuth, notification );

        // Then
        Assertions.assertThrows( IllegalArgumentException.class, todo );
    }

    @Test
    void canAccessPFUnauthorized() {
        String cxType = "PF";
        String cxId = "CX_ID";
        String iun = "IUN_01";
        Integer recipientIdx = 0;

        InternalNotification notification = newNotification();
        // Given
        ReadAccessAuth readAccessAuth = ReadAccessAuth.newAccessRequest(cxType, cxId, null, iun, recipientIdx);

        // When
        AuthorizationOutcome authorizationOutcome = checkAuthComponent.canAccess( readAccessAuth, notification );

        // Then
        Assertions.assertFalse( authorizationOutcome.isAuthorized() );
    }

    @Test
    void canAccessPFWithMandateIdSuccess() {
        String cxType = "PF";
        String cxId = "CX_ID";
        String iun = "IUN_01";
        String mandateId = "mandateId";
        Integer recipientIdx = 0;

        InternalNotification notification = newNotification();
        // Given
        ReadAccessAuth readAccessAuth = ReadAccessAuth.newAccessRequest(cxType, cxId, mandateId, iun, recipientIdx);

        // When
        Mockito.when( mandateClient.listMandatesByDelegate( cxId, mandateId, null, null ) ) // FIXME
                .thenReturn( Collections.singletonList(new InternalMandateDto()
                        .datefrom( "2022-01-01T00:00Z" )
                        .mandateId( mandateId )
                        .delegator( "recipientId" ))
                );
        AuthorizationOutcome authorizationOutcome = checkAuthComponent.canAccess( readAccessAuth, notification );

        // Then
        Assertions.assertTrue( authorizationOutcome.isAuthorized() );
    }

    @Test
    void canAccessPFWithMandateIdNotFoundExc() {
        String cxType = "PF";
        String cxId = "CX_ID";
        String iun = "IUN_01";
        String mandateId = "mandateId";
        Integer recipientIdx = 0;

        InternalNotification notification = newNotification();
        // Given
        ReadAccessAuth readAccessAuth = ReadAccessAuth.newAccessRequest(cxType, cxId, mandateId, iun, recipientIdx);

        // When
        Mockito.when( mandateClient.listMandatesByDelegate( cxId, mandateId, null, null ) ) // FIXME
                .thenReturn( Collections.emptyList());
        Executable todo = () -> checkAuthComponent.canAccess( readAccessAuth, notification );

        // Then
        Assertions.assertThrows( PnNotFoundException.class, todo);
    }

    @Test
    void canAccessPAUnauthorized() {
        String cxType = "PA";
        String cxId = "CX_ID";
        String iun = "IUN_01";
        Integer recipientIdx = 0;

        InternalNotification notification = newNotification();
        // Given
        ReadAccessAuth readAccessAuth = ReadAccessAuth.newAccessRequest(cxType, cxId, null, iun, recipientIdx);

        // When
        AuthorizationOutcome authorizationOutcome = checkAuthComponent.canAccess( readAccessAuth, notification );

        // Then
        Assertions.assertFalse( authorizationOutcome.isAuthorized() );
    }

    @Test
    void canAccessPASuccess() {
        String cxType = "PA";
        String cxId = "pa_02";
        String iun = "IUN_01";
        Integer recipientIdx = 0;

        InternalNotification notification = newNotification();
        // Given
        ReadAccessAuth readAccessAuth = ReadAccessAuth.newAccessRequest(cxType, cxId, null, iun, recipientIdx);

        // When
        AuthorizationOutcome authorizationOutcome = checkAuthComponent.canAccess( readAccessAuth, notification );

        // Then
        Assertions.assertTrue( authorizationOutcome.isAuthorized() );
    }



    private InternalNotification newNotification() {
        return new InternalNotification(FullSentNotification.builder()
                .iun("IUN_01")
                .paProtocolNumber("protocol_01")
                .subject("Subject 01")
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .senderPaId( "pa_02" )
                .notificationStatus( NotificationStatus.ACCEPTED )
                .sentAt( OffsetDateTime.parse( "2022-08-26T00:00Z" ) )
                .recipients( Collections.singletonList(
                        NotificationRecipient.builder()
                                .taxId("Codice Fiscale 01")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(NotificationDigitalAddress.builder()
                                        .type( NotificationDigitalAddress.TypeEnum.PEC )
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .documents(Arrays.asList(
                        NotificationDocument.builder()
                                .ref( NotificationAttachmentBodyRef.builder()
                                        .key("doc00")
                                        .versionToken("v01_doc00")
                                        .build()
                                )
                                .digests(NotificationAttachmentDigests.builder()
                                        .sha256("sha256_doc00")
                                        .build()
                                )
                                .build(),
                        NotificationDocument.builder()
                                .ref( NotificationAttachmentBodyRef.builder()
                                        .key("doc01")
                                        .versionToken("v01_doc01")
                                        .build()
                                )
                                .digests(NotificationAttachmentDigests.builder()
                                        .sha256("sha256_doc01")
                                        .build()
                                )
                                .build()
                ))
                .timeline( Collections.singletonList(TimelineElement.builder().build()))
                .notificationStatusHistory( Collections.singletonList( NotificationStatusHistoryElement.builder()
                        .status( NotificationStatus.ACCEPTED )
                        .build() ) )
                .build(), Collections.singletonList( "recipientId" ));
    }
}
