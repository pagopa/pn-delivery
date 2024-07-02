const AWSXRay = require("aws-xray-sdk-core");

AWSXRay.captureHTTPsGlobal(require("http"));
AWSXRay.captureHTTPsGlobal(require("https"));
AWSXRay.capturePromise();

const { sendMessages } = require("./eventBridgeFunctions");
const { mapMessage, isRecordToSend } = require("./messageUtils");

exports.handler = async (event) => {
  console.debug("PN-DELIVERY-INSERT-TRIGGER", JSON.stringify(event, null, 2));

  let messagesToSend = [];

  for (var i = 0; i < event.Records.length; i++) {
    let record = event.Records[i];
    if (!isRecordToSend(record)) continue;

    const message = mapMessage(record);

    console.info(
      "PN-DELIVERY-INSERT-TRIGGER",
      "Enqueuing message: %j",
      message
    );
    messagesToSend.push(message);
  }

  // se ho messaggi da spedire, procedo
  if (messagesToSend.length > 0) await sendMessages(messagesToSend);
  else console.info("PN-DELIVERY-INSERT-TRIGGER", "Nothing to send");

  const response = {
    StatusCode: 200,
  };
  return response;
};
