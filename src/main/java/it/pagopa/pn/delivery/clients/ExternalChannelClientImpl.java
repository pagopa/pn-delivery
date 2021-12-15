package it.pagopa.pn.delivery.clients;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class ExternalChannelClientImpl implements ExternalChannelClient{

    public static final String EXTERNAL_CHANNEL_GET_DOWNLOAD_LINKS = "/attachments/getDownloadLinks?%s";

    private final PnDeliveryConfigs cfg;

    public ExternalChannelClientImpl(PnDeliveryConfigs cfg) {
        this.cfg = cfg;
    }

    @Override
    public String[] getResponseAttachmentUrl(String[] attachmentIds) {

        String queryString = Arrays.asList( attachmentIds ).stream()
                .map( el ->  "attachmentKey=" + el)
                .collect(Collectors.joining( "&" ));
        RestTemplate template = new RestTemplate();
        final String baseUrl = cfg.getExternalChannelBaseUrl() + EXTERNAL_CHANNEL_GET_DOWNLOAD_LINKS;
        String url = String.format(baseUrl, queryString);
        return template.getForObject(url, String[].class);
    }
}
