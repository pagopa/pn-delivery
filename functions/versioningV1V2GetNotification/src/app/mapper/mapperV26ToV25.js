const {adjustTimelineAndHistory} = require('./mapperUtils.js');
const {DeceasedWorkflowException} = require("../exceptions.js");

exports.transformFromV26ToV25 = function (responseV26) {
    console.log("transformFromV26ToV25");

    if(process.env.ENABLE_DECEASED_WORKFLOW == "false"){
        console.log("Deceased workflow not enabled");
        throw new DeceasedWorkflowException("Deceased workflow not enabled");
    }

    const CATEGORY_TO_EXCLUDE = ["ANALOG_WORKFLOW_RECIPIENT_DECEASED"]
    let responseV25 = responseV26;

    const adjustedTimelineAndHistory = adjustTimelineAndHistory(responseV25.timeline, responseV25.notificationStatusHistory, CATEGORY_TO_EXCLUDE, transformTimeline, transformHistory)
    
    responseV25.timeline = adjustedTimelineAndHistory.timeline;
    responseV25.notificationStatusHistory = adjustedTimelineAndHistory.history;
    responseV25.notificationStatus = adjustedTimelineAndHistory.history[adjustedTimelineAndHistory.history.length -1].status;

    return responseV25;
}

function transformTimeline(tl){
    return tl;
}

function transformHistory(timelineIds, adjustedHistory){
    return adjustedHistory.filter(nsh => nsh.status !== "RETURNED_TO_SENDER");
}