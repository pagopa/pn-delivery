package it.pagopa.pn.delivery.svc.search;


import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.BaseRecipientDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationSearchRow;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.EntityToDtoNotificationMetadataMapper;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import it.pagopa.pn.delivery.utils.NotificationDaoMock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;

class NotificationSearchExactTest {

    private NotificationDao notificationDao;
    private InputSearchNotificationDto inputSearchNotificationDto;
    private PnDeliveryConfigs cfg;
    private PnDataVaultClientImpl dataVaultClient;
    private NotificationSearchExact notificationSearchExact;
    private EntityToDtoNotificationMetadataMapper entityToDtoNotificationMetadataMapper;

    @BeforeEach
    void setup() {
        this.notificationDao = new NotificationDaoMock();
        this.cfg = Mockito.mock( PnDeliveryConfigs.class );
        this.entityToDtoNotificationMetadataMapper = Mockito.mock(EntityToDtoNotificationMetadataMapper.class);
        this.dataVaultClient = Mockito.mock( PnDataVaultClientImpl.class );
        this.inputSearchNotificationDto = new InputSearchNotificationDto().toBuilder()
                .bySender( true )
                .startDate( Instant.now() )
                .endDate( Instant.now() )
                .size( 10 )
                .build();

        Mockito.when(entityToDtoNotificationMetadataMapper.entity2Dto((NotificationMetadataEntity) Mockito.any()))
                .thenReturn(NotificationSearchRow.builder()
                        .recipients(List.of("recipientId1"))
                        .build());

        BaseRecipientDto baseRecipientDto = new BaseRecipientDto();
        baseRecipientDto.setInternalId("recipientId1");
        baseRecipientDto.setDenomination("nome cognome");
        baseRecipientDto.setTaxId("EEEEEEEEEEEEE");
        Mockito.when(dataVaultClient.getRecipientDenominationByInternalId(Mockito.any())).thenReturn(List.of(baseRecipientDto));


        this.notificationSearchExact = new NotificationSearchExact(notificationDao, entityToDtoNotificationMetadataMapper, inputSearchNotificationDto,  dataVaultClient);
    }

    @Test
    void searchNotificationMetadata() {

        Mockito.when( cfg.getMaxPageSize() ).thenReturn( 4 );

        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> result = notificationSearchExact.searchNotificationMetadata();

        Assertions.assertNotNull( result );
    }
}
