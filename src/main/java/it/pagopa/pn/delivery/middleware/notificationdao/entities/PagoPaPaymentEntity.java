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
public class PagoPaPaymentEntity {

    private NotificationAttachmentDigestsEntity digests;
    private String contentType;
    private NotificationAttachmentBodyRefEntity ref;

    @DynamoDbAttribute(value = "ref")
    public NotificationAttachmentBodyRefEntity getNotificationAttachmentBodyRefEntity() {
        return ref;
    }

    public void setNotificationAttachmentBodyRefEntity(NotificationAttachmentBodyRefEntity ref) {
        this.ref = ref;
    }

    @DynamoDbAttribute(value = "contentType")
    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @DynamoDbAttribute(value = "digests")
    public NotificationAttachmentDigestsEntity getNotificationAttachmentDigestsEntity() {
        return digests;
    }

    public void setNotificationAttachmentDigestsEntity(NotificationAttachmentDigestsEntity digests) {
        this.digests = digests;
    }

}
