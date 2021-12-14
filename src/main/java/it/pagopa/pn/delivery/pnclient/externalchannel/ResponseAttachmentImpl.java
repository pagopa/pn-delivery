package it.pagopa.pn.delivery.pnclient.externalchannel;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

public class ResponseAttachmentImpl implements ResponseAttachment {

    public static final String EXTERNAL_CHANNEL_GET_DOWNLOAD_LINKS = "/attachments/getDownloadLinks?attachmentKey=%s";

    private final PnDeliveryConfigs cfg;
    private final RestTemplate restTemplate;


    public ResponseAttachmentImpl(PnDeliveryConfigs cfg, @Qualifier("withTracing") RestTemplate restTemplate) {
        this.cfg = cfg;
        this.restTemplate = restTemplate;
    }


    @Override
    public String[] getMessageResponseAttachmentUrl(String[] attachmentId) {
        RestTemplate template = this.restTemplate;

        final String baseUrl = cfg.getExternalChannelBaseUrl() + EXTERNAL_CHANNEL_GET_DOWNLOAD_LINKS;
        String url = String.format(baseUrl, String.join(",", Arrays.asList(attachmentId)));

        return template.getForObject(url, String[].class);
    }
}
