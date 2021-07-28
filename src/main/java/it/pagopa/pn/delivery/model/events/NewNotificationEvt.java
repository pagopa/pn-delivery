package it.pagopa.pn.delivery.model.events;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class NewNotificationEvt {

	public enum Type {
		TYPE1, TYPE2
	}

	private String iun;
	private Instant sentDate;
	private Type messageType;

}
