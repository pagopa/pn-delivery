package it.pagopa.pn.delivery.middleware.notificationdao.entities;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Data
@DynamoDbBean
public class PaNotificationLimitEntity {
    public static final String FIELD_PK = "pk";
    public static final String FIELD_REQUESTED_LIMIT = "requestedLimit";
    public static final String FIELD_RESIDUAL_LIMIT = "residualLimit";
    public static final String FIELD_DAILY_COUNTER_01 = "dailyCounter01";

    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute(FIELD_PK)})) private String pk;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_REQUESTED_LIMIT)})) private Integer requestedLimit;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_RESIDUAL_LIMIT)})) private Integer residualLimit;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DAILY_COUNTER_01)})) private Integer dailyCounter01;
}
