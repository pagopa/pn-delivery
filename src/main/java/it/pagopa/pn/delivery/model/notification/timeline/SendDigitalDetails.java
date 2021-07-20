package it.pagopa.pn.delivery.model.notification.timeline;

import it.pagopa.pn.delivery.model.notification.address.DigitalAddress;

public class SendDigitalDetails {

    private String fc;
    private DigitalAddress address;
    private Integer n;
    private DownstreamId downstreamId;

    public String getFc() {
        return fc;
    }

    public void setFc(String fc) {
        this.fc = fc;
    }

    public DigitalAddress getAddress() {
        return address;
    }

    public void setAddress(DigitalAddress address) {
        this.address = address;
    }

    public Integer getN() {
        return n;
    }

    public void setN(Integer n) {
        this.n = n;
    }

    public DownstreamId getDownstreamId() {
        return downstreamId;
    }

    public void setDownstreamId(DownstreamId downstreamId) {
        this.downstreamId = downstreamId;
    }

}
