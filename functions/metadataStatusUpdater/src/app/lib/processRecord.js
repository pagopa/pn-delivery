const utils = require("./utils");
const { unmarshall } = require("@aws-sdk/util-dynamodb");
const putNotificationMetadata = require("./putNotificationMetadata");
const dynamo = require("./dynamo.js");

const processRecord = async (record) => {
  const kinesisData = utils.decodePayload(record.kinesis.data);

  const timelineElement = unmarshall(kinesisData.dynamodb.NewImage);
  const { iun, statusInfo, timelineElementId } = timelineElement;

  const notification = await dynamo.getItem("pn-Notifications", { iun });

  const actualStatus = statusInfo.actual;

  console.log(
    `Processing record for notification ${iun} with status ${actualStatus} and timelineId ${timelineElementId}`
  );

  switch (actualStatus) {
    case "REFUSED":
      await deletePayments(notification);
      break;
    default:
      await putNotificationMetadata.putNotificationMetadata(
        statusInfo,
        notification
      );
      break;
  }
};

const deletePayments = async (notification) => {
  for (const recipient of notification.recipients) {
    for (const payment of recipient.payments) {
      if(payment.creditorTaxId && payment.noticeCode) {
        const creditorTaxId_noticeCode = `${payment.creditorTaxId}##${payment.noticeCode}`;
        console.log(
          `Deleting payment ${creditorTaxId_noticeCode} for recipient with id: ${recipient.recipientId}`
        );
        await dynamo.deleteItem("pn-NotificationsCost", {
          creditorTaxId_noticeCode,
        });
      }
    }
  }
};

module.exports = { processRecord };
