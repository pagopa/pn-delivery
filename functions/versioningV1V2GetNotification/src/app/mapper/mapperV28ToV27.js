const {adjustTimelineAndHistory} = require('./mapperUtils.js');

exports.transformFromV28ToV27 = function (responseV28) {
    console.log("transformFromV28ToV27");

    const CATEGORY_TO_EXCLUDE = ["SEND_ANALOG_TIMEOUT", "ANALOG_FAILURE_WORKFLOW_TIMEOUT"];

    let responseV27 = responseV28;

    const adjustedTimelineAndHistory = adjustTimelineAndHistory(responseV27.timeline, responseV27.notificationStatusHistory, CATEGORY_TO_EXCLUDE, transformTimeline, transformHistory);

    responseV27.timeline = adjustedTimelineAndHistory.timeline;
    responseV27.notificationStatusHistory = adjustedTimelineAndHistory.history;
    // Settiamo l'ultimo stato della history come stato finale, necessario in caso i filtri applicati sulla history abbiano rimosso gli ultimi stati
    responseV27.notificationStatus = adjustedTimelineAndHistory.history[adjustedTimelineAndHistory.history.length - 1].status;

    return responseV27;
}

function transformTimeline(tl){
    return tl;
}

function transformHistory(timelineIds, adjustedHistory){
    return adjustedHistory.filter(nsh => nsh.status !== "DELIVERY_TIMEOUT");
}
