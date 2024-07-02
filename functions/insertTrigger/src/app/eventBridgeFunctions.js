const {
  EventBridgeClient,
  PutEventsCommand,
} = require("@aws-sdk/client-eventbridge");

const DESTINATION_ID = "pn-delivery-push";
const MESSAGE_DETAIL = "PnDeliveryValidationOutcomeEvent";
const EVENT_SOURCE = "eventbridge.pn-delivery.insertTrigger";

const EVENT_BUS_ARN = process.env.EVENT_BUS_ENDPOINT
  ? process.env.EVENT_BUS_ENDPOINT
  : "";

const sendMessages = async (messages = []) => {
  const client = new EventBridgeClient({});
  console.debug(
    "PN-DELIVERY-INSERT-TRIGGER",
    `Using ${EVENT_BUS_ARN} as endpoint`
  );

  const entries = [];
  for (let i = 0; i < messages.length; i++) {
    const currEntry = {
      Detail: JSON.stringify({
        body: messages[i].MessageBody,
        cxId: DESTINATION_ID,
        Id: messages[i].Id,
        DelaySeconds: messages[i].DelaySeconds,
        MessageGroupId: messages[i].MessageGroupId,
        MessageDeduplicationId: messages[i].MessageDeduplicationId,
        MessageAttributes: messages[i].MessageAttributes,
      }),
      DetailType: MESSAGE_DETAIL,
      Resources: [],
      Source: EVENT_SOURCE,
      EventBusName: EVENT_BUS_ARN,
    };
    entries.push(currEntry);
  }

  console.debug(
    "PN-DELIVERY-INSERT-TRIGGER",
    "PutEvents sending events",
    entries
  );
  const response = await client.send(
    new PutEventsCommand({
      Entries: entries,
    })
  );

  console.debug("PN-DELIVERY-INSERT-TRIGGER", "PutEvents response:", response);

  return response;
};

module.exports = { sendMessages };
