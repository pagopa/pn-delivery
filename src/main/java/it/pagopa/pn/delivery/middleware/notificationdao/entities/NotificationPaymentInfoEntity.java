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
    @Builder.Default
    private Boolean applyCost = true;
    private PagoPaPaymentEntity pagoPaForm;
    private F24PaymentEntity f24;

    @DynamoDbAttribute(value = "applyCost")
    public Boolean isApplyCost() {
        return applyCost;
    }

    public void setApplyCost(Boolean applyCost) {
        this.applyCost = applyCost;
    }

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
    public PagoPaPaymentEntity getPagoPaForm() {
        return pagoPaForm;
    }

    public void setPagoPaForm(PagoPaPaymentEntity pagoPaForm) {
        this.pagoPaForm = pagoPaForm;
    }

    @DynamoDbAttribute(value = "F24")
    public F24PaymentEntity getF24() {
        return f24;
    }

    public void setF24(F24PaymentEntity f24) {
        this.f24 = f24;
    }



}
