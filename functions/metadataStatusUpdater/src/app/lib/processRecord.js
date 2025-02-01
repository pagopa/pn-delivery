const utils = require("./utils");
const { unmarshall } = require("@aws-sdk/util-dynamodb");
const putNotificationMetadata = require("./putNotificationMetadata");
const dynamo = require("./dynamo.js");
const { ItemNotFoundException } = require("./exceptions.js");

const processRecord = async (record) => {
  const kinesisData = utils.decodePayload(record.kinesis.data);
  if (utils.shouldSkipEvaluation(kinesisData)) {
    console.log(`Skipping evaluation for record with 
      eventSource: ${kinesisData.eventSource}, 
      eventName: ${kinesisData.eventName}, 
      tableName: ${kinesisData.tableName}, 
      NewImage: ${kinesisData.NewImage}
    `);
    return;
  } 

  const timelineElement = unmarshall(kinesisData.dynamodb.NewImage);
  const { iun, statusInfo, timelineElementId } = timelineElement;

  const notification = await dynamo.getItem("pn-Notifications", { iun });

  const actualStatus = statusInfo.actual;

  console.log(
    `Processing record for notification ${iun} with status ${actualStatus} and timelineId ${timelineElementId}`
  );

  let acceptedAt = statusInfo.statusChangeTimestamp;
  switch (actualStatus) {
    case "ACCEPTED":
      await putNotificationMetadata.putNotificationMetadata(
        statusInfo,
        notification,
        acceptedAt
      );
      break;
    case "REFUSED":
      await deletePayments(notification);
      break;
    default:
      await handleDefaultStatus(notification, statusInfo);
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

const handleDefaultStatus = async (notification, statusInfo) => {
  try {
    const acceptedAt = await retrieveAcceptedAtFromPersistedMetadata(
      notification
    );
    await putNotificationMetadata.putNotificationMetadata(
      statusInfo,
      notification,
      acceptedAt
    );
  } catch (error) {
    if (error instanceof ItemNotFoundException) {
      console.warn(
        `Unable to retrieve accepted date - iun=${notification.iun} recipientId=${notification.recipients[0].recipientId}`
      );
      return;
    }
    throw error;
  }
};

const retrieveAcceptedAtFromPersistedMetadata = async (notification) => {
  const iun_recipientId = `${notification.iun}##${notification.recipients[0].recipientId}`;
  const sentAt = notification.sentAt;
  const notificationMetadata = await dynamo.getItem(
    "pn-NotificationsMetadata",
    {
      iun_recipientId,
      sentAt,
    }
  );
  return notificationMetadata.tableRow.acceptedAt;
};

module.exports = { processRecord };
