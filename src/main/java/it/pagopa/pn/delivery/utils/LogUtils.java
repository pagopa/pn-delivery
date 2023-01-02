package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationAttachmentDownloadMetadataResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotNull;

@Component
public class LogUtils {

    public String getLogMessageForDownloadDocument(NotificationAttachmentDownloadMetadataResponse response) {
        @NotNull String filename = response.getFilename();
        String message = String.format("filename=%s, ", filename);
        String responseUrl = response.getUrl();
        String safeUrl = StringUtils.hasText( responseUrl )? responseUrl.split("\\?")[0] : null;
        String retryAfter = response.getRetryAfter() != null ? response.getRetryAfter().toString() : null;
        if (StringUtils.hasText( safeUrl ) ) {
            message += String.format("url=%s", safeUrl);
        }
        if ( StringUtils.hasText( retryAfter ) ) {
            message += String.format("retryAfter=%s", retryAfter);
        }
        return message;
    }
}
