package it.pagopa.pn.delivery.middleware.notificationdao.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Getter
@Setter
@DynamoDbBean
public class DocumentAttachmentEntity {

    public static final String COL_CONTENT_TYPE = "contentType";
    public static final String COL_DIGESTS = "digests";
    public static final String COL_REF = "ref";
    public static final String COL_TITLE = "title";
    public static final String COL_REQUIREACK = "requireAck";
    public static final String COL_SENDBYMAIL = "sendByMail";

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_CONTENT_TYPE)})) private String contentType;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_DIGESTS)})) private AttachmentDigestsEntity digests;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_REF)})) private AttachmentRefEntity ref;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_TITLE)})) private String title;

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_REQUIREACK)})) private Boolean requiresAck;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_SENDBYMAIL)})) private Boolean sendByMail;
}
