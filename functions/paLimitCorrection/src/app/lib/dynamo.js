const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const {
  DynamoDBDocumentClient,
  QueryCommand,
  UpdateCommand
} = require("@aws-sdk/lib-dynamodb");
const client = new DynamoDBClient({ region: process.env.AWS_REGION });
const docClient = DynamoDBDocumentClient.from(client, {
  marshallOptions: { removeUndefinedValues: true },
});

const getNotificationLimit = async (yearMonth) => {
  console.log('Calling getNotificationLimit with yearMonth:', yearMonth);
  const params = {
    TableName: process.env.DYNAMODB_TABLE_LIMIT || "pn-PaNotificationLimit",
    IndexName: "yearMonth-index",
    KeyConditionExpression: "#gsiKey = :gsiValue",
    ExpressionAttributeNames: {
      "#gsiKey": "yearMonth"
    },
    ExpressionAttributeValues: {
      ":gsiValue": yearMonth
    }
  };

  const allItems = await fetchData(params);

  console.log('Found PaNotificationLimit:', allItems.length);
  return allItems;
};

const getNotificationMetadata = async (key, sortKey) => {
  console.log('Calling getNotificationMetadata with key:', key, 'and sortKey:', sortKey);
  const params = {
    TableName: process.env.DYNAMODB_TABLE_METADATA || "pn-NotificationsMetadata",
    IndexName: "senderId",
    KeyConditionExpression: "#gsiKey = :gsiValue AND begins_with(#sortKey, :sortKey)",
    ProjectionExpression: "#iun_recipientId",
    ExpressionAttributeNames: {
      "#gsiKey": "senderId_creationMonth",
      '#sortKey': 'sentAt',
      "#iun_recipientId": "iun_recipientId"
    },
    ExpressionAttributeValues: {
      ":gsiValue": key,
      ':sortKey': sortKey
    },
  };

  const allItems = await fetchData(params);

  console.log('Found NotificationsMetadata:', allItems.length);
  return allItems;
};

const fetchData = async (params) => {
  let allItems = [];
  let exclusiveStartKey = null;

  do {
    if (exclusiveStartKey) {
      console.log('Fetching more data with exclusiveStartKey:', exclusiveStartKey);
      params.ExclusiveStartKey = exclusiveStartKey;
    }

    const result = await docClient.send(new QueryCommand(params));
    console.log('Fetched', result.Count, 'items');
    allItems = allItems.concat(result.Items);

    exclusiveStartKey = result.LastEvaluatedKey;

    // Log memory usage
    const memoryUsage = process.memoryUsage();
    console.log('Memory usage:', memoryUsage);

  } while (exclusiveStartKey);

  return allItems;
};

const updateNotificationLimit = async (key, dailyCounterX, dailyCounterValue, deltaDailyCounter) => {
  console.log(`Calling updateNotificationLimit with key ${key}, dailyCounterX ${dailyCounterX}, dailyCounterValue ${dailyCounterValue}, deltaDailyCounter ${deltaDailyCounter}`);
  const params = {
    TableName: process.env.DYNAMODB_TABLE_LIMIT || "pn-PaNotificationLimit",
    Key: { "pk": key },
    UpdateExpression: "ADD #residualLimit :increment, #dailyCounterX :decrement",
    ConditionExpression: "#dailyCounterX = :dailyCounterValue",
    ExpressionAttributeNames: {
      "#residualLimit": "residualLimit",
      "#dailyCounterX": dailyCounterX
    },
    ExpressionAttributeValues: {
      ":increment": deltaDailyCounter,
      ":decrement": - deltaDailyCounter,
      ":dailyCounterValue": dailyCounterValue
    }
  };

  try {
    await docClient.send(new UpdateCommand(params));
    console.log('updateNotificationLimit SUCCESS for key:', key);
  } catch (e) {
    console.error('Error updateNotificationLimit:', e);
    throw e;
  }
};

module.exports = { getNotificationMetadata, getNotificationLimit, updateNotificationLimit };
