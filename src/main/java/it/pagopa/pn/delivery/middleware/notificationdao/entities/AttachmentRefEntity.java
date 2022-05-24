package it.pagopa.pn.delivery.middleware.notificationdao.entities;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@DynamoDbBean
public class AttachmentRefEntity {
    private String key;
    private String versionToken;

    @DynamoDbAttribute(value = "key")
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @DynamoDbAttribute(value = "versionToken")
    public String getVersionToken() {
        return versionToken;
    }

    public void setVersionToken(String versionToken) {
        this.versionToken = versionToken;
    }
}
