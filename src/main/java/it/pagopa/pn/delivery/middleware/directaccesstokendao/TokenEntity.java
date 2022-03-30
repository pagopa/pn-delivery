package it.pagopa.pn.delivery.middleware.directaccesstokendao;


import lombok.*;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@DynamoDbBean
public class TokenEntity {
    public static final String FIELD_TOKEN = "token";
    public static final String FIELD_TAXID = "taxId";
    public static final String FIELD_IUN = "iun";

    private String token;
    private String taxId;
    private String iun;

    @DynamoDbPartitionKey
    @DynamoDbAttribute(value = FIELD_TOKEN )
    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }

    @DynamoDbAttribute(value = FIELD_TAXID )
    public String getTaxId() {
        return taxId;
    }
    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    @DynamoDbAttribute(value = FIELD_IUN )
    public String getIun() {
        return iun;
    }
    public void setIun(String iun) {
        this.iun = iun;
    }
}
