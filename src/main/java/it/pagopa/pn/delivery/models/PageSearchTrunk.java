package it.pagopa.pn.delivery.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageSearchTrunk<T> {
    private List<T> results;
    private Map<String, AttributeValue> lastEvaluatedKey;
}
