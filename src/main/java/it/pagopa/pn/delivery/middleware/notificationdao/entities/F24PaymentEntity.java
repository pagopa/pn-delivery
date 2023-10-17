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
public class F24PaymentEntity {
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_APPLY_COST = "applyCost";
    public static final String FIELD_METADATA_ATTACHMENT = "metadataAttachment";

    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_TITLE)})) private String title;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_APPLY_COST)})) private Boolean applyCost;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_METADATA_ATTACHMENT)})) private MetadataAttachmentEntity metadataAttachment;
}
