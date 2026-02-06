const {adjustTimelineAndHistory} = require('./mapperUtils.js');

exports.transformFromV28ToV27 = function (responseV28) {
    console.log("transformFromV28ToV27");

    const CATEGORY_TO_EXCLUDE = ["NOTIFICATION_TIMELINE_REWORKED"];

    let responseV27 = responseV28;

    const adjustedTimelineAndHistory = adjustTimelineAndHistory(responseV27.timeline, responseV27.notificationStatusHistory, CATEGORY_TO_EXCLUDE, transformTimeline);

    responseV27.timeline = adjustedTimelineAndHistory.timeline;
    responseV27.notificationStatusHistory = adjustedTimelineAndHistory.history;
    return responseV27;
}

function transformTimeline(tl){
    /* 
        Nella versione 2.8 è stato rinominato il campo "legalfactId" in "legalFactId" per le category:
        COMPLETELY_UNREACHABLE_CREATION_REQUEST
        DIGITAL_DELIVERY_CREATION_REQUEST
        NOTIFICATION_VIEWED_CREATION_REQUEST
        Si decide di restituire per tutte le versioni precedenti il campo "legalfactId", in modo da non creare problemi di compatibilità.
    */
    if ((tl.category === 'COMPLETELY_UNREACHABLE_CREATION_REQUEST'
        || tl.category === 'DIGITAL_DELIVERY_CREATION_REQUEST'
        || tl.category === 'NOTIFICATION_VIEWED_CREATION_REQUEST'
        ) && tl.details != null) {
        tl.details.legalfactId = tl.details.legalFactId;
        delete tl.details.legalFactId;
    }
    return tl;
}