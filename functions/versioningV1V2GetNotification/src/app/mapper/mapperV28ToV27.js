const {adjustTimelineAndHistory} = require('./mapperUtils.js');

exports.transformFromV28ToV27 = function (responseV28) {
    console.log("transformFromV28ToV27");

    const CATEGORY_TO_EXCLUDE = ["NOTIFICATION_TIMELINE_REWORKED"];

    let responseV27 = responseV28;

    const adjustedTimelineAndHistory = adjustTimelineAndHistory(responseV27.timeline, responseV27.notificationStatusHistory, CATEGORY_TO_EXCLUDE, null);

    responseV27.timeline = adjustedTimelineAndHistory.timeline;
    responseV27.notificationStatusHistory = adjustedTimelineAndHistory.history;
    return responseV27;
}