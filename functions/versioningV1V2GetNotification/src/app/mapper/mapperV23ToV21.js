const {adjustTimelineAndHistory} = require('./mapperUtils.js');

exports.transformFromV23ToV21 = function(responseV23) {
    const CATEGORY_TO_EXCLUDE = [
        "NOTIFICATION_RADD_RETRIEVED"
    ]
    let responseV21 = responseV23;
    responseV21.vat = undefined;
    const adjustedTimelineAndHistory = adjustTimelineAndHistory(responseV21.timeline, responseV21.notificationStatusHistory, CATEGORY_TO_EXCLUDE, transformTimeline)
    
    responseV21.timeline = adjustedTimelineAndHistory.timeline;
    responseV21.notificationStatusHistory = adjustedTimelineAndHistory.history;
    return responseV21;
}

function transformTimeline(tl){
    if (tl.category == "SEND_ANALOG_PROGRESS")
    {
        console.log("transformTimeline - " + tl.category);
        // in 2.3 è stato aggiunto nel detail la property serviceLevel, che va rimossa
        delete tl.details.serviceLevel;
    }
    else if (tl.category == "NOTIFICATION_VIEWED"
    || tl.category == "REFINEMENT"
    || tl.category == "SEND_DIGITAL_PROGRESS"
    || tl.category == "PAYMENT")
    {
        console.log("transformTimeline - " + tl.category);
        // in 2.3 è stato aggiunto nel detail la property eventTimestamp, che va rimossa
        delete tl.details.eventTimestamp;
    }
    else if (tl.category == "SCHEDULE_ANALOG_WORKFLOW"
    || tl.category == "SCHEDULE_DIGITAL_WORKFLOW")
    {
        console.log("transformTimeline - " + tl.category);
        // in 2.3 è stato aggiunto nel detail la property schedulingDate, che va rimossa
        delete tl.details.schedulingDate;
    }
    return tl;
}