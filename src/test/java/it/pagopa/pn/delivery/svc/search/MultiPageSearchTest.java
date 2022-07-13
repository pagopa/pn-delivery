package it.pagopa.pn.delivery.svc.search;


import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationSearchRow;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import it.pagopa.pn.delivery.utils.NotificationDaoMock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;

class MultiPageSearchTest {

    private NotificationDao notificationDao;
    private InputSearchNotificationDto inputSearchNotificationDto;
    private PnDeliveryConfigs cfg;
    private PnDataVaultClientImpl dataVaultClient;

    @BeforeEach
    void setup() {
        this.notificationDao = new NotificationDaoMock();
        this.cfg = Mockito.mock( PnDeliveryConfigs.class );
        this.dataVaultClient = Mockito.mock( PnDataVaultClientImpl.class );
        this.inputSearchNotificationDto = new InputSearchNotificationDto.Builder()
                .bySender( true )
                .startDate( Instant.now() )
                .endDate( Instant.now() )
                .size( 10 )
                .build();
    }

    @Test
    void searchNotificationMetadata() {
        MultiPageSearch multiPageSearch = new MultiPageSearch(
                notificationDao,
                inputSearchNotificationDto,
                null,
                cfg, dataVaultClient);

        Mockito.when( cfg.getMaxPageSize() ).thenReturn( 4 );

        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> result = multiPageSearch.searchNotificationMetadata();

        Assertions.assertNotNull( result );
    }
}
