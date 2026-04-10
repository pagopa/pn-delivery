package it.pagopa.pn.delivery.svc.authorization;

import lombok.Data;

import java.util.List;
import java.util.Objects;

@Data
public class ReadAccessAuth {
    private CxType cxType;
    private String cxId;
    private String mandateId;

    private List<String> cxGroups;

    private String iun;
    private Integer recipientIdx;
    private ReadAccessAction action;

    public static ReadAccessAuth newAccessRequest(String cxType, String cxId, String mandateId, List<String> cxGroups, String iun, Integer recipientIdx, ReadAccessAction action) {
        Objects.requireNonNull(action, "ReadAccessAuth action must not be null");
        ReadAccessAuth result = new ReadAccessAuth();
        result.setCxType( CxType.valueOf( cxType ) );
        result.setCxId( cxId );
        result.setMandateId( mandateId );
        result.setCxGroups( cxGroups );
        result.setIun( iun );
        result.setRecipientIdx( recipientIdx );
        result.setAction( action );
        return result;
    }

}
