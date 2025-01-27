package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusV26;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.EntityToDtoNotificationMetadataMapper;
import it.pagopa.pn.delivery.models.InputSearchNotificationDelegatedDto;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.mock;

class NotificationSearchFactoryTest {


    private PnDeliveryConfigs cfg;

    NotificationSearchFactory notificationSearchFactory;

    private static final String START_DATE = "2021-09-17T00:00:00.000Z";
    private static final String END_DATE = "2021-09-18T00:00:00.000Z";
    private static final NotificationStatusV26 STATUS = NotificationStatusV26.IN_VALIDATION;
    private static final String RECIPIENT_ID = "CGNNMO80A01H501M";

    public static final List<String> GROUPS = List.of("Group1", "Group2");


    @BeforeEach
    void setup() {
        NotificationDao notificationDao = mock(NotificationDao.class);
        EntityToDtoNotificationMetadataMapper entityToDto = mock(EntityToDtoNotificationMetadataMapper.class);
        cfg = mock(PnDeliveryConfigs.class);
        PnDataVaultClientImpl dataVaultClient = mock(PnDataVaultClientImpl.class);
        NotificationDelegatedSearchUtils notificationDelegatedSearchUtils = mock(NotificationDelegatedSearchUtils.class);
        notificationSearchFactory = new NotificationSearchFactory(notificationDao, entityToDto, cfg, dataVaultClient, notificationDelegatedSearchUtils);
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

    @Test
    void getMultiPageDelegatedSearchTestWithIun() {
        InputSearchNotificationDelegatedDto inputSearchNotificationDelegatedDto = InputSearchNotificationDelegatedDto.builder()
                .delegateId("test")
                .startDate(Instant.parse(START_DATE))
                .endDate(Instant.parse(END_DATE))
                .group(null)
                .senderId(null)
                .iun("iun")
                .statuses(List.of(STATUS))
                .size(null)
                .nextPageKey("eyJlayI6ImNfYjQyOSMjZWQ4NGI4YzktNDQ0ZS00MTBkLTgwZDctY2ZhZDZhYTEyMDcwIiwiaWsiOnsiaXVuX3JlY2lwaWVudElkIjoiY19iNDI5LTIwMjIwNDA1MTEyOCMjZWQ4NGI4YzktNDQ0ZS00MTBkLTgwZDctY2ZhZDZhYTEyMDcwIiwic2VudEF0IjoiMjAyMi0wNC0wNVQwOToyODo0Mi4zNTgxMzZaIiwic2VuZGVySWRfcmVjaXBpZW50SWQiOiJjX2I0MjkjI2VkODRiOGM5LTQ0NGUtNDEwZC04MGQ3LWNmYWQ2YWExMjA3MCJ9fQ")
                .cxGroups(GROUPS)
                .build();

        NotificationDelegatedSearchWithIun notificationSearch = (NotificationDelegatedSearchWithIun) notificationSearchFactory.getMultiPageDelegatedSearch(inputSearchNotificationDelegatedDto,null);

        Assertions.assertNotNull(notificationSearch);
        Assertions.assertEquals(NotificationDelegatedSearchWithIun.class, notificationSearch.getClass());
    }

    @Test
    void getMultiPageDelegatedSearchTestExact() {
        InputSearchNotificationDelegatedDto inputSearchNotificationDelegatedDto = InputSearchNotificationDelegatedDto.builder()
                .delegateId("test")
                .startDate(Instant.parse(START_DATE))
                .endDate(Instant.parse(END_DATE))
                .group(null)
                .senderId(null)
                .receiverId(RECIPIENT_ID)
                .statuses(List.of(STATUS))
                .iun("iun")
                .size(null)
                .nextPageKey("eyJlayI6ImNfYjQyOSMjZWQ4NGI4YzktNDQ0ZS00MTBkLTgwZDctY2ZhZDZhYTEyMDcwIiwiaWsiOnsiaXVuX3JlY2lwaWVudElkIjoiY19iNDI5LTIwMjIwNDA1MTEyOCMjZWQ4NGI4YzktNDQ0ZS00MTBkLTgwZDctY2ZhZDZhYTEyMDcwIiwic2VudEF0IjoiMjAyMi0wNC0wNVQwOToyODo0Mi4zNTgxMzZaIiwic2VuZGVySWRfcmVjaXBpZW50SWQiOiJjX2I0MjkjI2VkODRiOGM5LTQ0NGUtNDEwZC04MGQ3LWNmYWQ2YWExMjA3MCJ9fQ")
                .cxGroups(GROUPS)
                .build();

        NotificationDelegatedSearchExact notificationSearch = (NotificationDelegatedSearchExact) notificationSearchFactory.getMultiPageDelegatedSearch(inputSearchNotificationDelegatedDto,null);

        Assertions.assertNotNull(notificationSearch);
        Assertions.assertEquals(NotificationDelegatedSearchExact.class, notificationSearch.getClass());
    }
}