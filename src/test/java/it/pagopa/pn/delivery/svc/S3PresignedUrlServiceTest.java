package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.preload.PreloadRequest;
import it.pagopa.pn.api.dto.preload.PreloadResponse;
import it.pagopa.pn.commons.configs.aws.AwsConfigs;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.util.Base64Utils;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class S3PresignedUrlServiceTest {
    public static final String ATTACHMENT_BODY_STR = "Body";
    public static final String KEY = "KEY";
    public static final String BASE64_BODY = Base64Utils.encodeToString(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8));
    public static final String SHA256_BODY = DigestUtils.sha256Hex(ATTACHMENT_BODY_STR);
    public static final String VERSION_TOKEN = "VERSION_TOKEN";
    public static final NotificationDocument NOTIFICATION_ATTACHMENT = NotificationDocument.builder()
            //.body(BASE64_BODY)
            .contentType("application/pdf")
            .digests(NotificationAttachmentDigests.builder()
                    .sha256(SHA256_BODY)
                    .build()
            )
            .ref( NotificationAttachmentBodyRef.builder()
                    .key( KEY )
                    .versionToken( VERSION_TOKEN )
                    .build() )
            .build();
    public static final NotificationDocument NOTIFICATION_ATTACHMENT_FAIL_CONTENT = NotificationDocument.builder()
            //.body(BASE64_BODY)
            .contentType("asd")
            .digests(NotificationAttachmentDigests.builder()
                    .sha256(SHA256_BODY)
                    .build()
            )
            .ref( NotificationAttachmentBodyRef.builder()
                    .key( KEY )
                    .versionToken( VERSION_TOKEN )
                    .build() )
            .build();
    
    private static final String PA_ID = "PA_ID";
    private static final String BUCKET_NAME = "bucket";
    private static final String FILE_NAME = "FILE_NAME";
    private AwsConfigs awsConfigs;
    private PnDeliveryConfigs cfg;
    private AttachmentService attachmentService;
    private S3Presigner presigner;
    private S3PresignedUrlService service;
    

    @BeforeEach
    void setup() {
        awsConfigs = new AwsConfigs();
        awsConfigs.setBucketName(BUCKET_NAME);
        cfg = Mockito.mock( PnDeliveryConfigs.class );
        attachmentService = Mockito.mock( AttachmentService.class );
        presigner = Mockito.mock( S3Presigner.class );
        
        service = new S3PresignedUrlService( awsConfigs, cfg, attachmentService ) {

            @Override
            protected S3Presigner buildPresigner() {
                return presigner;
            }
        };
    }
    
    @Test
    void presignedUploadSuccess() {
        //Given
        List<PreLoadRequest> preloadRequests = new ArrayList<>();
        preloadRequests.add( PreLoadRequest.builder()
                        .preloadIdx( KEY )
                        .build());
        
        //When
        Mockito.when( attachmentService.buildPreloadFullKey( Mockito.anyString(), Mockito.anyString() ))
                .thenReturn( "preload/" + PA_ID + "/" + KEY );
        Mockito.when( presigner.presignPutObject( Mockito.any(PutObjectPresignRequest.class)))
                .thenReturn(PresignedPutObjectRequest.builder()
                        .expiration( Instant.MAX )
                        .isBrowserExecutable( true )
                        .httpRequest( SdkHttpRequest.builder()
                                .protocol( "http" )
                                .host( "host" )
                                .method( SdkHttpMethod.POST )
                                .build() )
                        .signedHeaders( Map.of( "k1", Collections.singletonList("v1")) )
                        .build());
        List<PreLoadResponse> preloadResponses = service.presignedUpload( PA_ID, preloadRequests );
        
        //Then
        assertNotNull(preloadResponses.get(0).getUrl());
        assertNotNull(preloadResponses.get(0).getSecret());
        assertEquals( KEY, preloadResponses.get( 0 ).getKey() );
    }

    @Test
    void presignedDownloadSuccess() {
        //When
        Mockito.when( presigner.presignGetObject( Mockito.any(GetObjectPresignRequest.class)) )
                .thenReturn(PresignedGetObjectRequest.builder()
                        .expiration( Instant.MAX )
                        .isBrowserExecutable( true )
                        .httpRequest( SdkHttpRequest.builder()
                                .protocol( "http" )
                                .host( "host" )
                                .method( SdkHttpMethod.POST )
                                .build() )
                        .signedHeaders( Map.of( "k1", Collections.singletonList("v1")) )
                        .build());
        PreLoadResponse response = service.presignedDownload( FILE_NAME, NOTIFICATION_ATTACHMENT );
        
        //Then
        assertNotNull( response.getUrl() );
        assertNotNull( response.getHttpMethod() );
        assertNotNull( response.getSecret() );
    }

    @Test
    void presignedDownloadFailure() {
        //When
        Mockito.when( presigner.presignGetObject( Mockito.any(GetObjectPresignRequest.class)) )
                .thenReturn(PresignedGetObjectRequest.builder()
                        .expiration( Instant.MAX )
                        .isBrowserExecutable( true )
                        .httpRequest( SdkHttpRequest.builder()
                                .protocol( "http" )
                                .host( "host" )
                                .method( SdkHttpMethod.POST )
                                .build() )
                        .signedHeaders( Map.of( "k1", Collections.singletonList("v1")) )
                        .build());
        Executable todo = () -> service.presignedDownload( FILE_NAME, NOTIFICATION_ATTACHMENT_FAIL_CONTENT );

        //Then
        assertThrows( PnInternalException.class, todo );

    }
}
