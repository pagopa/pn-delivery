package it.pagopa.pn.delivery.svc.search;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class SearchTimeout {

    private SearchTimeout(){}
    private final static long SECOND_TO_NANOS = 1_000_000_000L;

    public static boolean isSearchTimeExpired(Integer searchTimeoutSeconds, long searchStartTimeNanos, IndexNameAndPartitions.SearchIndexEnum indexName) {
        if(indexName.equals(IndexNameAndPartitions.SearchIndexEnum.INDEX_BY_SENDER) &&  searchTimeoutSeconds != null && searchTimeoutSeconds > 0 ){
            long timeoutNanos = searchTimeoutSeconds * SECOND_TO_NANOS ;
            long elapsed = System.nanoTime() - searchStartTimeNanos;
            if (elapsed >= timeoutNanos) {
                long elapsedSeconds = elapsed/SECOND_TO_NANOS;
                log.warn("Timeout reached after {} seconds, stopping loop", elapsedSeconds);
                return true;
            }
        }
        return false;
    }
}
