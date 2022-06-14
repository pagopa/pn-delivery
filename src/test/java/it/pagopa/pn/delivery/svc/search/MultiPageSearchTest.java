package it.pagopa.pn.delivery.svc.search;




import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationSearchRow;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatus;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.EntityToDtoNotificationMetadataMapper;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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

    private static class NotificationDaoMock implements NotificationDao {

        private final EntityToDtoNotificationMetadataMapper entityToDto = new EntityToDtoNotificationMetadataMapper();

        private final Map<Key, NotificationMetadataEntity> storage = new ConcurrentHashMap<>();


        @Override
        public void addNotification(InternalNotification notification) throws IdConflictException {

        }

        @Override
        public Optional<InternalNotification> getNotificationByIun(String iun) {
            return Optional.empty();
        }

        @Override
        public ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> searchForOneMonth(InputSearchNotificationDto inputSearchNotificationDto, String indexName, String partitionValue, int size, PnLastEvaluatedKey lastEvaluatedKey) {

            return ResultPaginationDto.<NotificationSearchRow, PnLastEvaluatedKey>builder()
                    .resultsPage(Collections.singletonList( NotificationSearchRow.builder()
                            .iun( "IUN" )
                            //.group( "GRP" )
                            .paProtocolNumber( "paProtocolNumber" )
                            .notificationStatus( NotificationStatus.VIEWED )
                            .sender( "SenderId" )
                            .subject( "Subject" )
                            .recipients( List.of( "internalId1", "internalId2" ) )
                            .build() ))
                    .moreResult( false )
                    .build();
        }
    }
}
