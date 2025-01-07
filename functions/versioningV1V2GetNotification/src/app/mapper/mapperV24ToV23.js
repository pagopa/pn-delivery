const {adjustTimelineAndHistory} = require('./mapperUtils.js');

exports.transformFromV24ToV23 = function(responseV24) {
    const CATEGORY_TO_EXCLUDE = []
    let responseV23 = responseV24;
    const adjustedTimelineAndHistory = adjustTimelineAndHistory(responseV23.timeline, responseV23.notificationStatusHistory, CATEGORY_TO_EXCLUDE, transformTimeline)
    
    responseV23.timeline = adjustedTimelineAndHistory.timeline;
    responseV23.notificationStatus = adjustedTimelineAndHistory.history[adjustedTimelineAndHistory.history.length -1].status;

    return responseV23;
}

function transformTimeline(tl){
    // in 2.4 sono stati aggiunti: ingestionTimestamp, eventTimestamp e notificationSentAt
    if( tl.ingestionTimestamp || tl.notificationSentAt || tl.eventTimestamp ) {
        console.log("transformTimeline - rm ingestionTimestamp, eventTimestamp e notificationSentAt")
        delete tl.notificationSentAt;
        delete tl.ingestionTimestamp;
        delete tl.eventTimestamp;
    }
    return tl;
}