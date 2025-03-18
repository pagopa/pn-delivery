package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@AllArgsConstructor
public class FeatureFlagUtils {
    private final PnDeliveryConfigs pnDeliveryConfigs;

    public boolean isPhysicalAddressLookupEnabled() {
        if(pnDeliveryConfigs.getPhysicalAddressLookupStartDate() == null) {
            return false;
        }

        Instant now = Instant.now();
        return pnDeliveryConfigs.getPhysicalAddressLookupStartDate().compareTo(now) <= 0;
    }
}
