package it.pagopa.pn.delivery.config;

import it.pagopa.pn.commons.abstractions.ParameterConsumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Configuration
@AllArgsConstructor
public class PhysicalAddressLookupParameterConsumer {

    private final ParameterConsumer parameterConsumer;

    private static final String PARAMETER_NAME = "PaActiveForPhysicalAddressLookup";

    public List<String> getActivePAsForPhysicalAddressLookup() {
        log.debug("Start getActivePAsForPhysicalAddressLookup");

        Optional<String[]> optionalActivePasForPhysicalAddressLookup = parameterConsumer.getParameterValue(
                PARAMETER_NAME, String[].class);
        if( optionalActivePasForPhysicalAddressLookup.isPresent() ) {
            log.info("Active PA for physical address lookup found, paIds={}", Arrays.toString(optionalActivePasForPhysicalAddressLookup.get()));
            return Arrays.stream(optionalActivePasForPhysicalAddressLookup.get()).toList();
        }

        log.debug("There are not active PAs for physicalAddressLookup");
        return Collections.emptyList();
    }
}
