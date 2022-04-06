package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.directaccesstoken.DirectAccessToken;
import it.pagopa.pn.commons.pnclients.recipientschallenge.RecipientsChallenge;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DirectAccessServiceTest {
    private static final String SECRET = "TAX_ID-secret";
    NotificationDao notificationDao;
    RecipientsChallenge recipientsChallenge;
    DirectAccessService directAccessService;

    @BeforeEach
    public void setup() {
        notificationDao = Mockito.mock(NotificationDao.class);
        recipientsChallenge = Mockito.mock(RecipientsChallenge.class);

        directAccessService = new DirectAccessService(notificationDao, recipientsChallenge);
    }

    @Test
    void notValidToken() {
        //Given
        String iun = "IUN";
        String taxId = "TAX_ID";
        String TOKEN = "TOKEN";

        Optional<Notification> notification = Optional.ofNullable(Notification.builder()
                .iun(iun)
                .sentAt( Instant.parse("2021-09-16T15:00:00.00Z") )
                .subject( "Subject" )
                .sender(NotificationSender.builder()
                        .paId( "PAID" )
                        .build())
                .recipients( Collections.singletonList(NotificationRecipient.builder()
                        .taxId( taxId )
                        .token(TOKEN)
                        .build()) )
                .build());

        Optional<DirectAccessToken> expectedDirectAccessToken = Optional.empty();

        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( notification );
        Mockito.when( recipientsChallenge.getSecret( taxId )).thenReturn( SECRET );
        Optional<DirectAccessToken> directAccessToken = directAccessService.doChallenge( TOKEN , SECRET);
        
        //Then
        assertEquals( expectedDirectAccessToken, directAccessToken );
    }
    
    @Test
    void doChallengeSuccess() {
        //Given
        String iun = "IUN";
        String taxId = "TAX_ID";
        String token = directAccessService.generateToken(iun, taxId);
        String taxId2 = "TAX_ID_2";
        String token2 = directAccessService.generateToken(iun, "TAX_ID_2");

        List<NotificationRecipient> recipients = new ArrayList<>();
        recipients.add(NotificationRecipient.builder()
                .taxId( taxId )
                .token(token)
                .build());
        recipients.add(NotificationRecipient.builder()
                .taxId( taxId2 )
                .token(token2)
                .build());

        Optional<Notification> notification = Optional.ofNullable(Notification.builder()
                .iun(iun)
                .sentAt( Instant.parse("2021-09-16T15:00:00.00Z") )
                .subject( "Subject" )
                .sender(NotificationSender.builder()
                        .paId( "PAID" )
                        .build())
                .recipients( recipients )
                .build());
        
        Optional<DirectAccessToken> expectedDirectAccessToken = Optional.of( DirectAccessToken.builder()
                .taxId( taxId )
                .iun( iun )
                .token( token )
                .build());
        
        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( notification );
        Mockito.when( recipientsChallenge.getSecret( taxId )).thenReturn( SECRET );
        Optional<DirectAccessToken> directAccessToken = directAccessService.doChallenge( token , SECRET);

        //Then
        assertEquals( expectedDirectAccessToken, directAccessToken );
    }

    @Test
    void doChallengeFailure() {
        //Given
        String iun = "IUN";
        String taxId = "TAX_ID";
        String TOKEN = directAccessService.generateToken(iun, taxId);

        Optional<Notification> notification = Optional.ofNullable(Notification.builder()
                .iun(iun)
                .sentAt( Instant.parse("2021-09-16T15:00:00.00Z") )
                .subject( "Subject" )
                .sender(NotificationSender.builder()
                        .paId( "PAID" )
                        .build())
                .recipients( Collections.singletonList(NotificationRecipient.builder()
                        .taxId( taxId )
                        .token(TOKEN)
                        .build()) )
                .build());

        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( notification );
        Mockito.when( recipientsChallenge.getSecret( taxId )).thenReturn( "secret diverso" );
        Optional<DirectAccessToken> directAccessToken = directAccessService.doChallenge( TOKEN , SECRET);

        //Then
        assertEquals( Optional.empty(), directAccessToken );
    }
}
