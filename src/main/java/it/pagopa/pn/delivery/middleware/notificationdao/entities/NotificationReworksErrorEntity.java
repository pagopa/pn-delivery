package it.pagopa.pn.delivery.middleware.notificationdao.entities;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Data
@DynamoDbBean
public class NotificationReworksErrorEntity {
    public static final String FIELD_CAUSE = "cause";
    public static final String FIELD_DESCRIPTION = "description";

    @Getter(onMethod = @__({@DynamoDbAttribute(FIELD_CAUSE)}))
    private String cause;

    @Getter(onMethod = @__({@DynamoDbAttribute(FIELD_DESCRIPTION)}))
    private String description;
}
