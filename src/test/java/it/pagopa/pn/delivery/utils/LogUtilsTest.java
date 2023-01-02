package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationAttachmentDownloadMetadataResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class LogUtilsTest {

    private LogUtils logUtils;

    private static final String FILENAME = "filename";
    private static final String URL = "http://fakedownloadurl?token=fakeToken";

    @BeforeEach
    void setup() { this.logUtils = new LogUtils(); }

    @Test
    void getMessageWithSafeUrl() {
        NotificationAttachmentDownloadMetadataResponse response = NotificationAttachmentDownloadMetadataResponse.builder()
                .filename( FILENAME )
                .url( URL )
                .build();
        String messageResult = logUtils.getLogMessageForDownloadDocument( response );

        Assertions.assertNotNull( messageResult );
        Assertions.assertEquals( "filename=filename, url=http://fakedownloadurl", messageResult );
    }

    @Test
    void getMessageWithRetryAfter() {
        NotificationAttachmentDownloadMetadataResponse response = NotificationAttachmentDownloadMetadataResponse.builder()
                .filename( FILENAME )
                .retryAfter( 3600 )
                .build();
        String messageResult = logUtils.getLogMessageForDownloadDocument( response );

        Assertions.assertNotNull( messageResult );
        Assertions.assertEquals( "filename=filename, retryAfter=3600", messageResult );
    }

}
