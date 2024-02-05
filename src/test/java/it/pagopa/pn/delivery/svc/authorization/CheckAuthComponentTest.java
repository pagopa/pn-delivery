package it.pagopa.pn.delivery.svc.authorization;

import it.pagopa.pn.delivery.exception.PnMandateNotFoundException;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.CxTypeAuthFleet;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.InternalMandateDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewNotificationRequestV23;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationDigitalAddress;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipientV23;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatus;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

class CheckAuthComponentTest {

    private static final String X_PAGOPA_PN_SRC_CH = "sourceChannel";
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
        ReadAccessAuth readAccessAuth = ReadAccessAuth.newAccessRequest(cxType, cxId, null, null, iun, recipientIdx);

        // When
        Executable todo = () -> checkAuthComponent.canAccess( readAccessAuth, notification );

        // Then
        Assertions.assertThrows( IllegalArgumentException.class, todo );
    }


    @ParameterizedTest
    @ValueSource(strings = {"PG", "PF", "PA"})
    void canAccessPFPGPAUnauthorized(String cxType) {
        String cxId = "CX_ID";
        String iun = "IUN_01";
        Integer recipientIdx = 0;

        InternalNotification notification = newNotification();
        // Given
        ReadAccessAuth readAccessAuth = ReadAccessAuth.newAccessRequest(cxType, cxId, null, null, iun, recipientIdx);

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
        ReadAccessAuth readAccessAuth = ReadAccessAuth.newAccessRequest(cxType, cxId, mandateId, null, iun, recipientIdx);

        // When
        Mockito.when( mandateClient.listMandatesByDelegate( cxId, mandateId, CxTypeAuthFleet.PF, null ) )
                .thenReturn( Collections.singletonList(new InternalMandateDto()
                        .datefrom( "2022-01-01T00:00Z" )
                        .mandateId( mandateId )
                        .delegator( "recipientId" ))
                );
        AuthorizationOutcome authorizationOutcome = checkAuthComponent.canAccess( readAccessAuth, notification );

        // Then
        Assertions.assertFalse( authorizationOutcome.isAuthorized() );
    }

    @Test
    void canAccessPFWithMandateIdSuccess1() {
        String cxType = "PF";
        String cxId = "CX_ID";
        String iun = "IUN_01";
        String mandateId = "mandateId";
        Integer recipientIdx = 0;

        InternalNotification notification = newNotification();
        notification.setCancelledIun(iun);
        notification.setRecipientIds(List.of("recipientId"));
        notification.setRecipients(List.of(NotificationRecipient.builder().taxId("taxId").recipientType(NotificationRecipientV23.RecipientTypeEnum.PF).build()));
        // Given
        ReadAccessAuth readAccessAuth = ReadAccessAuth.newAccessRequest(cxType, cxId, mandateId, null, iun, recipientIdx);

        // When
        Mockito.when( mandateClient.listMandatesByDelegate( cxId, mandateId, CxTypeAuthFleet.PF, null ) )
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
    void canAccessPGWithMandateIdSuccess() {
        String cxType = "PG";
        String cxId = "CX_ID";
        String iun = "IUN_01";
        String mandateId = "mandateId";
        Integer recipientIdx = 0;

        InternalNotification notification = newNotification();
        // Given
        ReadAccessAuth readAccessAuth = ReadAccessAuth.newAccessRequest(cxType, cxId, mandateId, null, iun, recipientIdx);

        // When
        Mockito.when( mandateClient.listMandatesByDelegate( cxId, mandateId, CxTypeAuthFleet.PG, null ) )
                .thenReturn( Collections.singletonList(new InternalMandateDto()
                        .datefrom( "2022-01-01T00:00Z" )
                        .mandateId( mandateId )
                        .delegator( "recipientId" ))
                );
        AuthorizationOutcome authorizationOutcome = checkAuthComponent.canAccess( readAccessAuth, notification );

        // Then
        Assertions.assertFalse( authorizationOutcome.isAuthorized() );
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
        ReadAccessAuth readAccessAuth = ReadAccessAuth.newAccessRequest(cxType, cxId, mandateId, null, iun, recipientIdx);

        // When
        Mockito.when( mandateClient.listMandatesByDelegate( cxId, mandateId, CxTypeAuthFleet.PF, null ) )
                .thenReturn( Collections.emptyList());
        Executable todo = () -> checkAuthComponent.canAccess( readAccessAuth, notification );

        // Then
        Assertions.assertThrows( PnMandateNotFoundException.class, todo);
    }
    @Test
    void canAccessPGWithMandateIdNotFoundExc() {
        String cxType = "PG";
        String cxId = "CX_ID";
        String iun = "IUN_01";
        String mandateId = "mandateId";
        Integer recipientIdx = 0;

        InternalNotification notification = newNotification();
        // Given
        ReadAccessAuth readAccessAuth = ReadAccessAuth.newAccessRequest(cxType, cxId, mandateId, null, iun, recipientIdx);

        // When
        Mockito.when( mandateClient.listMandatesByDelegate( cxId, mandateId, CxTypeAuthFleet.PG, null ) )
                .thenReturn( Collections.emptyList());
        Executable todo = () -> checkAuthComponent.canAccess( readAccessAuth, notification );

        // Then
        Assertions.assertThrows( PnNotFoundException.class, todo);
    }


    @Test
    void canAccessPASuccess() {
        String cxType = "PA";
        String cxId = "pa_02";
        String iun = "IUN_01";
        Integer recipientIdx = 0;

        InternalNotification notification = newNotification();
        // Given
        ReadAccessAuth readAccessAuth = ReadAccessAuth.newAccessRequest(cxType, cxId, null, null, iun, recipientIdx);

        // When
        AuthorizationOutcome authorizationOutcome = checkAuthComponent.canAccess( readAccessAuth, notification );

        // Then
        Assertions.assertFalse( authorizationOutcome.isAuthorized() );
    }

    private InternalNotification newNotification() {
        InternalNotification internalNotification = new InternalNotification();
        internalNotification.setSourceChannel(X_PAGOPA_PN_SRC_CH);
        internalNotification.setSentAt(OffsetDateTime.MAX);
        internalNotification.setPagoPaIntMode(NewNotificationRequestV23.PagoPaIntModeEnum.NONE);
        internalNotification.setRecipientIds(List.of("IUN_01"));
        internalNotification.setIun("IUN_01");
        internalNotification.setPaProtocolNumber("protocol_01");
        internalNotification.setSubject("Subject 01");
        internalNotification.setCancelledIun("IUN_05");
        internalNotification.setCancelledIun("IUN_00");
        internalNotification.setSenderPaId("PA_ID");
        internalNotification.setNotificationStatus(NotificationStatus.ACCEPTED);
        internalNotification.setRecipients(Collections.singletonList(
                NotificationRecipient.builder()
                        .taxId("Codice Fiscale 01")
                        .denomination("Nome Cognome/Ragione Sociale")
                        .internalId( "recipientInternalId" )
                        .digitalDomicile(it.pagopa.pn.delivery.models.internal.notification.NotificationDigitalAddress.builder()
                                .type( NotificationDigitalAddress.TypeEnum.PEC )
                                .address("account@dominio.it")
                                .build()).build()));
        return internalNotification;
    }
}
