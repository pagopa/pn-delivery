package it.pagopa.pn.delivery.middleware.notificationdao.entities;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Data
@ToString
@DynamoDbBean
public class NotificationAttachmentDigestsEntity {
    public static final String FIELD_SHA_256 = "sha256";

    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_SHA_256)})) private String sha256;
}
