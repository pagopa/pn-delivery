package it.pagopa.pn.delivery.clients;

public interface ExternalChannelClient {

    String[] getResponseAttachmentUrl( String[] attachmentIds );
}
