package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

import static it.pagopa.pn.delivery.svc.search.IndexNameAndPartitions.SearchIndexEnum.*;

class IndexNameAndPartitionsTest {

    @Test
    void searchByIUN() {
        // - GIVEN
        InputSearchNotificationDto searchParams = new InputSearchNotificationDto().toBuilder()
                .bySender( true )
                .senderReceiverId( "senderId" )
                .iunMatch("iun123")
                .build();

        // - WHEN
        IndexNameAndPartitions indexAndPartitions;
        indexAndPartitions = IndexNameAndPartitions.selectIndexAndPartitions( searchParams );

        // - THAN
        Assertions.assertEquals( INDEX_BY_IUN, indexAndPartitions.getIndexName() );
        Assertions.assertEquals(
                0,
                indexAndPartitions.getPartitions().size()
        );
    }

    @Test
    void searchBySenderWithReceiverFilter() {
        // - GIVEN
        InputSearchNotificationDto searchParams = new InputSearchNotificationDto().toBuilder()
                .bySender( true )
                .senderReceiverId( "senderId" )
                .filterId( "recipientId" )
                .build();

        // - WHEN
        IndexNameAndPartitions indexAndPartitions;
        indexAndPartitions = IndexNameAndPartitions.selectIndexAndPartitions( searchParams );

        // - THAN
        Assertions.assertEquals( INDEX_WITH_BOTH_IDS, indexAndPartitions.getIndexName() );
        Assertions.assertEquals(
                Collections.singletonList("senderId##recipientId"),
                indexAndPartitions.getPartitions()
            );
    }


    @Test
    void searchByReceiverWithSenderFilter() {
        // - GIVEN
        InputSearchNotificationDto searchParams = new InputSearchNotificationDto().toBuilder()
                .bySender( false )
                .senderReceiverId( "recipientId" )
                .filterId( "senderId" )
                .build();

        // - WHEN
        IndexNameAndPartitions indexAndPartitions;
        indexAndPartitions = IndexNameAndPartitions.selectIndexAndPartitions( searchParams );

        // - THAN
        Assertions.assertEquals( INDEX_WITH_BOTH_IDS, indexAndPartitions.getIndexName() );
        Assertions.assertEquals(
                Collections.singletonList("senderId##recipientId"),
                indexAndPartitions.getPartitions()
        );
    }


    @Test
    void searchBySender() {
        // - GIVEN
        InputSearchNotificationDto searchParams = new InputSearchNotificationDto().toBuilder()
                .bySender( true )
                .senderReceiverId( "senderId" )
                .startDate(Instant.parse("2020-10-13T10:00:00Z"))
                .endDate(Instant.parse("2020-12-13T10:00:00Z"))
                .build();

        // - WHEN
        IndexNameAndPartitions indexAndPartitions;
        indexAndPartitions = IndexNameAndPartitions.selectIndexAndPartitions( searchParams );

        // - THAN
        Assertions.assertEquals( INDEX_BY_SENDER, indexAndPartitions.getIndexName() );
        Assertions.assertEquals(
                Arrays.asList("senderId##202012","senderId##202011","senderId##202010"),
                indexAndPartitions.getPartitions()
        );
    }


    @Test
    void searchByReceiver() {
        // - GIVEN
        InputSearchNotificationDto searchParams = new InputSearchNotificationDto().toBuilder()
                .bySender( false )
                .senderReceiverId( "receiverId" )
                .startDate(Instant.parse("2020-10-13T10:00:00Z"))
                .endDate(Instant.parse("2020-12-13T10:00:00Z"))
                .build();

        // - WHEN
        IndexNameAndPartitions indexAndPartitions;
        indexAndPartitions = IndexNameAndPartitions.selectIndexAndPartitions( searchParams );

        // - THAN
        Assertions.assertEquals( INDEX_BY_RECEIVER, indexAndPartitions.getIndexName() );
        Assertions.assertEquals(
                Arrays.asList("receiverId##202012","receiverId##202011","receiverId##202010"),
                indexAndPartitions.getPartitions()
        );
    }

}
