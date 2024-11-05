const {adjustTimelineAndHistory} = require('./mapperUtils.js');

exports.transformFromV25ToV24 = function (responseV25) {
    console.log("transformFromV25ToV24");

    const CATEGORY_TO_EXCLUDE = []
    let responseV24 = responseV25;

    const adjustedTimelineAndHistory = adjustTimelineAndHistory(responseV24.timeline, responseV24.notificationStatusHistory, CATEGORY_TO_EXCLUDE, transformTimeline)

    // eliminazione additionalLanguages per tutte le api con versione precedente all’ultima
    responseV24.additionalLanguages = undefined;

    responseV24.timeline = adjustedTimelineAndHistory.timeline;
    return responseV24;
}

function transformTimeline(tl){
    if (tl.category === 'NOTIFICATION_CANCELLED') {
        tl.legalFactsIds = [];
    }
    return tl;
}