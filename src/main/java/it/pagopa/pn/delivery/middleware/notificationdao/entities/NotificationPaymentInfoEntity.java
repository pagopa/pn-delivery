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
    private PagoPaPaymentEntity pagoPaForm;
    private F24PaymentEntity f24;

    @DynamoDbAttribute(value = "f24")
    public F24PaymentEntity getF24() {
        return f24;
    }

    public void setF24(F24PaymentEntity f24) {
        this.f24 = f24;
    }

    @DynamoDbAttribute(value = "pagoPaForm")
    public PagoPaPaymentEntity getPagoPaForm() {
        return pagoPaForm;
    }

    public void setPagoPaForm(PagoPaPaymentEntity pagoPaForm) {
        this.pagoPaForm = pagoPaForm;
    }
}
