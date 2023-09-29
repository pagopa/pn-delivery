package it.pagopa.pn.delivery.middleware.notificationdao.entities;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@DynamoDbBean
public class NotificationAttachmentBodyRefEntity {
    public static final String FIELD_KEY = "key";
    public static final String FIELD_VERSION_TOKEN = "versionToken";

    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_KEY)})) private String key;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_VERSION_TOKEN)})) private String versionToken;
}
