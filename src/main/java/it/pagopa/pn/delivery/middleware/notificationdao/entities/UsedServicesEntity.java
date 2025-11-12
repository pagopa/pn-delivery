package it.pagopa.pn.delivery.middleware.notificationdao.entities;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Data
@ToString
@DynamoDbBean
public class UsedServicesEntity {
    public static final String COL_PHYSICAL_ADDRESS_LOOKUP = "physicalAddressLookup";

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_PHYSICAL_ADDRESS_LOOKUP)})) private Boolean physicalAddressLookup;
}
