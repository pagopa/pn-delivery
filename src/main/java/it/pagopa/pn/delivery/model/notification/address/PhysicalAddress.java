package it.pagopa.pn.delivery.model.notification.address;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;

@Data
@Builder
public class PhysicalAddress extends ArrayList<String> {

    public PhysicalAddress() {
        super(6);
    }
}
