package it.pagopa.pn.delivery.middleware.notificationdao.entities;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.time.Instant;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Data
@DynamoDbBean
public class NotificationPaymentInfoEntity {
    public static final String FIELD_NOTICE_CODE = "noticeCode";
    public static final String FIELD_CREDITOR_TAX_ID = "creditorTaxId";
    public static final String FIELD_APPLY_COST = "applyCost";
    public static final String FIELD_PAGO_PA_FORM = "pagoPaForm";
    public static final String FIELD_F24 = "f24";
    public static final String FIELD_AMOUNT = "amount";
    public static final String FIELD_DUE_DATE = "dueDate";

    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_NOTICE_CODE)})) private String noticeCode;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_CREDITOR_TAX_ID)})) private String creditorTaxId;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_APPLY_COST)})) private Boolean applyCost;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_AMOUNT)})) private Integer amount;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DUE_DATE)})) private Instant dueDate;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_PAGO_PA_FORM)})) private PagoPaPaymentEntity pagoPaForm;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_F24)})) private F24PaymentEntity f24;

}
