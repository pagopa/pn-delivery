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
public class NotificationPaymentInfoEntity {

    private String noticeCode;
    private String creditorTaxId;
    private String noticeCode_optional;
    private PaymentAttachmentEntity pagoPaForm;
    private PaymentAttachmentEntity f24flatRate;
    private PaymentAttachmentEntity f24standard;

    @DynamoDbAttribute(value = "noticeCode")
    public String getNoticeCode() {
        return noticeCode;
    }

    public void setNoticeCode(String noticeCode) {
        this.noticeCode = noticeCode;
    }

    @DynamoDbAttribute(value = "creditorTaxId")
    public String getCreditorTaxId() {
        return creditorTaxId;
    }

    public void setCreditorTaxId(String creditorTaxId) {
        this.creditorTaxId = creditorTaxId;
    }

    @DynamoDbAttribute(value = "noticeCode_optional")
    public String getNoticeCode_optional() {
        return noticeCode_optional;
    }

    public void setNoticeCode_optional(String noticeCode_optional) {
        this.noticeCode_optional = noticeCode_optional;
    }


    @DynamoDbAttribute(value = "pagoPaForm")
    public PaymentAttachmentEntity getPagoPaForm() {
        return pagoPaForm;
    }

    public void setPagoPaForm(PaymentAttachmentEntity pagoPaForm) {
        this.pagoPaForm = pagoPaForm;
    }

    @DynamoDbAttribute(value = "f24flatRate")
    public PaymentAttachmentEntity getF24flatRate() {
        return f24flatRate;
    }

    public void setF24flatRate(PaymentAttachmentEntity f24flatRate) {
        this.f24flatRate = f24flatRate;
    }

    @DynamoDbAttribute(value = "f24standard")
    public PaymentAttachmentEntity getF24standard() {
        return f24standard;
    }

    public void setF24standard(PaymentAttachmentEntity f24standard) {
        this.f24standard = f24standard;
    }

}
