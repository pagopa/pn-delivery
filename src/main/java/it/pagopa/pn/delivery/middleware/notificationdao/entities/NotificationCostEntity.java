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
public class NotificationCostEntity {

    public static final String FIELD_CREDITOR_TAX_ID_NOTICE_CODE = "creditorTaxId_noticeCode";
    private static final String FIELD_IUN = "iun";
    private static final String FIELD_RECIPIENT_IDX = "recipientIdx";

    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute(FIELD_CREDITOR_TAX_ID_NOTICE_CODE)})) private String creditorTaxIdNoticeCode;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_IUN)})) private String iun;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_RECIPIENT_IDX)})) private int recipientIdx;
}
