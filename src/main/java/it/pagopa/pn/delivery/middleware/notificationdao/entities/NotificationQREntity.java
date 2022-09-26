package it.pagopa.pn.delivery.middleware.notificationdao.entities;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@DynamoDbBean
public class NotificationQREntity {

    public static final String FIELD_RECIPIENT_TYPE = "recipientType";
    public static final String FIELD_RECIPIENT_INTERNAL_ID = "recipientId";
    public static final String FIELD_AAR_QR_CODE_VALUE = "aarQRCodeValue";
    public static final String FIELD_IUN = "iun";

    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute(FIELD_AAR_QR_CODE_VALUE)})) private String aarQRCodeValue;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_IUN)})) private String iun;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_RECIPIENT_TYPE)})) private RecipientTypeEntity recipientType;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_RECIPIENT_INTERNAL_ID)})) private String recipientId;
}
