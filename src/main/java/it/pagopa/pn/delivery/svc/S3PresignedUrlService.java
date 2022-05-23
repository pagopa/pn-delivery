package it.pagopa.pn.delivery.svc;





import it.pagopa.pn.api.dto.preload.PreloadResponse;
import it.pagopa.pn.commons.configs.aws.AwsConfigs;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
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

    public List<PreLoadResponse> presignedUpload(String paId, List<PreLoadRequest> requestList ) {
        List<PreLoadResponse> preloadResponseList = new ArrayList<>( requestList.size() );

        for ( PreLoadRequest request : requestList ) {
            String key = request.getPreloadIdx();
            String contentType = request.getContentType();
            preloadResponseList.add( presignedUpload(paId, key, contentType) );
        }
        return preloadResponseList;
    }


    public PreLoadResponse presignedUpload(String paId, String key, String contentType ) {
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

        return new PreLoadResponse()
                .url( urlForUpload )
                .httpMethod( PreLoadResponse.HttpMethodEnum.fromValue(httpMethodForUpload) )
                .secret( secret )
                .key( key );
    }

    public String presignedDownload( String name, NotificationDocument attachment ) {
        Duration urlDuration = cfgs.getDownloadUrlDuration();
        log.debug( "Retrieve extension for attachment with name={}", name );

        NotificationAttachmentBodyRef attachmentRef = attachment.getRef();
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(props.getBucketName() )
                .key( attachmentRef.getKey() )
                .versionId( attachmentRef.getVersionToken() )
                .responseCacheControl("no-store, max-age=0")
                .responseContentDisposition("attachment; filename=\"" + name + "\"")
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration( urlDuration )
                .getObjectRequest(objectRequest)
                .build();
        log.debug( "GET presigned object START" );
        PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
        log.debug( "GET presigned object END" );

        return  presignedRequest.url().toString();
    }

}
