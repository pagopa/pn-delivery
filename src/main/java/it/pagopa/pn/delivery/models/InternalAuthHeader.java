package it.pagopa.pn.delivery.models;

import java.util.List;

public record InternalAuthHeader(String cxType, String xPagopaPnCxId, String xPagopaPnUid, List<String> xPagopaPnCxGroups) {
}
