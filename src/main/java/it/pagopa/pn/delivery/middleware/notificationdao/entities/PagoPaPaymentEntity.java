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

    private String noticeCode;
    private String creditorTaxId;
    private boolean applyCost;
    private String noticeCodeAlternative;
    private MetadataAttachmentEntity attachment;

    public void setNoticeCode(String noticeCode) {
        this.noticeCode = noticeCode;
    }

    public void setCreditorTaxId(String creditorTaxId) {
        this.creditorTaxId = creditorTaxId;
    }

    public void setApplyCost(boolean applyCost) {
        this.applyCost = applyCost;
    }

    public void setNoticeCodeAlternative(String noticeCodeAlternative) {
        this.noticeCodeAlternative = noticeCodeAlternative;
    }

    public void setAttachment(MetadataAttachmentEntity attachment) {
        this.attachment = attachment;
    }




    @DynamoDbAttribute(value = "noticeCode")
    public String getNoticeCode() {
        return noticeCode;
    }

    @DynamoDbAttribute(value = "noticeCodeAlternative")
    public String getNoticeCodeAlternative() {
        return noticeCodeAlternative;
    }

    @DynamoDbAttribute(value = "creditorTaxId")
    public String getCreditorTaxId() {
        return creditorTaxId;
    }

    @DynamoDbAttribute(value = "applyCost")
    public boolean getApplyCost() {
        return applyCost;
    }

    @DynamoDbAttribute(value = "attachment")
    public MetadataAttachmentEntity getAttachment() {
        return attachment;
    }

}
