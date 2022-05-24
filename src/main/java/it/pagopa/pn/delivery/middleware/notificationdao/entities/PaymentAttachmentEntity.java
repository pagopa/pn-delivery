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
public class PaymentAttachmentEntity {
    private String contentType;
    private AttachmentDigestsEntity digests;
    private AttachmentRefEntity ref;

    @DynamoDbAttribute(value = "contentType")
    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @DynamoDbAttribute(value = "digests")
    public AttachmentDigestsEntity getDigests() {
        return digests;
    }

    public void setDigests(AttachmentDigestsEntity digests) {
        this.digests = digests;
    }

    @DynamoDbAttribute(value = "ref")
    public AttachmentRefEntity getRef() {
        return ref;
    }

    public void setRef(AttachmentRefEntity ref) {
        this.ref = ref;
    }
}
