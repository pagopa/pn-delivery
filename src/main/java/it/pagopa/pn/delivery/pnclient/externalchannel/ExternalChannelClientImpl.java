package it.pagopa.pn.delivery.pnclient.externalchannel;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class ExternalChannelClientImpl implements ExternalChannelClient{

    public static final String EXTERNAL_CHANNEL_GET_DOWNLOAD_LINKS = "/attachments/getDownloadLinks?%s";

    private final PnDeliveryConfigs cfg;
    private final RestTemplate restTemplate;


    public ExternalChannelClientImpl(PnDeliveryConfigs cfg, @Qualifier("withTracing") RestTemplate restTemplate) {
        this.cfg = cfg;
        this.restTemplate = restTemplate;
    }

    @Override
    public String[] getResponseAttachmentUrl(String[] attachmentIds) {

        String queryString = Arrays.asList( attachmentIds ).stream()
                .map( el ->  "attachmentKey=" + el)
                .collect(Collectors.joining( "&" ));
        final String baseUrl = cfg.getExternalChannelBaseUrl() + EXTERNAL_CHANNEL_GET_DOWNLOAD_LINKS;
        String url = String.format(baseUrl, queryString);
        return restTemplate.getForObject(url, String[].class);
    }

}
