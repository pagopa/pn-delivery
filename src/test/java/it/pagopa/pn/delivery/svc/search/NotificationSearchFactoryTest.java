package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatus;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.EntityToDtoNotificationMetadataMapper;
import it.pagopa.pn.delivery.models.InputSearchNotificationDelegatedDto;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;

class NotificationSearchFactoryTest {


    private NotificationDao notificationDao;

    private EntityToDtoNotificationMetadataMapper entityToDto;

    private PnDeliveryConfigs cfg;

    private PnDataVaultClientImpl dataVaultClient;

    private PnMandateClientImpl mandateClient;

    NotificationSearchFactory notificationSearchFactory;

    private static final String START_DATE = "2021-09-17T00:00:00.000Z";
    private static final String END_DATE = "2021-09-18T00:00:00.000Z";
    private static final NotificationStatus STATUS = NotificationStatus.IN_VALIDATION;
    private static final String RECIPIENT_ID = "CGNNMO80A01H501M";

    public static final List<String> GROUPS = List.of("Group1", "Group2");


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
        Assertions.assertEquals(NotificationSearchMultiPageByPFOrPG.class, result.getClass());
    }

    @Test
    void getMultiPageSearchPFAndPG() {
        InputSearchNotificationDto inputSearchNotificationDto = new InputSearchNotificationDto().toBuilder()
                .bySender( true )
                .startDate( Instant.now() )
                .endDate( Instant.now() )
                .filterId( "EEEEEEEEEEEEEEEE" )
                .opaqueFilterIdPF( "opaqueFilterIdPF" )
                .opaqueFilterIdPG( "opaqueFilterIdPG" )
                .size( 10 )
                .build();

        Mockito.when( cfg.getMaxPageSize() ).thenReturn( 4 );

        NotificationSearch result = notificationSearchFactory.getMultiPageSearch(inputSearchNotificationDto, null);

        Assertions.assertNotNull( result );
        Assertions.assertEquals(NotificationSearchMultiPageByPFAndPGOnly.class, result.getClass());
    }

    @Test
    void getMultiPageSearchPFOrPG() {
        InputSearchNotificationDto inputSearchNotificationDto = new InputSearchNotificationDto().toBuilder()
                .bySender( true )
                .startDate( Instant.now() )
                .endDate( Instant.now() )
                .filterId( "12345678901" )
                .opaqueFilterIdPG( "opaqueFilterIdPG" )
                .size( 10 )
                .build();

        Mockito.when( cfg.getMaxPageSize() ).thenReturn( 4 );

        NotificationSearch result = notificationSearchFactory.getMultiPageSearch(inputSearchNotificationDto, null);

        Assertions.assertNotNull( result );
        Assertions.assertEquals(NotificationSearchMultiPageByPFOrPG.class, result.getClass());
    }

    @Test
    void getMultiPageSearchPF() {
        InputSearchNotificationDto inputSearchNotificationDto = new InputSearchNotificationDto().toBuilder()
                .bySender( true )
                .startDate( Instant.now() )
                .endDate( Instant.now() )
                .filterId( "EEEEEEEEEEEEEEEE" )
                .opaqueFilterIdPG( "opaqueFilterIdPF" )
                .size( 10 )
                .build();

        Mockito.when( cfg.getMaxPageSize() ).thenReturn( 4 );

        NotificationSearch result = notificationSearchFactory.getMultiPageSearch(inputSearchNotificationDto, null);

        Assertions.assertNotNull( result );
        Assertions.assertEquals(NotificationSearchMultiPageByPFOrPG.class, result.getClass());
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


    @Test
    void getMultiPageDelegatedSearchTest() {
        InputSearchNotificationDelegatedDto inputSearchNotificationDelegatedDto = InputSearchNotificationDelegatedDto.builder()
                .delegateId("test")
                .startDate(Instant.parse(START_DATE))
                .endDate(Instant.parse(END_DATE))
                .group(null)
                .senderId(null)
                .receiverId(RECIPIENT_ID)
                .statuses(List.of(STATUS))
                .size(null)
                .nextPageKey("eyJlayI6ImNfYjQyOSMjZWQ4NGI4YzktNDQ0ZS00MTBkLTgwZDctY2ZhZDZhYTEyMDcwIiwiaWsiOnsiaXVuX3JlY2lwaWVudElkIjoiY19iNDI5LTIwMjIwNDA1MTEyOCMjZWQ4NGI4YzktNDQ0ZS00MTBkLTgwZDctY2ZhZDZhYTEyMDcwIiwic2VudEF0IjoiMjAyMi0wNC0wNVQwOToyODo0Mi4zNTgxMzZaIiwic2VuZGVySWRfcmVjaXBpZW50SWQiOiJjX2I0MjkjI2VkODRiOGM5LTQ0NGUtNDEwZC04MGQ3LWNmYWQ2YWExMjA3MCJ9fQ")
                .cxGroups(GROUPS)
                .build();

        NotificationDelegatedSearchMultiPage notificationSearch = (NotificationDelegatedSearchMultiPage) notificationSearchFactory.getMultiPageDelegatedSearch(inputSearchNotificationDelegatedDto,null);

        Assertions.assertNotNull(notificationSearch);
        Assertions.assertEquals(NotificationDelegatedSearchMultiPage.class, notificationSearch.getClass());
    }
}