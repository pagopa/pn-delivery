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
public class NotificationPhysicalAddressEntity {
    private String at;
    private String address;
    private String addressDetails;
    private String zip;
    private String municipality;
    private String municipalityDetails;
    private String province;
    private String foreignState;

    @DynamoDbAttribute(value = "at")
    public String getAt() {
        return at;
    }

    public void setAt(String at) {
        this.at = at;
    }

    @DynamoDbAttribute(value = "address")
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @DynamoDbAttribute(value = "addressDetails")
    public String getAddressDetails() {
        return addressDetails;
    }

    public void setAddressDetails(String addressDetails) {
        this.addressDetails = addressDetails;
    }

    @DynamoDbAttribute(value = "zip")
    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    @DynamoDbAttribute(value = "municipality")
    public String getMunicipality() {
        return municipality;
    }

    public void setMunicipality(String municipality) {
        this.municipality = municipality;
    }

    @DynamoDbAttribute(value = "municipalityDetails")
    public String getMunicipalityDetails() {
        return municipalityDetails;
    }

    public void setMunicipalityDetails(String municipalityDetails) {
        this.municipalityDetails = municipalityDetails;
    }

    @DynamoDbAttribute(value = "province")
    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    @DynamoDbAttribute(value = "foreignState")
    public String getForeignState() {
        return foreignState;
    }

    public void setForeignState(String foreignState) {
        this.foreignState = foreignState;
    }
}
