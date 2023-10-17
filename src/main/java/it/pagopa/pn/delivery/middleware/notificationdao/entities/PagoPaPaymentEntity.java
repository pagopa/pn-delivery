package it.pagopa.pn.delivery.middleware.notificationdao.entities;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Data
@DynamoDbBean
public class PagoPaPaymentEntity {

    public static final String FIELD_DIGESTS = "digests";
    public static final String FIELD_CONTENT_TYPE = "contentType";
    public static final String FIELD_REF = "ref";

    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DIGESTS)})) private NotificationAttachmentDigestsEntity digests;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_CONTENT_TYPE)})) private String contentType;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_REF)})) private NotificationAttachmentBodyRefEntity ref;

}
