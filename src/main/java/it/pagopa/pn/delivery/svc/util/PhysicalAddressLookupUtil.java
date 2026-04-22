package it.pagopa.pn.delivery.svc.util;

import it.pagopa.pn.delivery.config.PhysicalAddressLookupParameterConsumer;
import it.pagopa.pn.delivery.utils.FeatureFlagUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class PhysicalAddressLookupUtil {
    private final PhysicalAddressLookupParameterConsumer physicalAddressLookupParameter;
    private final FeatureFlagUtils featureFlagUtils;

    public boolean checkPhysicalAddressLookupIsEnabled (String paId){
        List<String> activePAsForPhysicalAddressLookup = physicalAddressLookupParameter.getActivePAsForPhysicalAddressLookup();

        boolean isActive = featureFlagUtils.isPhysicalAddressLookupEnabled() &&
                (activePAsForPhysicalAddressLookup.isEmpty() ||
                        activePAsForPhysicalAddressLookup.contains(paId));

        log.debug("Physical address lookup is {} for paId: {}", isActive ? "enabled" : "disabled", paId);
        return isActive;
    }
}
