const utils = require("./utils");
const { unmarshall } = require("@aws-sdk/util-dynamodb");
const dynamo = require("./dynamo");

const LEGAL_CATEGORY_FIELD_MAP = {
  NOTIFICATION_VIEWED: () => ({ viewed: true }),
  SEND_DIGITAL_FEEDBACK: (details) =>
    details && details.responseStatus === "OK" ? { delivered: true } : null,
  SEND_ANALOG_FEEDBACK: (details) =>
    details && details.responseStatus === "OK" ? { delivered: true } : null,
};

const INFORMAL_CATEGORY_FIELD_MAP = {
  INFORMAL_NOTIFICATION_VIEWED: () => ({ viewed: true }),
  REACHED: () => ({ delivered: true }),
  WORKFLOW_DONE: () => ({ desiredFeedback: true }),
};

const processRecord = async (record) => {
  const kinesisData = utils.decodePayload(record.kinesis.data);
  const timelineElement = unmarshall(kinesisData.dynamodb.NewImage);
  const { iun, category, details } = timelineElement;

  console.log(`Processing record for notification ${iun} with category ${category}`);

  const notification = await dynamo.getItem("pn-Notifications", { iun });

  const communicationType = notification.communicationType;
  const categoryMap =
    communicationType === "INFORMAL"
      ? INFORMAL_CATEGORY_FIELD_MAP
      : LEGAL_CATEGORY_FIELD_MAP;

  const categoryHandler = categoryMap[category];
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
  await dynamo.updateMetadata(
    "pn-NotificationsMetadata",
    { iun_recipientId },
    fieldsToUpdate
  );
};

module.exports = { processRecord };
