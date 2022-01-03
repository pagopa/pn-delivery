package it.pagopa.pn.delivery.svc;


import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.preload.PreloadRequest;
import it.pagopa.pn.api.dto.preload.PreloadResponse;
import it.pagopa.pn.commons.configs.aws.AwsConfigs;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class S3PresignedUrlService {

    public static final String PRELOAD_URL_SECRET_HEADER = "secret";

    private final AwsConfigs props;
    private final PnDeliveryConfigs cfgs;
    private final AttachmentService attachmentService;
    private final S3Presigner presigner;

    public S3PresignedUrlService(AwsConfigs props, PnDeliveryConfigs cfgs, AttachmentService attachmentService ) {
        this.props = props;
        this.cfgs = cfgs;
        this.attachmentService = attachmentService;
        this.presigner = buildPresigner();
    }

    protected S3Presigner buildPresigner( ) {
        S3Presigner.Builder builder = S3Presigner.builder();
        if( props != null ) {

            String profileName = props.getProfileName();
            if( StringUtils.isNotBlank( profileName ) ) {
                builder.credentialsProvider( ProfileCredentialsProvider.create( profileName ));
            }

            String regionCode = props.getRegionCode();
            if( StringUtils.isNotBlank( regionCode )) {
                builder.region( Region.of( regionCode ));
            }

            String endpointUrl = props.getEndpointUrl();
            if( StringUtils.isNotBlank( endpointUrl )) {
                builder.endpointOverride( URI.create( endpointUrl ));
            }

        }
        return builder.build();
    }

    public List<PreloadResponse> presignedUpload(String paId, List<PreloadRequest> requestList ) {
        List<PreloadResponse> preloadResponseList = new ArrayList<>( requestList.size() );

        for ( PreloadRequest request : requestList ) {
            String key = request.getKey();
            String contentType = request.getContentType();
            preloadResponseList.add( presignedUpload(paId, key, contentType) );
        }
        return preloadResponseList;
    }


    public PreloadResponse presignedUpload(String paId, String key, String contentType ) {
        log.debug( "Presigned upload file for paId={} key={} contentType={}", paId, key, contentType );
        Duration urlDuration = cfgs.getPreloadUrlDuration();
        String fullKey = attachmentService.buildPreloadFullKey( paId, key );
        String secret = UUID.randomUUID().toString();

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(props.getBucketName() )
                .key( fullKey )
                .contentType( contentType )
                .metadata(Collections.singletonMap( PRELOAD_URL_SECRET_HEADER, secret))
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration( urlDuration )
                .putObjectRequest(objectRequest)
                .build();
        log.debug( "Put presigned object START" );
        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);
        log.debug( "Put presigned object END" );
        String httpMethodForUpload = presignedRequest.httpRequest().method().toString();
        String urlForUpload = presignedRequest.url().toString();

        return PreloadResponse.builder()
                .url( urlForUpload )
                .httpMethod( httpMethodForUpload )
                .secret( secret )
                .key( key )
                .build();
    }

    public PreloadResponse presignedDownload( String name, NotificationAttachment attachment ) {
        Duration urlDuration = cfgs.getDownloadUrlDuration();
        String secret = UUID.randomUUID().toString();
        log.debug( "Retrieve extension for attachment with name={}", name );
        String extension = getExtension( attachment );

        String fullName = name + "." + extension;

        NotificationAttachment.Ref attachmentRef = attachment.getRef();
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(props.getBucketName() )
                .key( attachmentRef.getKey() )
                .versionId( attachmentRef.getVersionToken() )
                .responseCacheControl("no-store, max-age=0")
                .responseContentDisposition("attachment; filename=\"" + fullName + "\"")
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration( urlDuration )
                .getObjectRequest(objectRequest)
                .build();
        log.debug( "GET presigned object START" );
        PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
        log.debug( "GET presigned object END" );

        String httpMethodForDownload = presignedRequest.httpRequest().method().toString();
        String urlForDownload = presignedRequest.url().toString();

        return PreloadResponse.builder()
                .url( urlForDownload )
                .httpMethod( httpMethodForDownload )
                .secret( secret )
                .key( null )
                .build();
    }

    private String getExtension(NotificationAttachment attachment) {
        String extension;
        try {
            MediaType contentType = MediaType.parseMediaType( attachment.getContentType() );
            extension = contentType.getSubtype();
        } catch (InvalidMediaTypeException e) {
            log.error( "Error parsing media type for attachment=" + attachment.getRef().toString());
            throw new PnInternalException( "Error parsing media type for attachment="
                    + attachment.getRef().toString(), e);
        }
        return extension;
    }

}
