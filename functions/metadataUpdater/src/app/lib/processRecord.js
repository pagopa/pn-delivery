const utils = require("./utils");
const { unmarshall } = require("@aws-sdk/util-dynamodb");
const dynamo = require("./dynamo");

const CATEGORY_FIELD_MAP = {
  INFORMAL_NOTIFICATION_VIEWED: () => ({ viewed: true }),
  DELIVERED: () => ({ delivered: true }),
  WORKFLOW_DONE_REACHED: () => ({ desiredFeedback: true }),
  WORKFLOW_DONE_UNREACHED: () => ({ desiredFeedback: true }),
};

const processRecord = async (record) => {
  const sequenceNumber = record.kinesis?.sequenceNumber;
  console.log(`[metadataUpdater] Decoding Kinesis record: sequenceNumber=${sequenceNumber}`);
  const kinesisData = utils.decodePayload(record.kinesis.data);
  const timelineElement = unmarshall(kinesisData.dynamodb.NewImage);
  const { iun, category, details } = timelineElement;

  console.log(`[metadataUpdater] Timeline event decoded: sequenceNumber=${sequenceNumber}, iun=${iun}, category=${category}, recIndex=${details?.recIndex}`);

  console.log(`[metadataUpdater] Loading notification: iun=${iun}`);
  const notification = await dynamo.getItem("pn-Notifications", { iun });
  console.log(`[metadataUpdater] Notification loaded: iun=${iun}, communicationType=${notification.communicationType ?? "LEGACY"}, recipientCount=${notification.recipients?.length ?? 0}`);

  if (notification.communicationType !== "INFORMAL") {
    console.log(`[metadataUpdater] Skipping non-informal notification: iun=${iun}, communicationType=${notification.communicationType ?? "LEGACY"}`);
    return;
  }

  const categoryHandler = CATEGORY_FIELD_MAP[category];
  if (!categoryHandler) {
    console.log(`[metadataUpdater] Skipping unhandled category: iun=${iun}, category=${category}`);
    return;
  }

  const fieldsToUpdate = categoryHandler(details);
  if (!fieldsToUpdate) {
    console.log(`[metadataUpdater] No fields to update: iun=${iun}, category=${category}`);
    return;
  }

  const recIndex = details?.recIndex;
  if (recIndex === null || recIndex === undefined) {
    const errMsg = `Missing recIndex in details for category ${category} on notification ${iun}`;
    console.error(`[metadataUpdater] ${errMsg}`);
    throw new Error(errMsg);
  }

  const recipient = notification.recipients[recIndex];
  if (!recipient) {
    const errMsg = `Recipient at index ${recIndex} not found for notification ${iun}`;
    console.error(`[metadataUpdater] ${errMsg}`);
    throw new Error(errMsg);
  }

  const iun_recipientId = `${iun}##${recipient.recipientId}`;
  console.log(`[metadataUpdater] Updating notification metadata: iun_recipientId=${iun_recipientId}, sentAt=${notification.sentAt}, fields=${Object.keys(fieldsToUpdate).join(",")}`);
  await dynamo.updateMetadata(
    "pn-NotificationsMetadata",
    { iun_recipientId, sentAt: notification.sentAt },
    fieldsToUpdate
  );
  console.log(`[metadataUpdater] Notification metadata updated: iun_recipientId=${iun_recipientId}, category=${category}`);
};

module.exports = { processRecord };
