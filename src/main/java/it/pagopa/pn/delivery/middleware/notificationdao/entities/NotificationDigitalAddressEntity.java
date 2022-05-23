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
public class NotificationDigitalAddressEntity {
    private DigitalAddressTypeEntity type;
    private String address;

    @DynamoDbAttribute(value = "type")
    public DigitalAddressTypeEntity getType() {
        return type;
    }

    public void setType(DigitalAddressTypeEntity type) {
        this.type = type;
    }

    @DynamoDbAttribute(value = "address")
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
