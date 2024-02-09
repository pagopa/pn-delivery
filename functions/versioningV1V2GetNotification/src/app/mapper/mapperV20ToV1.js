const {adjustTimelineAndHistory} = require('./mapperUtils.js');

exports.transformFromV20ToV1 = function (responseV20) {
  const CATEGORY_TO_EXCLUDE = [
    "NOTIFICATION_CANCELLATION_REQUEST",
    "NOTIFICATION_CANCELLED",
    "PREPARE_ANALOG_DOMICILE_FAILURE"
  ]
  
  // eliminare elementi timeline annullamento
  let responseV1 = responseV20;
  const adjustedTimelineAndHistory = adjustTimelineAndHistory(responseV1.timeline, responseV1.notificationStatusHistory, CATEGORY_TO_EXCLUDE, null)
  
  responseV1.timeline = adjustedTimelineAndHistory.timeline;
  responseV1.notificationStatusHistory = adjustedTimelineAndHistory.history;
  
  return responseV1;
}