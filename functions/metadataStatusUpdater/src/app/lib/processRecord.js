const utils = require("./utils");
const { unmarshall } = require("@aws-sdk/util-dynamodb");
const putNotificationMetadata = require("./putNotificationMetadata");
const dynamo = require("./dynamo.js");

const processRecord = async (record) => {
  const sequenceNumber = record.kinesis?.sequenceNumber;
  console.log(`[metadataStatusUpdater] Decoding Kinesis record: sequenceNumber=${sequenceNumber}`);
  const kinesisData = utils.decodePayload(record.kinesis.data);

  const timelineElement = unmarshall(kinesisData.dynamodb.NewImage);
  const { iun, statusInfo, timelineElementId } = timelineElement;
  console.log(`[metadataStatusUpdater] Timeline event decoded: sequenceNumber=${sequenceNumber}, iun=${iun}, timelineElementId=${timelineElementId}, statusChanged=${statusInfo?.statusChanged}, status=${statusInfo?.actual}`);

  console.log(`[metadataStatusUpdater] Loading notification: iun=${iun}`);
  const notification = await dynamo.getItem("pn-Notifications", { iun });
  console.log(`[metadataStatusUpdater] Notification loaded: iun=${iun}, communicationType=${notification.communicationType ?? "LEGACY"}, recipientCount=${notification.recipients?.length ?? 0}`);

  const actualStatus = statusInfo.actual;

  console.log(
    `Processing record for notification ${iun} with status ${actualStatus} and timelineId ${timelineElementId}`
  );

  switch (actualStatus) {
    case "REFUSED":
      console.log(`[metadataStatusUpdater] Handling REFUSED status: iun=${iun}`);
      await deletePayments(notification);
      break;
    default:
      console.log(`[metadataStatusUpdater] Updating notification metadata: iun=${iun}, status=${actualStatus}`);
      await putNotificationMetadata.putNotificationMetadata(
        statusInfo,
        notification
      );
      break;
  }
  console.log(`[metadataStatusUpdater] Timeline event handled: iun=${iun}, status=${actualStatus}`);
};

const deletePayments = async (notification) => {
  let deletedPayments = 0;
  for (const recipient of notification.recipients) {
    for (const payment of recipient.payments ?? []) {
      if(payment.creditorTaxId && payment.noticeCode) {
        const creditorTaxId_noticeCode = `${payment.creditorTaxId}##${payment.noticeCode}`;
        console.log(
          `Deleting payment ${creditorTaxId_noticeCode} for recipient with id: ${recipient.recipientId}`
        );
        await dynamo.deleteItem("pn-NotificationsCost", {
          creditorTaxId_noticeCode,
        }, notification.iun);
        deletedPayments += 1;
      }
    }
  }
  console.log(`[metadataStatusUpdater] Payment cleanup completed: iun=${notification.iun}, deletedPaymentCount=${deletedPayments}`);
};

module.exports = { processRecord };
