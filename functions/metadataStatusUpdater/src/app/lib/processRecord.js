const utils = require('./utils');
const { unmarshall } = require("@aws-sdk/util-dynamodb");
const putNotificationMetadata = require('./putNotificationMetadata');
const dynamo = require('./dynamo.js');
const { ItemNotFoundException } = require("./exceptions.js");

const processRecord = async (record) => {
  const kinesisData = utils.decodePayload(record.kinesis.data);

  if(utils.shouldSkipEvaluation(kinesisData)){
    return;
  }

  const timelineElement = unmarshall(kinesisData.dynamodb.NewImage);
  const iun = timelineElement.iun;

  const notification = await dynamo.getItem('pn-Notifications', { iun });

  const statusInfo = timelineElement.statusInfo;
  const actualStatus = statusInfo.actual;

  console.log(`Processing record for notification ${iun} with status ${actualStatus} and timelineId ${timelineElement.timelineElementId}`);

  if (actualStatus === 'ACCEPTED') {
    const acceptedAt = statusInfo.statusChangeTimestamp;
    await putNotificationMetadata.putNotificationMetadata(statusInfo, notification, acceptedAt);
  } else if (actualStatus === 'REFUSED') {
    await deletePayments(notification);
  } else {
    let notificationMetadata;
    const iun_recipientId = `${notification.iun}##${notification.recipients[0].recipientId }`;
    const sentAt = notification.sentAt;
    try {
      notificationMetadata = await dynamo.getItem('pn-NotificationsMetadata', {  iun_recipientId, sentAt });
    } catch (error) {
      if (error instanceof ItemNotFoundException) {
        console.warn( `Unable to retrieve accepted date - iun=${notification.iun} recipientId=${notification.recipients[0].recipientId}`);
        return;
      }
      throw error;
    }
    const acceptedAt = notificationMetadata.tableRow.acceptedAt;
    await putNotificationMetadata.putNotificationMetadata(statusInfo, notification, acceptedAt);
  }
};

const deletePayments = async (notification) => {
  for (const recipient of notification.recipients) {
    for (const payment of recipient.payments) {
      const creditorTaxId_noticeCode = `${payment.creditorTaxId}##${payment.noticeCode}`;   
       await dynamo.deleteItem('pn-NotificationsCost', {creditorTaxId_noticeCode });
    }
  }
}

module.exports = { processRecord };