package it.pagopa.pn.delivery.middleware.notificationdao.entities;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@DynamoDbBean
public class NotificationDigitalAddressEntity {
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_ADDRESS = "address";

    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_TYPE)})) private DigitalAddressTypeEntity type;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_ADDRESS)})) private String address;
}
