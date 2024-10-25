package it.pagopa.pn.delivery.middleware.notificationdao.entities;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.Map;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@DynamoDbBean
public class TaxonomyCodeEntity {

    public static final String FIELD_KEY = "key";
    public static final String FIELD_PAID = "PAId";

    @Getter(onMethod=@__({@DynamoDbPartitionKey}))
    private String key;
    @Getter(onMethod=@__ ({@DynamoDbAttribute(FIELD_PAID)}))
    private String paId;
    private Map<String,Object> description;





}
