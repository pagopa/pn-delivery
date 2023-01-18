package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.EntityToDtoNotificationMetadataMapper;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;

class NotificationSearchFactoryTest {


    private NotificationDao notificationDao;

    private EntityToDtoNotificationMetadataMapper entityToDto;

    private PnDeliveryConfigs cfg;

    private PnDataVaultClientImpl dataVaultClient;

    private PnMandateClientImpl mandateClient;

    NotificationSearchFactory notificationSearchFactory;

    @BeforeEach
    void setup() {
        notificationDao = Mockito.mock(NotificationDao.class);
        entityToDto = Mockito.mock(EntityToDtoNotificationMetadataMapper.class);
        cfg = Mockito.mock(PnDeliveryConfigs.class);
        dataVaultClient = Mockito.mock(PnDataVaultClientImpl.class);
        mandateClient = Mockito.mock(PnMandateClientImpl.class);
        notificationSearchFactory = new NotificationSearchFactory(notificationDao, entityToDto, cfg, dataVaultClient, mandateClient);
    }

    @Test
    void getMultiPageSearch() {
        InputSearchNotificationDto inputSearchNotificationDto = new InputSearchNotificationDto().toBuilder()
                .bySender( true )
                .startDate( Instant.now() )
                .endDate( Instant.now() )
                .size( 10 )
                .build();

        Mockito.when( cfg.getMaxPageSize() ).thenReturn( 4 );

        NotificationSearch result = notificationSearchFactory.getMultiPageSearch(inputSearchNotificationDto, null);

        Assertions.assertNotNull( result );
        Assertions.assertEquals(NotificationSearchMultiPage.class, result.getClass());
    }

    @Test
    void getMultiPageSearchExact() {
        InputSearchNotificationDto inputSearchNotificationDto = new InputSearchNotificationDto().toBuilder()
                .bySender( true )
                .startDate( Instant.now() )
                .endDate( Instant.now() )
                .iunMatch("123123123")
                .size( 10 )
                .build();

        Mockito.when( cfg.getMaxPageSize() ).thenReturn( 4 );

        NotificationSearch result = notificationSearchFactory.getMultiPageSearch(inputSearchNotificationDto, null);

        Assertions.assertNotNull( result );
        Assertions.assertEquals(NotificationSearchExact.class, result.getClass());
    }
}