package it.pagopa.pn.delivery.middleware.directaccesstokendao;


import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@DynamoDbBean
public class TokenEntity {
    public static final String FIELD_TOKENID = "tokenId";
    public static final String FIELD_TAXID = "taxId";
    public static final String FIELD_IUN = "iun";
    public static final String INDEX_IUN_NAME = "iunIndex";

    private String tokenId;
    private String taxId;
    private String iun;
    
    @DynamoDbPartitionKey
    @DynamoDbAttribute(value = FIELD_TOKENID )
    public String getTokenId() {
        return tokenId;
    }
    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    @DynamoDbAttribute(value = FIELD_TAXID )
    public String getTaxId() {
        return taxId;
    }
    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = { INDEX_IUN_NAME })
    @DynamoDbAttribute(value = FIELD_IUN )
    public String getIun() {
        return iun;
    }
    public void setIun(String iun) {
        this.iun = iun;
    }
}
