package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.CxTypeAuthFleet;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Slf4j
@NoArgsConstructor(access = AccessLevel.NONE)
public class PgUtils {

    public static boolean checkAuthorizationPG(String recipientType, List<String> cxGroups) {
        if (CxTypeAuthFleet.valueOf(recipientType) == CxTypeAuthFleet.PG && !CollectionUtils.isEmpty(cxGroups)) {
            log.warn("only a PG admin/operator without groups can access this resource");
            return true;
        }
        log.debug("access granted for {}, groups: {}", recipientType, cxGroups);
        return false;
    }
}
