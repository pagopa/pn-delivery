package it.pagopa.pn.delivery.middleware.notificationdao.entities;

public enum DigitalAddressTypeEntity {
    PEC("PEC");

    private String value;

    DigitalAddressTypeEntity(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
