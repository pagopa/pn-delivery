const utils = require("./utils");
const { unmarshall } = require("@aws-sdk/util-dynamodb");
const putNotificationRework = require("./putNotificationRework.js");

const processRecord = async (record) => {
  const kinesisData = utils.decodePayload(record.kinesis.data);
  const timelineElement = unmarshall(kinesisData.dynamodb.NewImage);

  console.log(
    `Processing record for notification rework ${timelineElement.iun} with category ${timelineElement.category} and reworkId ${timelineElement.reworkId}`
  );

  switch (timelineElement.category) {
    case "REWORK_CREATED":
      await putNotificationRework.updateRework(
        timelineElement,
        "CREATED",
        "READY",
        true
      );
      break;
    case "SEND_ANALOG_PROGRESS":
      await putNotificationRework.updateRework(
        timelineElement,
        "READY",
        "IN_PROGRESS",
        false
      );
      break;
    case "SEND_ANALOG_FEEDBACK":
      await putNotificationRework.updateRework(
        timelineElement,
        "IN_PROGRESS",
        "DONE",
        false
      );
      break;
    default:
      throw new Error(`Category ${timelineElement.category} not managed`);
  }
};

module.exports = { processRecord };
