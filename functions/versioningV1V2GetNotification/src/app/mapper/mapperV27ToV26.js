const {adjustTimelineAndHistory} = require('./mapperUtils.js');

exports.transformFromV27ToV26 = function (responseV27) {
    console.log("transformFromV27ToV26");

    const CATEGORY_TO_EXCLUDE = ["PUBLIC_REGISTRY_VALIDATION_CALL", "PUBLIC_REGISTRY_VALIDATION_RESPONSE"];

    let responseV26 = responseV27;

    const adjustedTimelineAndHistory = adjustTimelineAndHistory(responseV26.timeline, responseV26.notificationStatusHistory, CATEGORY_TO_EXCLUDE)
    
    responseV26.timeline = adjustedTimelineAndHistory.timeline;
    responseV26.notificationStatusHistory = adjustedTimelineAndHistory.history;

    delete responseV26.usedServices;
    return responseV26;
}

function transformTimeline(tl){
// nella versione 2.7 sono stati aggiunti i seguenti campi per la category REQUEST_ACCEPTED: notificationRequestId, paProtocolNumber, idempotenceToken
    if (tl.category === 'REQUEST_ACCEPTED' && tl.details != null) {
        delete tl.details.notificationRequestId;
        delete tl.details.paProtocolNumber;
        delete tl.details.idempotenceToken;
    }
    return tl;
}