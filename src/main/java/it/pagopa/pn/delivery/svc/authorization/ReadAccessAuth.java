package it.pagopa.pn.delivery.svc.authorization;

import lombok.Data;

import java.util.List;

@Data
public class ReadAccessAuth {
    private CxType cxType;
    private String cxId;
    private String mandateId;

    private List<String> cxGroups;

    private String iun;
    private Integer recipientIdx;

    public static ReadAccessAuth newAccessRequest(String cxType, String cxId, String mandateId, List<String> cxGroups, String iun, Integer recipientIdx) {
        ReadAccessAuth result = new ReadAccessAuth();
        result.setCxType( CxType.valueOf( cxType ) );
        result.setCxId( cxId );
        result.setMandateId( mandateId );
        result.setCxGroups( cxGroups );
        result.setIun( iun );
        result.setRecipientIdx( recipientIdx );
        return result;
    }

}
