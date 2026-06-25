const utils = require("./utils");
const { unmarshall } = require("@aws-sdk/util-dynamodb");
const dynamo = require("./dynamo");

const CATEGORY_FIELD_MAP = {
  NOTIFICATION_VIEWED: () => ({ viewed: true }),
  NOTIFICATION_VIEWED_INFORMAL: () => ({ viewed: true }),
  SEND_DIGITAL_FEEDBACK: (details) =>
    details && details.responseStatus === "OK" ? { delivered: true } : null,
  SEND_ANALOG_FEEDBACK: (details) =>
    details && details.responseStatus === "OK" ? { delivered: true } : null,
  WORKFLOW_DONE: () => ({ desiredFeedback: true }),
};

const processRecord = async (record) => {
  const kinesisData = utils.decodePayload(record.kinesis.data);
  const timelineElement = unmarshall(kinesisData.dynamodb.NewImage);
  const { iun, category, details } = timelineElement;

  console.log(`Processing record for notification ${iun} with category ${category}`);

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

  const notification = await dynamo.getItem("pn-Notifications", { iun });

  const recipient = notification.recipients[recIndex];
  if (!recipient) {
    const errMsg = `Recipient at index ${recIndex} not found for notification ${iun}`;
    console.error(errMsg);
    throw new Error(errMsg);
  }

  const iun_recipientId = `${iun}##${recipient.recipientId}`;
  await dynamo.updateMetadata(
    "pn-NotificationsMetadata",
    { iun_recipientId },
    fieldsToUpdate
  );
};

module.exports = { processRecord };
