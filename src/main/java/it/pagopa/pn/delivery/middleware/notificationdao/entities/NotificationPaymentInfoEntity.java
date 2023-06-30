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
    private PaymentAttachmentEntity pagoPaForm;

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

    @DynamoDbAttribute(value = "pagoPaForm")
    public PaymentAttachmentEntity getPagoPaForm() {
        return pagoPaForm;
    }

    public void setPagoPaForm(PaymentAttachmentEntity pagoPaForm) {
        this.pagoPaForm = pagoPaForm;
    }
}
