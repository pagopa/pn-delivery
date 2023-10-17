package it.pagopa.pn.delivery.middleware.notificationdao.entities;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnoreNulls;

import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Data
@DynamoDbBean
public class NotificationRecipientEntity {
    public static final String FIELD_RECIPIENT_TYPE = "recipientType";
    public static final String FIELD_RECIPIENT_ID = "recipientId";
    public static final String FIELD_DENOMINATION = "denomination";
    public static final String FIELD_DIGITAL_DOMICILE = "digitalDomicile";
    public static final String FIELD_PHYSICAL_ADDRESS = "physicalAddress";
    public static final String FIELD_PAYMENTS = "payments";

    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_RECIPIENT_TYPE)})) private RecipientTypeEntity recipientType;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_RECIPIENT_ID)})) private String recipientId;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DENOMINATION)})) private String denomination;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DIGITAL_DOMICILE)})) private NotificationDigitalAddressEntity digitalDomicile;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_PHYSICAL_ADDRESS)})) private NotificationPhysicalAddressEntity physicalAddress;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_PAYMENTS),  @DynamoDbIgnoreNulls})) private List<NotificationPaymentInfoEntity> payments;

}
