const {adjustTimelineAndHistory} = require('./mapperUtils.js');

exports.transformFromV24ToV23 = function(responseV24) {
    const CATEGORY_TO_EXCLUDE = []
    let responseV23 = responseV24;
    const adjustedTimelineAndHistory = adjustTimelineAndHistory(responseV23.timeline, responseV23.notificationStatusHistory, CATEGORY_TO_EXCLUDE, transformTimeline)
    
    responseV23.timeline = adjustedTimelineAndHistory.timeline;
    return responseV23;
}

function transformTimeline(tl){
    // in 2.4 sono stati aggiunti: ingestionTimestamp e notificationSentAt
    if( tl.ingestionTimestamp || tl.notificationSentAt ) {
        console.log("transformTimeline - rm ingestionTimestamp e notificationSentAt")
        delete tl.notificationSentAt;
        delete tl.ingestionTimestamp;
    }
    return tl;
}