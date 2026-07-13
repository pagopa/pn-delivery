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
  const kinesisData = utils.decodePayload(record.kinesis.data);
  const timelineElement = unmarshall(kinesisData.dynamodb.NewImage);
  const { iun, category, details } = timelineElement;

  console.log(`Processing record for notification ${iun} with category ${category}`);

  const notification = await dynamo.getItem("pn-Notifications", { iun });

  if (notification.communicationType !== "INFORMAL") {
    console.log(`Skipping non-informal notification ${iun}`);
    return;
  }

  const categoryHandler = CATEGORY_FIELD_MAP[category];
  if (!categoryHandler) {
    console.log(`Skipping unhandled category ${category} for notification ${iun}`);
    return;
  }

  const fieldsToUpdate = categoryHandler(details);
  if (!fieldsToUpdate) {
    console.log(`No fields to update for category ${category} on notification ${iun}`);
    return;
  }

  const recIndex = details?.recIndex;
  if (recIndex === null || recIndex === undefined) {
    const errMsg = `Missing recIndex in details for category ${category} on notification ${iun}`;
    console.error(errMsg);
    throw new Error(errMsg);
  }

  const recipient = notification.recipients[recIndex];
  if (!recipient) {
    const errMsg = `Recipient at index ${recIndex} not found for notification ${iun}`;
    console.error(errMsg);
    throw new Error(errMsg);
  }

  const iun_recipientId = `${iun}##${recipient.recipientId}`;
  console.log(
    `Updating notification metadata for ${iun_recipientId}:`,
    JSON.stringify(fieldsToUpdate)
  );
  await dynamo.updateMetadata(
    "pn-NotificationsMetadata",
    { iun_recipientId, sentAt: notification.sentAt },
    fieldsToUpdate
  );
};

module.exports = { processRecord };
