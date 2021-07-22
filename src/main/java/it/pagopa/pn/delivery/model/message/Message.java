package it.pagopa.pn.delivery.model.message;

import java.util.Date;

public class Message {

	private String iun;
	private Date sentDate;

	public String getIun() {
		return iun;
	}

	public void setIun(String iun) {
		this.iun = iun;
	}

	public Date getSentDate() {
		return sentDate;
	}

	public void setSentDate(Date sentDate) {
		this.sentDate = sentDate;
	}

}
