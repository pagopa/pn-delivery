package it.pagopa.pn.delivery.rest.mapper;

import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.MessageRequestDto;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.MessageResponseDto;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.LocalizedContent;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewMessageRequest;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.MessageResponse;

public class InformalMessageMapper {
    private InformalMessageMapper() {}

    public static MessageRequestDto toMs(NewMessageRequest api, String senderId) {
        if (api == null) return null;
        MessageRequestDto ms = new MessageRequestDto();
        ms.setSenderId(senderId);
        ms.setPrimaryContent(toMs(api.getPrimaryMessage()));
        ms.setSecondaryContent(toMs(api.getAdditionalMessage()));
        return ms;
    }

    public static MessageResponse toApi(MessageResponseDto ms) {
        if (ms == null) return null;
        MessageResponse api = new MessageResponse();
        api.setMessageId(ms.getMessageId());
        api.setPrimaryMessage(toApi(ms.getPrimaryContent()));
        api.setAdditionalMessage(toApi(ms.getSecondaryContent()));
        return api;
    }

    public static it.pagopa.pn.delivery.generated.openapi.server.v1.dto.LocalizedContent toApi(LocalizedContent ms) {
        if (ms == null) return null;
        it.pagopa.pn.delivery.generated.openapi.server.v1.dto.LocalizedContent api = new it.pagopa.pn.delivery.generated.openapi.server.v1.dto.LocalizedContent();
        api.setSubject(ms.getSubject());
        api.setLongBody(ms.getLongBody());
        api.setShortBody(ms.getShortBody());
        api.setLanguage(ms.getLanguage().getValue());
        return api;
    }

    public static LocalizedContent toMs(it.pagopa.pn.delivery.generated.openapi.server.v1.dto.LocalizedContent api) {
        if (api == null) return null;
        LocalizedContent ms = new LocalizedContent();
        ms.setSubject(api.getSubject());
        ms.setLongBody(api.getLongBody());
        ms.setShortBody(api.getShortBody());
        ms.setLanguage(LocalizedContent.LanguageEnum.fromValue(api.getLanguage()));
        return ms;
    }
}
