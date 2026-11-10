package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.exception.PnDeliveryGroupsUnavailableException;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaGroup;
import it.pagopa.pn.delivery.pnclient.externalregistries.PnExternalRegistriesClientImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaGroupService {

    private final PnExternalRegistriesClientImpl pnExternalRegistriesClient;

    public List<PaGroup> getGroupsForLabelization(String senderId) {
        try {
            log.info("Getting groups for senderId {}", senderId);
            return pnExternalRegistriesClient.getGroups(senderId, false);
        } catch (PnDeliveryGroupsUnavailableException exc) {
            log.warn("Could not get groups for senderId {}", senderId);
            return Collections.emptyList();
        }
    }
}