const { extractKinesisData } = require("./lib/utils");
const { mapEvent } = require("./lib/eventMapper");
const { unmarshall } = require("@aws-sdk/util-dynamodb");
const { SQSClient, SendMessageCommand } = require("@aws-sdk/client-sqs");

const sqs = new SQSClient({ region: process.env.REGION });
const QUEUE_URL = process.env.QUEUE_URL

const eventHandler = async (event) => {
  const cdcEvents = extractKinesisData(event);
  console.log(`Batch size: ${cdcEvents.length} cdc`);

  if (cdcEvents.length == 0) {
    console.log("No events to process");
    return {
      batchItemFailures: [],
    };
  }

  for(let i=0; i<cdcEvents.length; i++) {
    let currentCdcEvent = cdcEvents[i];
    let record = {
      ...unmarshall(currentCdcEvent.dynamodb.NewImage),
      kinesisSeqNumber: currentCdcEvent.kinesisSeqNumber
    };

    try {
      console.log('Sending notification refused event for notification with iun: ', record.iun);
      let sqsRecord = await mapEvent(record);
      let sqsParams = {
        QueueUrl: QUEUE_URL,
        ...sqsRecord
      };
      await sqs.send(new SendMessageCommand(sqsParams));
    } catch (error) {
      console.log(`Error: ${error.message} sending event for notification with iun: ${record.iun}`);
      return {batchItemFailures: [{ itemIdentifier: record.kinesisSeqNumber }]};
    }
  }


  console.log("Processed all notification refused events successfully");
  return {
    batchItemFailures: [],
  };
};

module.exports = { eventHandler };
