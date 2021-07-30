package it.pagopa.pn.delivery.model.notification.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NotificationResponse {

	private String iun;
	private String paNotificationId;

	public NotificationResponse(String iun, String paNotificationId) {
		super();
		this.iun = iun;
		this.paNotificationId = paNotificationId;
	}

}
