package it.pagopa.pn.delivery.models;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Data
@DynamoDbBean
public class NotificationLang {
    private String lang;
}
