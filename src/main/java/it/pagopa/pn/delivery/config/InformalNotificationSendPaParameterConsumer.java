package it.pagopa.pn.delivery.config;

import io.micrometer.core.instrument.util.StringUtils;
import it.pagopa.pn.commons.abstractions.ParameterConsumer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
public class InformalNotificationSendPaParameterConsumer {

    @Value("${pn.delivery.features.is-send-informal-active-default-value}")
    private Boolean isSendInformalActiveDefaultValue;

    private final Map<String, Boolean> cxIdIsInformalActiveMap = new HashMap<>();
    private final ParameterConsumer parameterConsumer;

    private static final String PARAMETER_STORE_MAP_CXID_IS_INFORMAL = "InformalNotificationSendPaWhiteList";

    public InformalNotificationSendPaParameterConsumer(ParameterConsumer parameterConsumer) {
        this.parameterConsumer = parameterConsumer;
    }

    public Boolean isSenderActiveForInformalNotification(String cxId) {
        log.debug("Start isSendActive for cxId={}", cxId);

        Boolean isInformalActive = cxIdIsInformalActiveMap.get(cxId);
        if (isInformalActive != null) {
            log.debug("cxId={} isInformalActive={}", cxId, isInformalActive);
            return isInformalActive;
        }

        log.debug("cxId={} configuration not found, isSendInformalActiveDefaultValue={}", cxId, isSendInformalActiveDefaultValue);
        return isSendInformalActiveDefaultValue;
    }

    @PostConstruct
    protected void initialize() {
        Optional<CxIdIsInformalActive[]> optionalCxIdIsInformalActives =
                parameterConsumer.getParameterValue(PARAMETER_STORE_MAP_CXID_IS_INFORMAL, CxIdIsInformalActive[].class);

        if (optionalCxIdIsInformalActives.isPresent()) {
            for (CxIdIsInformalActive cxIdIsInformalActive : optionalCxIdIsInformalActives.get()) {
                addToMapIsInformalId(cxIdIsInformalActive);
            }
            log.info("Loaded informal sender whitelist in memory with {} entries", cxIdIsInformalActiveMap.size());
        } else {
            log.info("No informal sender whitelist found. Using default value={}", isSendInformalActiveDefaultValue);
        }
    }

    private void addToMapIsInformalId(CxIdIsInformalActive cxIdIsInformalActive) {
        if (Objects.isNull(cxIdIsInformalActive)
                || StringUtils.isBlank(cxIdIsInformalActive.getCxId())
                || Objects.isNull(cxIdIsInformalActive.getIsActive())) {
            log.warn("Invalid informal whitelist entry: {}", cxIdIsInformalActive);
            return;
        }
        cxIdIsInformalActiveMap.put(cxIdIsInformalActive.getCxId(), cxIdIsInformalActive.getIsActive());
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class CxIdIsInformalActive {
        private String cxId;
        private Boolean isActive;
    }
}
