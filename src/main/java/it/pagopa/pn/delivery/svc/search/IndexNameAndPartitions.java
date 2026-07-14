package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.models.InputSearchNotificationDelegatedDto;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_UNSUPPORTED_INDEX_NAME;

@Value
public class IndexNameAndPartitions {

    public enum SearchIndexEnum {
        INDEX_WITH_BOTH_IDS("senderId_recipientId"),

        INDEX_BY_SENDER("senderId"),

        INDEX_BY_RECEIVER("recipientId"),

        INDEX_BY_IUN("iun_recipientId"),

        INDEX_BY_DELEGATE("delegateId"),

        INDEX_BY_DELEGATE_GROUP("delegateId_groupId"),

        INDEX_BY_CAMPAIGN("campaignId"),

        INDEX_BY_CAMPAIGN_RECIPIENT("campaignId_recipientId");

        private final String value;

        SearchIndexEnum(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    public static final String PARTITION_KEY_SEPARATOR = "##";

    /*
    INDICI:
    tabella: iun_recipientId
    indici:
      recipientId -> recipientId_creationMonth
      senderId -> senderId_creationMonth
      senderId_recipientId -> senderId_recipientId
    */

    SearchIndexEnum indexName;

    List<String> partitions;

    public static IndexNameAndPartitions selectIndexAndPartitions(InputSearchNotificationDto searchParams) {
        SearchIndexEnum indexName = chooseIndex( searchParams );

        if( indexName == null ) {
            throw new PnInternalException("There is a bug in method IndexNameAndPartitions::chooseIndex; result can't be null",
                    ERROR_CODE_DELIVERY_UNSUPPORTED_INDEX_NAME);
        }

        List<String> partitions;
        if (SearchIndexEnum.INDEX_BY_IUN.equals( indexName ))
        {
            partitions = new ArrayList<>();
        }
        else if( SearchIndexEnum.INDEX_WITH_BOTH_IDS.equals( indexName ) ) {
            partitions = getPartitionValueWhenSenderAndReceiverIdsAreSpecified(searchParams);
        }
        else if( SearchIndexEnum.INDEX_BY_CAMPAIGN_RECIPIENT.equals( indexName ) ) {
            partitions = getCampaignAndRecipientPartition(searchParams);
        }
        else if( SearchIndexEnum.INDEX_BY_CAMPAIGN.equals( indexName ) ) {
            partitions = campaignIdAndMonthsPartitionsListBuilder(searchParams);
        }
        else {
            partitions = idAndMonthsPartitionsListBuilder( searchParams );
        }
        return new IndexNameAndPartitions( indexName, partitions );
    }

    public static IndexNameAndPartitions selectDelegatedIndexAndPartitions(InputSearchNotificationDelegatedDto searchParams) {
        SearchIndexEnum index = chooseDelegatedIndex(searchParams);
        if (index == null) {
            throw new PnInternalException("There is a bug in method IndexNameAndPartitions::chooseDelegatedIndex; result can't be null", ERROR_CODE_DELIVERY_UNSUPPORTED_INDEX_NAME);
        }
        List<String> partitions;
        if (SearchIndexEnum.INDEX_BY_DELEGATE_GROUP.equals(index)) {
            partitions = idAndGroupAndMonthsPartitionsListBuilder(searchParams);
        } else {
            partitions = idAndMonthsPartitionsListBuilder(searchParams);
        }
        return new IndexNameAndPartitions(index, partitions);
    }

    private static List<String> idAndMonthsPartitionsListBuilder(InputSearchNotificationDto searchParam) {
        String prefix = searchParam.getSenderReceiverId() + PARTITION_KEY_SEPARATOR;
        return prefixAndMonthsPartitionsListBuilder(prefix, searchParam.getStartDate(), searchParam.getEndDate());
    }

    private static List<String> campaignIdAndMonthsPartitionsListBuilder(InputSearchNotificationDto searchParam) {
        String prefix = searchParam.getCampaignId() + PARTITION_KEY_SEPARATOR;
        return prefixAndMonthsPartitionsListBuilder(prefix, searchParam.getStartDate(), searchParam.getEndDate());
    }

    @NotNull
    private static List<String> getCampaignAndRecipientPartition(InputSearchNotificationDto searchParams) {
        List<String> partitionValues = new ArrayList<>();
        // le notifiche bonarie possono avere destinatari sia PF che PG: come per la ricerca
        // mittente legale (INDEX_WITH_BOTH_IDS), si costruiscono entrambe le partizioni quando
        // entrambi gli id opachi sono disponibili, effettuando poi 2 query reali unite a valle
        if ( StringUtils.hasText( searchParams.getOpaqueFilterIdPF() ) ) {
            partitionValues.add(searchParams.getCampaignId()
                    + PARTITION_KEY_SEPARATOR + searchParams.getOpaqueFilterIdPF());
        }
        if ( StringUtils.hasText( searchParams.getOpaqueFilterIdPG() ) ) {
            partitionValues.add(searchParams.getCampaignId()
                    + PARTITION_KEY_SEPARATOR + searchParams.getOpaqueFilterIdPG());
        }
        // Se non sono disponibili gli ID opachi, lancia eccezione
        if ( partitionValues.isEmpty() ) {
            throw new PnInternalException("Opaque filter IDs are required for campaign search with recipient filter",
                    ERROR_CODE_DELIVERY_UNSUPPORTED_INDEX_NAME);
        }
        return partitionValues;
    }

    private static List<String> idAndMonthsPartitionsListBuilder(InputSearchNotificationDelegatedDto searchParam) {
        String prefix = searchParam.getDelegateId() + PARTITION_KEY_SEPARATOR;
        return prefixAndMonthsPartitionsListBuilder(prefix, searchParam.getStartDate(), searchParam.getEndDate());
    }

    private static List<String> idAndGroupAndMonthsPartitionsListBuilder(InputSearchNotificationDelegatedDto searchParam) {
        String prefix = searchParam.getDelegateId() + PARTITION_KEY_SEPARATOR + searchParam.getGroup() + PARTITION_KEY_SEPARATOR;
        return prefixAndMonthsPartitionsListBuilder(prefix, searchParam.getStartDate(), searchParam.getEndDate());
    }

    private static List<String> prefixAndMonthsPartitionsListBuilder(String prefix, Instant startDate, Instant endDate) {
        YearAndMonth start = YearAndMonth.fromInstant(startDate);
        YearAndMonth end = YearAndMonth.fromInstant(endDate);
        return start.generateStringFromThisMonthUntil(end, prefix);
    }

    @NotNull
    private static List<String> getPartitionValueWhenSenderAndReceiverIdsAreSpecified(InputSearchNotificationDto searchParams) {
        List<String> partitionValues = new ArrayList<>();

        boolean searchBySender = searchParams.isBySender();
        if(searchBySender) {
            if ( StringUtils.hasText( searchParams.getOpaqueFilterIdPF() ) ) {
                partitionValues.add(searchParams.getSenderReceiverId()
                        + PARTITION_KEY_SEPARATOR + searchParams.getOpaqueFilterIdPF());
            }
            if ( StringUtils.hasText( searchParams.getOpaqueFilterIdPG() ) ) {
                partitionValues.add(searchParams.getSenderReceiverId()
                        + PARTITION_KEY_SEPARATOR + searchParams.getOpaqueFilterIdPG());
            }
        }
        else {
            partitionValues.add( searchParams.getFilterId()
                    + PARTITION_KEY_SEPARATOR + searchParams.getSenderReceiverId()
            );
        }
        return partitionValues;
    }

    private static SearchIndexEnum chooseIndex( InputSearchNotificationDto searchParams ) {
        SearchIndexEnum indexName;

        // - Flusso di ricerca per campagna (notifiche bonarie): usa gli indici dedicati alla campagna
        if( searchParams.isByCampaign() ) {
            return chooseCampaignIndex( searchParams );
        }

        // - Se devo filtrare non solo in base a chi a eseguito la query ma anche al "lato opposto"
        //   della comunicazione di notifica ...
        if(StringUtils.hasText( searchParams.getIunMatch() )) {
            // ... uso l'indice principale
            indexName = SearchIndexEnum.INDEX_BY_IUN;
        } else if(StringUtils.hasText( searchParams.getFilterId() )) {
            // ... uso l'indice che concatena i due identificativi
            indexName = SearchIndexEnum.INDEX_WITH_BOTH_IDS;
        } else {
            // - Se ho solo un identificativo devo usare l'indice ...
            boolean searchBySender = searchParams.isBySender();
            if( searchBySender ) {
                // ... per mittente ...
                indexName = SearchIndexEnum.INDEX_BY_SENDER;
            }
            else {
                // ... o per destinatario.
                indexName = SearchIndexEnum.INDEX_BY_RECEIVER;
            }
        }
        return indexName;
    }

    private static SearchIndexEnum chooseCampaignIndex( InputSearchNotificationDto searchParams ) {
        // - Ricerca puntuale per IUN: riuso dell'indice principale, la verifica di appartenenza
        //   alla campagna avviene a valle (filtro in memoria).
        if( StringUtils.hasText( searchParams.getIunMatch() ) ) {
            return SearchIndexEnum.INDEX_BY_IUN;
        }
        // - Ricerca per campagna + destinatario specifico: indice campaignId##recipientId.
        else if( StringUtils.hasText( searchParams.getFilterId() ) ) {
            return SearchIndexEnum.INDEX_BY_CAMPAIGN_RECIPIENT;
        }
        // - Ricerca massiva per campagna: indice campaignId##YYYYMM (multi-mese).
        else {
            return SearchIndexEnum.INDEX_BY_CAMPAIGN;
        }
    }

    private static SearchIndexEnum chooseDelegatedIndex(InputSearchNotificationDelegatedDto searchParams) {
        if (StringUtils.hasText(searchParams.getGroup())) {
            return SearchIndexEnum.INDEX_BY_DELEGATE_GROUP;
        } else {
            return SearchIndexEnum.INDEX_BY_DELEGATE;
        }
    }

}
