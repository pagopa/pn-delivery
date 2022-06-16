package it.pagopa.pn.delivery.middleware.notificationdao.entities;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@DynamoDbBean
public class NotificationCostEntity {

    public static final String FIELD_CREDITOR_TAX_ID_NOTICE_CODE = "creditorTaxId_noticeCode";
    private static final String FIELD_IUN = "iun";
    private static final String FIELD_RECIPIENT_IDX = "recipientIdx";

    private String creditorTaxId_noticeCode;
    private String iun;
    private int recipientIdx;


    @DynamoDbPartitionKey
    @DynamoDbAttribute(value = FIELD_CREDITOR_TAX_ID_NOTICE_CODE)
    public String getCreditorTaxId_noticeCode() { return creditorTaxId_noticeCode; }

    public void setCreditorTaxId_noticeCode(String creditorTaxId_noticeCode) {
        this.creditorTaxId_noticeCode = creditorTaxId_noticeCode;
    }

    @DynamoDbAttribute(value = FIELD_IUN )
    public String getIun() { return iun; }

    public void setIun( String iun ) {
        this.iun = iun;
    }

    @DynamoDbAttribute(value = FIELD_RECIPIENT_IDX)
    public int getRecipientIdx() { return recipientIdx; }

    public void setRecipientIdx( int recipientIdx ) {
        this.recipientIdx = recipientIdx;
    }
}
