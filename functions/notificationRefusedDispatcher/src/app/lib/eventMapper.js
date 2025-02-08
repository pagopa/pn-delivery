const crypto = require('crypto');

exports.mapEvent = async (record) => {

  let date = new Date();

  let body = {
    iun: record.iun,
    paId: record.paId,
    notificationSentAt: record.notificationSentAt
  };

  let messageAttributes = {
    publisher: {
      DataType: 'String',
      StringValue: 'delivery'
    },
    iun: {
      DataType: 'String',
      StringValue: body.iun
    },
    eventId: {
      DataType: 'String',
      StringValue: crypto.randomUUID()
    },
    createdAt: {
      DataType: 'String',
      StringValue: date.toISOString()
    }, 
    eventType:  {
      DataType: 'String',
      StringValue:'NOTIFICATION_REFUSED'
    },
  };

  return {
    Id: record.kinesisSeqNumber,
    MessageAttributes: messageAttributes,
    MessageBody: JSON.stringify(body)
  };
};


