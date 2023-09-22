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
public class F24PaymentEntity {

    private String title;
    private boolean applyCost;
    private Integer index;
    private MetadataAttachmentEntity metadataAttachment;


    @DynamoDbAttribute(value = "title")
    public String getTitle() {
        return title;
    }

    @DynamoDbAttribute(value = "applyCost")
    public boolean getApplyCost() {
        return applyCost;
    }

    @DynamoDbAttribute(value = "index")
    public Integer getIndex() {
        return index;
    }

    @DynamoDbAttribute(value = "metadataAttachment")
    public MetadataAttachmentEntity getMetadataAttachment() {
        return metadataAttachment;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setApplyCost(boolean applyCost) {
        this.applyCost = applyCost;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public void setMetadataAttachment(MetadataAttachmentEntity metadataAttachment) {
        this.metadataAttachment = metadataAttachment;
    }
}
