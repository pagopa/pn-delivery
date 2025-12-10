const logger = require("./logger");

const HEADERS_TO_RETRIEVE = [
    "x-pagopa-lollipop-assertion-ref",
    "x-pagopa-lollipop-assertion-type",
    "x-pagopa-lollipop-auth-jwt",
    "x-pagopa-lollipop-original-method",
    "x-pagopa-lollipop-original-url",
    "x-pagopa-lollipop-public-key",
    "x-pagopa-lollipop-user-id",
    "x-pagopa-pn-src-ch",
    "signature",
    "signature-input"
];

function retrieveHeadersToForward(headers) {
    return Object.fromEntries(
        Object.entries(headers).filter(([key]) => HEADERS_TO_RETRIEVE.includes(key))
    );
}

function getUserInfoFromEvent(event) {
  logger.info("Trying to get user info from event...");
  const requestContext = event.requestContext || {};
  const authorizer = requestContext.authorizer || {};
  const headers = event.headers || {};
  const cxId = authorizer["cx_id"];
  const cxType = authorizer["cx_type"];
  const taxId = headers["x-pagopa-cx-taxid"];
  checkRequiredUserInfo([
    { name: "cxId", value: cxId },
    { name: "cxType", value: cxType },
    { name: "taxId", value: taxId }
  ]);
  const uid = removeCxPrefix(cxId);
  return { uid, cxType, cxId, taxId };
}

function checkRequiredUserInfo(userInfoData) {
  let missingFields = "";
  for(const field of userInfoData) {
    if(!field.value){
      missingFields += field.name + " ";
    }
  }
  if (missingFields !== "") {
    throw new Error("Missing required info: " + missingFields);
  }
}

function removeCxPrefix(cxId) {
  return cxId.replace(/^(PF|PG|PA)-/, "");
}


exports.getUserInfoFromEvent = getUserInfoFromEvent;
exports.retrieveHeadersToForward = retrieveHeadersToForward;
exports.removeCxPrefix = removeCxPrefix;