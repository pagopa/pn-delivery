package it.pagopa.pn.delivery.middleware.notificationdao.entities;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.util.Map;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@DynamoDbBean
public class TaxonomyCodeEntity {
    public static final String FIELD_KEY = "key";
    public static final String FIELD_PAID = "PAId";
    public static final String FIELD_DESCRIPTION = "description";

    private String key;
    private String paId;
    private Map<String, String> description;

    @DynamoDbPartitionKey
    @DynamoDbAttribute(value = FIELD_KEY)
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute(value = FIELD_PAID)
    public String getPaId() {
        return paId;
    }

    public void setPaId(String paId) {
        this.paId = paId;
    }

    @DynamoDbAttribute(value = FIELD_DESCRIPTION)
    public Map<String, String> getDescription() {
        return description;
    }

    public void setDescription(Map<String, String> description) {
        this.description = description;
    }
}