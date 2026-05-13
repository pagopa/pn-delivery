const {adjustTimelineAndHistory} = require('./mapperUtils.js');

exports.transformFromV29ToV28 = function (responseV29) {
    console.log("transformFromV29ToV28");

    const CATEGORY_TO_EXCLUDE = [];

    let responseV28 = responseV29;

    const adjustedTimelineAndHistory = adjustTimelineAndHistory(responseV28.timeline, responseV28.notificationStatusHistory, CATEGORY_TO_EXCLUDE, transformTimeline);

    responseV28.timeline = adjustedTimelineAndHistory.timeline;
    responseV28.notificationStatusHistory = adjustedTimelineAndHistory.history;

    delete responseV28.priority;
    return responseV28;
}

function transformTimeline(tl){
    return tl;
}

