package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
@AllArgsConstructor
public class FeatureFlagUtils {
    private final PnDeliveryConfigs pnDeliveryConfigs;

    public boolean isPhysicalAddressLookupEnabled() {
        if(pnDeliveryConfigs.getPhysicalAddressLookupStartDate() == null) {
            log.error("The parameter PhysicalAddressLookupStartDate is not configured, feature is always deactivated.");
            return false;
        }

        Instant now = Instant.now();
        return pnDeliveryConfigs.getPhysicalAddressLookupStartDate().compareTo(now) <= 0;
    }
}
