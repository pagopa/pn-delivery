package it.pagopa.pn.delivery.models;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatus;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.time.Instant;
import java.util.List;

@EqualsAndHashCode
@ToString
@Getter
public class InputSearchNotificationDto {
    @NotEmpty
    private String senderReceiverId;
    
    @NotNull
    private Instant startDate;
    
    @NotNull
    private Instant endDate;

    private String mandateId;
    
    private String filterId;
    
    private  List<NotificationStatus> statuses;

    private final List<String> groups;
    
    private final String subjectRegExp;

    private final String iunMatch;

    private final boolean receiverIdIsOpaque;

    @Positive
    @NotNull
    private final Integer size;
    
    private final String nextPagesKey;
    
    private final boolean bySender;

    private final boolean isPrivate;

    public InputSearchNotificationDto(String senderReceiverId, Instant startDate, Instant endDate, String mandateId, String filterId,  List<NotificationStatus> statuses,
                                      List<String> groups, String subjectRegExp, String iunMatch, Integer size, String nextPagesKey, boolean bySender) {
        this(senderReceiverId, startDate, endDate, mandateId, filterId, statuses,
                groups, subjectRegExp, iunMatch, size, nextPagesKey, bySender, false, false);
    }


    public InputSearchNotificationDto(String senderReceiverId, Instant startDate, Instant endDate, String mandateId, String filterId, List<NotificationStatus> statuses,
                                      List<String> groups, String subjectRegExp, String iunMatch, Integer size, String nextPagesKey, boolean bySender, boolean receiverIdIsOpaque, boolean isPrivate) {
        this.senderReceiverId = senderReceiverId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.mandateId = mandateId;
        this.filterId = filterId;
        this.statuses = statuses;
        this.groups = groups;
        this.subjectRegExp = subjectRegExp;
        this.iunMatch = iunMatch;
        this.size = size;
        this.nextPagesKey = nextPagesKey;
        this.bySender = bySender;
        this.receiverIdIsOpaque = receiverIdIsOpaque;
        this.isPrivate = isPrivate;
    }

    public String getSenderReceiverId() {
        return senderReceiverId;
    }

    public void setSenderReceiverId(String senderReceiverId) {
        this.senderReceiverId = senderReceiverId;
    }

    public void setStartDate(Instant startDate){
        this.startDate = startDate;
    }

    public void setEndDate(Instant endDate) { this.endDate = endDate; }

    public void setFilterId(String filterId) { this.filterId = filterId; }
    
    public void setStatuses(List<NotificationStatus>  statuses){
        this.statuses = statuses;
    }
    
    public static class Builder
    {
        private String senderReceiverId;
        private Instant startDate;
        private Instant endDate;
        private String mandateId;
        private String filterId;
        private List<NotificationStatus> statuses;
        private String subjectRegExp;
        private String iunMatch;
        private Integer size;
        private String nextPagesKey;
        private boolean bySender;
        private boolean receiverIdIsOpaque;
        private boolean isPrivate;

        private List<String> groups;

        public Builder() {}
        
        public Builder bySender(boolean bySender) {
            this.bySender = bySender;
            return this;
        }

        public Builder senderReceiverId(String senderReceiverId) {
            this.senderReceiverId = senderReceiverId;
            return this;
        }

        public Builder startDate(Instant startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder endDate(Instant endDate) {
            this.endDate = endDate;
            return this;
        }

        public Builder mandateId(String mandateId) {
            this.mandateId = mandateId;
            return this;
        }

        public Builder filterId(String filterId) {
            this.filterId = filterId;
            return this;
        }

        public Builder statuses( List<NotificationStatus> statuses) {
            this.statuses = statuses;
            return this;
        }

        public Builder subjectRegExp(String subjectRegExp) {
            this.subjectRegExp = subjectRegExp;
            return this;
        }

        public Builder iunMatch(String iunMatch) {
            this.iunMatch = iunMatch;
            return this;
        }

        public Builder size(Integer size) {
            this.size = size;
            return this;
        }

        public Builder nextPagesKey(String nextPagesKey) {
            this.nextPagesKey = nextPagesKey;
            return this;
        }

        public Builder groups(List<String> groups) {
            this.groups = groups;
            return this;
        }


        public Builder receiverIdIsOpaque(Boolean receiverIdIsOpaque) {
            this.receiverIdIsOpaque = receiverIdIsOpaque;
            return this;
        }

        public Builder isPrivate(Boolean isPrivate) {
            this.isPrivate = isPrivate;
            return this;
        }
        
        public InputSearchNotificationDto build() {
           return new InputSearchNotificationDto(senderReceiverId, startDate, endDate, mandateId, filterId, statuses,
                   groups, subjectRegExp, iunMatch, size, nextPagesKey, bySender, receiverIdIsOpaque, isPrivate);
        }
        
    }
    
}
