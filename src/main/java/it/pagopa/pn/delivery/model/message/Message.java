package it.pagopa.pn.delivery.model.message;

import java.time.Instant;

public class Message {

	public enum Type {
		TYPE1, TYPE2
	}

	private String iun;
	private Instant sentDate;
	private Type messageType;

	public String getIun() {
		return iun;
	}

	public void setIun(String iun) {
		this.iun = iun;
	}

	public Instant getSentDate() {
		return sentDate;
	}

	public void setSentDate(Instant sentDate) {
		this.sentDate = sentDate;
	}

	public Type getMessageType() {
		return messageType;
	}

	public void setMessageType(Type messageType) {
		this.messageType = messageType;
	}

}
