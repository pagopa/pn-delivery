package it.pagopa.pn.delivery.rest.mapper;

import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.MessageRequestDto;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.MessageResponseDto;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.LocalizedContent;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewMessageRequest;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.MessageResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewMessageRequestAdditionalMessage;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewMessageRequestPrimaryMessage;

public class InformalMessageMapper {
    private InformalMessageMapper() {}

    public static MessageRequestDto toMs(NewMessageRequest api, String senderId) {
        if (api == null) return null;
        MessageRequestDto ms = new MessageRequestDto();
        ms.setSenderId(senderId);
        ms.setPrimaryContent(toMsPrimaryMessage(api.getPrimaryMessage()));
        ms.setSecondaryContent(toMsAdditionalMessage(api.getAdditionalMessage()));
        return ms;
    }

    public static MessageResponse toApi(MessageResponseDto ms) {
        if (ms == null) return null;
        MessageResponse api = new MessageResponse();
        api.setMessageId(ms.getMessageId());
        api.setPrimaryMessage(toApiPrimaryMessage(ms.getPrimaryContent()));
        api.setAdditionalMessage(toApiAdditionalMessage(ms.getSecondaryContent()));
        api.setCreatedAt(ms.getCreatedAt());
        return api;
    }

    public static NewMessageRequestPrimaryMessage toApiPrimaryMessage(LocalizedContent ms) {
        if (ms == null) return null;
        NewMessageRequestPrimaryMessage api = new NewMessageRequestPrimaryMessage();
        api.setSubject(ms.getSubject());
        api.setLongBody(ms.getLongBody());
        api.setShortBody(ms.getShortBody());
        api.setLanguage(ms.getLanguage().getValue());
        return api;
    }

    public static NewMessageRequestAdditionalMessage toApiAdditionalMessage(LocalizedContent ms) {
        if (ms == null) return null;
        NewMessageRequestAdditionalMessage api = new NewMessageRequestAdditionalMessage();
        api.setSubject(ms.getSubject());
        api.setLongBody(ms.getLongBody());
        api.setShortBody(ms.getShortBody());
        api.setLanguage(ms.getLanguage().getValue());
        return api;
    }

    public static LocalizedContent toMsPrimaryMessage(NewMessageRequestPrimaryMessage api) {
        if (api == null) return null;
        LocalizedContent ms = new LocalizedContent();
        ms.setSubject(api.getSubject());
        ms.setLongBody(api.getLongBody());
        ms.setShortBody(api.getShortBody());
        ms.setLanguage(LocalizedContent.LanguageEnum.fromValue(api.getLanguage()));
        return ms;
    }

    public static LocalizedContent toMsAdditionalMessage(NewMessageRequestAdditionalMessage api) {
        if (api == null) return null;
        LocalizedContent ms = new LocalizedContent();
        ms.setSubject(api.getSubject());
        ms.setLongBody(api.getLongBody());
        ms.setShortBody(api.getShortBody());
        ms.setLanguage(LocalizedContent.LanguageEnum.fromValue(api.getLanguage()));
        return ms;
    }
}
