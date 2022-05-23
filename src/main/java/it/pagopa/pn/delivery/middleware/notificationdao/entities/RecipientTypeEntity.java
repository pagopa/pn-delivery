package it.pagopa.pn.delivery.middleware.notificationdao.entities;

public enum RecipientTypeEntity {
    PF("PF"),
    PG("PG");

    private String value;

    RecipientTypeEntity(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
