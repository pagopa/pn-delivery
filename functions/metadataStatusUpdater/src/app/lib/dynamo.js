const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const {
  DynamoDBDocumentClient,
  GetCommand,
  DeleteCommand,
  UpdateCommand,
} = require("@aws-sdk/lib-dynamodb");
const client = new DynamoDBClient({ region: process.env.AWS_REGION });
const docClient = DynamoDBDocumentClient.from(client, {
  marshallOptions: { removeUndefinedValues: true },
});

const { ItemNotFoundException } = require("./exceptions.js");

const getItem = async (TableName, Key) => {
  console.log(`[metadataStatusUpdater] DynamoDB GetItem started: table=${TableName}, key=${JSON.stringify(Key)}`);
  const params = {
    TableName,
    Key,
  };
  const command = new GetCommand(params);
  const result = await docClient.send(command);

  if (!result.Item) {
    console.error(`[metadataStatusUpdater] DynamoDB GetItem returned no item: table=${TableName}, key=${JSON.stringify(Key)}`);
    throw new ItemNotFoundException(JSON.stringify(Key), TableName);
  }
  console.log(`[metadataStatusUpdater] DynamoDB GetItem completed: table=${TableName}, key=${JSON.stringify(Key)}`);
  return result.Item;
};

const deleteItem = async (TableName, Key, Iun) => {
  const params = {
    TableName,
    Key,
    ConditionExpression: "attribute_exists(iun) AND iun = :Iun",
    ExpressionAttributeValues: {
      ":Iun": Iun,
    },
  };
  try {
    console.log(`[metadataStatusUpdater] DynamoDB DeleteItem started: table=${TableName}, key=${JSON.stringify(Key)}`);
    const command = new DeleteCommand(params);
    const result = await docClient.send(command);
    console.log(`[metadataStatusUpdater] DynamoDB DeleteItem completed: table=${TableName}, key=${JSON.stringify(Key)}`);
    return result;
  } catch (error) {
    if (error.name === "ConditionalCheckFailedException") {
      console.warn(`[metadataStatusUpdater] DynamoDB DeleteItem condition rejected: table=${TableName}, key=${JSON.stringify(Key)}, iun=${Iun}`);
      throw error;
    } else {
      console.error(`[metadataStatusUpdater] DynamoDB DeleteItem failed: table=${TableName}, key=${JSON.stringify(Key)}, error=${error.message}`, error.stack);
      throw error;
    }
  }
};

const addUpdateField = (field, value, expressionAttributeNames, expressionAttributeValues, updateExpressions) => {
  if (value === undefined) {
    return;
  }

  const namePlaceholder = `#${field}`;
  const valuePlaceholder = `:${field}`;
  expressionAttributeNames[namePlaceholder] = field;
  expressionAttributeValues[valuePlaceholder] = value;
  updateExpressions.push(`${namePlaceholder} = ${valuePlaceholder}`);
};

const buildMetadataUpdateParams = (tablename, item, partitionKeyName) => {
  const key = {
    [partitionKeyName]: item[partitionKeyName],
    sentAt: item.sentAt,
  };
  const expressionAttributeNames = {};
  const expressionAttributeValues = {};
  const updateExpressions = [];

  addUpdateField("notificationStatus", item.notificationStatus, expressionAttributeNames, expressionAttributeValues, updateExpressions);
  addUpdateField("notificationStatusTimestamp", item.notificationStatusTimestamp, expressionAttributeNames, expressionAttributeValues, updateExpressions);
  addUpdateField("senderId", item.senderId, expressionAttributeNames, expressionAttributeValues, updateExpressions);
  addUpdateField("rootSenderId", item.rootSenderId, expressionAttributeNames, expressionAttributeValues, updateExpressions);
  addUpdateField("recipientId", item.recipientId, expressionAttributeNames, expressionAttributeValues, updateExpressions);
  addUpdateField("notificationGroup", item.notificationGroup, expressionAttributeNames, expressionAttributeValues, updateExpressions);
  addUpdateField("communicationType", item.communicationType, expressionAttributeNames, expressionAttributeValues, updateExpressions);
  addUpdateField("recipientIds", item.recipientIds, expressionAttributeNames, expressionAttributeValues, updateExpressions);
  addUpdateField("tableRow", item.tableRow, expressionAttributeNames, expressionAttributeValues, updateExpressions);
  addUpdateField("senderId_recipientId", item.senderId_recipientId, expressionAttributeNames, expressionAttributeValues, updateExpressions);
  addUpdateField("senderId_creationMonth", item.senderId_creationMonth, expressionAttributeNames, expressionAttributeValues, updateExpressions);
  addUpdateField("recipientId_creationMonth", item.recipientId_creationMonth, expressionAttributeNames, expressionAttributeValues, updateExpressions);
  addUpdateField("recipientOne", item.recipientOne, expressionAttributeNames, expressionAttributeValues, updateExpressions);
  addUpdateField("mandateId", item.mandateId, expressionAttributeNames, expressionAttributeValues, updateExpressions);
  addUpdateField("delegateId_creationMonth", item.delegateId_creationMonth, expressionAttributeNames, expressionAttributeValues, updateExpressions);

  return {
    TableName: tablename,
    Key: key,
    UpdateExpression: `SET ${updateExpressions.join(", ")}`,
    ConditionExpression:
      "attribute_not_exists(#notificationStatusTimestamp) OR #notificationStatusTimestamp < :statusChangeTimestamp",
    ExpressionAttributeNames: {
      ...expressionAttributeNames,
      "#notificationStatusTimestamp": "notificationStatusTimestamp",
    },
    ExpressionAttributeValues: {
      ...expressionAttributeValues,
      ":statusChangeTimestamp": item.notificationStatusTimestamp,
    },
  };
};

const updateMetadata = async (tablename, item, partitionKeyName) => {
  const params = buildMetadataUpdateParams(tablename, item, partitionKeyName);
  try {
    console.log(`[metadataStatusUpdater] DynamoDB UpdateItem started: table=${tablename}, key=${JSON.stringify(params.Key)}, fields=${Object.keys(params.ExpressionAttributeValues).filter((field) => field !== ":statusChangeTimestamp").map((field) => params.ExpressionAttributeNames[field.replace(":", "#")]).filter(Boolean).join(",")}`);
    const command = new UpdateCommand(params);
    await docClient.send(command);
    console.log(`[metadataStatusUpdater] DynamoDB UpdateItem completed: table=${tablename}, key=${JSON.stringify(params.Key)}, status=${item.notificationStatus}`);
  } catch (error) {
    if (error.name === "ConditionalCheckFailedException") {
      console.warn(`[metadataStatusUpdater] DynamoDB UpdateItem skipped by timestamp condition: table=${tablename}, key=${JSON.stringify(params.Key)}, status=${item.notificationStatus}`);
    } else {
      console.error(`[metadataStatusUpdater] DynamoDB UpdateItem failed: table=${tablename}, key=${JSON.stringify(params.Key)}, status=${item.notificationStatus}, error=${error.message}`, error.stack);
      throw error;
    }
  }
};

module.exports = { getItem, deleteItem, updateMetadata, buildMetadataUpdateParams };
