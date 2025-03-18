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