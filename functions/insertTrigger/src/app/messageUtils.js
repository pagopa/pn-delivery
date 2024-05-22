function isRecordToSend(record) {
  if (record.eventName != "INSERT") return false;

  console.log("Processing dynamoDB Record: %j", record.dynamodb);
  const iun = record.dynamodb.Keys.iun.S;

  if (iun.match(/^\S\S\S\S-\S\S\S\S-\S\S\S\S-\d\d\d\d\d\d-\S-\d$/) == null) {
    console.log("Record is not a notification, skipping");
    return false;
  }

  // il record è buono e va processato e inviato
  return true;
}

function mapMessage(record) {
  const senderPaId = record.dynamodb.NewImage.senderPaId.S;
  const iun = record.dynamodb.Keys.iun.S;

  const message = {
    Id: iun + "_start",
    DelaySeconds: 0,
    MessageGroupId: "DELIVERY-" + iun,
    MessageDeduplicationId: iun + "_start",
    MessageAttributes: {
      createdAt: {
        DataType: "String",
        StringValue: new Date().toISOString(),
      },
      eventId: {
        DataType: "String",
        StringValue: iun + "_start",
      },
      eventType: {
        DataType: "String",
        StringValue: "NEW_NOTIFICATION",
      },
      iun: {
        DataType: "String",
        StringValue: iun,
      },
      publisher: {
        DataType: "String",
        StringValue: "DELIVERY",
      },
    },
    MessageBody: JSON.stringify({ paId: senderPaId }),
  };
  return message;
}

module.exports = { mapMessage, isRecordToSend };
