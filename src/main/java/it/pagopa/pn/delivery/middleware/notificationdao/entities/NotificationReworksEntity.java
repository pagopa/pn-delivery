package it.pagopa.pn.delivery.middleware.notificationdao.entities;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Data
@DynamoDbBean
public class NotificationReworksEntity {
    public static final String FIELD_IUN = "iun";
    public static final String FIELD_REWORK_ID = "reworkId";
    public static final String FIELD_INVALIDATED_TIMELINE_ELEMENT_IDS = "invalidatedTimelineElementIds";
    public static final String FIELD_REASON = "reason";
    public static final String FIELD_IDX = "idx";
    public static final String FIELD_EXPECTED_STATUS_CODES = "expectedStatusCodes";
    public static final String FIELD_EXPECTED_DELIVERY_FAILURE_CAUSE = "expectedDeliveryFailureCause";
    public static final String FILED_STATUS = "status";
    public static final String FIELD_ERRORS = "errors";
    public static final String FIELD_CREATED_AT = "createdAt";
    public static final String FIELD_UPDATED_AT = "updatedAt";

    @Getter(onMethod = @__({@DynamoDbPartitionKey, @DynamoDbAttribute(FIELD_IUN)}))
    private String iun;

    @Getter(onMethod = @__({@DynamoDbSortKey, @DynamoDbAttribute(FIELD_REWORK_ID)}))
    private String reworkId;

    @Getter(onMethod = @__({@DynamoDbAttribute(FIELD_INVALIDATED_TIMELINE_ELEMENT_IDS)}))
    private List<String> invalidatedTimelineElementIds;

    @Getter(onMethod = @__({@DynamoDbAttribute(FIELD_REASON)}))
    private String reason;

    @Getter(onMethod = @__({@DynamoDbAttribute(FIELD_IDX)}))
    private String idx;

    @Getter(onMethod = @__({@DynamoDbAttribute(FIELD_EXPECTED_STATUS_CODES)}))
    private List<String> expectedStatusCodes;

    @Getter(onMethod = @__({@DynamoDbAttribute(FIELD_EXPECTED_DELIVERY_FAILURE_CAUSE)}))
    private String expectedDeliveryFailureCause;

    @Getter(onMethod = @__({@DynamoDbAttribute(FILED_STATUS)}))
    private String status;

    @Getter(onMethod = @__({@DynamoDbAttribute(FIELD_ERRORS)}))
    private List<NotificationReworksErrorEntity> errors;

    @Getter(onMethod = @__({@DynamoDbAttribute(FIELD_CREATED_AT)}))
    private Instant createdAt;

    @Getter(onMethod = @__({@DynamoDbAttribute(FIELD_UPDATED_AT)}))
    private Instant updatedAt;
}
