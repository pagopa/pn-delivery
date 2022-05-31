package it.pagopa.pn.delivery.models;

import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class ResultPaginationDto<R,K> {
    private List<R> resultsPage;
    private boolean moreResult;
    private List<K> nextPagesKey;
}
