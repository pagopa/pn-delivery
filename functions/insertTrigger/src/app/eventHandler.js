const AWSXRay = require("aws-xray-sdk-core");

AWSXRay.captureHTTPsGlobal(require("http"));
AWSXRay.captureHTTPsGlobal(require("https"));
AWSXRay.capturePromise();

const { SQSClient, SendMessageBatchCommand } = require("@aws-sdk/client-sqs");
let sqsParams = { region: process.env.REGION };
if (process.env.ENDPOINT) sqsParams.endpoint = process.env.ENDPOINT;
const sqs = new SQSClient(sqsParams);

const QUEUE_URL = process.env.QUEUE_URL;

exports.handler = async (event, context) => {
  console.log(JSON.stringify(event, null, 2));
  console.log(QUEUE_URL);

  let messagesToSend = [];

  for (var i = 0; i < event.Records.length; i++) {
    let record = event.Records[i];
    if (!isRecordToSend(record)) continue;

    const message = mapMessage(record);

    console.log("Enqueuing message: %j", message);
    messagesToSend.push(message);
  }

  // se ho messaggi da spedire, procedo
  if (messagesToSend.length > 0) await sendMessages(messagesToSend);
  else console.log("Nothing to send");

  const response = {
    StatusCode: 200,
  };
  return response;
};
