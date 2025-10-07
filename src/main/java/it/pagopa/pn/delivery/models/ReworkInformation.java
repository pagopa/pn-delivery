package it.pagopa.pn.delivery.models;

import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.ReworkError;
import it.pagopa.pn.delivery.generated.openapi.server.rework.v1.dto.ReworkRequest;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationReworksEntity;
import it.pagopa.pn.delivery.models.internal.notification.NotificationDocument;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;

@Data
@Builder
public class ReworkInformation {
    String reworkId;
    ReworkRequest request;
    String iun;
    ArrayList<ReworkError> errors;
    NotificationReworksEntity previousReworkEntity;
    InternalNotification notification;
    NotificationDocument currentDoc;
}