const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const {
  DynamoDBDocumentClient,
  GetCommand,
  DeleteCommand,
  UpdateCommand,
  PutCommand,
} = require("@aws-sdk/lib-dynamodb");
const client = new DynamoDBClient({ region: process.env.AWS_REGION });
const docClient = DynamoDBDocumentClient.from(client, {
  marshallOptions: { removeUndefinedValues: true },
});

const { ItemNotFoundException } = require("./exceptions.js");

const getItem = async (TableName, Key) => {
  const params = {
    TableName,
    Key,
  };
  const command = new GetCommand(params);
  const result = await docClient.send(command);

  if (!result.Item) {
    throw new ItemNotFoundException(JSON.stringify(Key), TableName);
  }
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
    const command = new DeleteCommand(params);
    const result = await docClient.send(command);
    console.log("Item deleted successfully with key:", JSON.stringify(Key));
    return result;
  } catch (error) {
    if (error.name === "ConditionalCheckFailedException") {
      console.log("Delete failed: iun does not match");
      throw error;
    } else {
      console.log("Error deleting item:", error.message);
      throw error;
    }
  }
};

const buildNotificationMetadataUpdateParams = (tablename, item) => ({
  TableName: tablename,
  Key: { iun_recipientId: item.iun_recipientId },
  UpdateExpression:
    "SET #notificationStatus = :notificationStatus, " +
    "#notificationStatusTimestamp = :notificationStatusTimestamp, " +
    "#senderId = :senderId, " +
    "#rootSenderId = :rootSenderId, " +
    "#recipientId = :recipientId, " +
    "#sentAt = :sentAt, " +
    "#notificationGroup = :notificationGroup, " +
    "#communicationType = :communicationType, " +
    "#recipientIds = :recipientIds, " +
    "#tableRow = :tableRow, " +
    "#senderId_recipientId = :senderId_recipientId, " +
    "#senderId_creationMonth = :senderId_creationMonth, " +
    "#recipientId_creationMonth = :recipientId_creationMonth, " +
    "#recipientOne = :recipientOne, " +
    "#viewed = if_not_exists(#viewed, :false), " +
    "#delivered = if_not_exists(#delivered, :false), " +
    "#desiredFeedback = if_not_exists(#desiredFeedback, :false)",
  ConditionExpression:
    "attribute_not_exists(#notificationStatusTimestamp) OR #notificationStatusTimestamp < :statusChangeTimestamp",
  ExpressionAttributeNames: {
    "#notificationStatus": "notificationStatus",
    "#notificationStatusTimestamp": "notificationStatusTimestamp",
    "#senderId": "senderId",
    "#rootSenderId": "rootSenderId",
    "#recipientId": "recipientId",
    "#sentAt": "sentAt",
    "#notificationGroup": "notificationGroup",
    "#communicationType": "communicationType",
    "#recipientIds": "recipientIds",
    "#tableRow": "tableRow",
    "#senderId_recipientId": "senderId_recipientId",
    "#senderId_creationMonth": "senderId_creationMonth",
    "#recipientId_creationMonth": "recipientId_creationMonth",
    "#recipientOne": "recipientOne",
    "#viewed": "viewed",
    "#delivered": "delivered",
    "#desiredFeedback": "desiredFeedback",
  },
  ExpressionAttributeValues: {
    ":notificationStatus": item.notificationStatus,
    ":notificationStatusTimestamp": item.notificationStatusTimestamp,
    ":senderId": item.senderId,
    ":rootSenderId": item.rootSenderId,
    ":recipientId": item.recipientId,
    ":sentAt": item.sentAt,
    ":notificationGroup": item.notificationGroup ?? null,
    ":communicationType": item.communicationType ?? null,
    ":recipientIds": item.recipientIds,
    ":tableRow": item.tableRow,
    ":senderId_recipientId": item.senderId_recipientId,
    ":senderId_creationMonth": item.senderId_creationMonth,
    ":recipientId_creationMonth": item.recipientId_creationMonth,
    ":recipientOne": item.recipientOne,
    ":statusChangeTimestamp": item.notificationStatusTimestamp,
    ":false": false,
  },
});

const buildDelegationMetadataPutParams = (tablename, item) => ({
  TableName: tablename,
  Item: item,
  ConditionExpression:
    "attribute_not_exists(notificationStatusTimestamp) OR #notificationStatusTimestamp < :statusChangeTimestamp",
  ExpressionAttributeNames: {
    "#notificationStatusTimestamp": "notificationStatusTimestamp",
  },
  ExpressionAttributeValues: {
    ":statusChangeTimestamp": item.notificationStatusTimestamp,
  },
});

const putNotificationMetadataRecord = async (tablename, item) => {
  const params = buildNotificationMetadataUpdateParams(tablename, item);
  try {
    const command = new UpdateCommand(params);
    await docClient.send(command);
    console.log(`putItem successfully executed with pk: ${item.iun_recipientId} and status: ${item.notificationStatus} on table: ${tablename}`);
  } catch (error) {
    if (error.name === "ConditionalCheckFailedException") {
      console.log(`update not necessary for item with pk: ${item.iun_recipientId} and status: ${item.notificationStatus} on table: ${tablename}`);
    } else {
      console.log(`Error ${error.message} during putNotificationMetadataRecord with pk: ${item.iun_recipientId} and status: ${item.notificationStatus} on table: ${tablename}`);
      throw error;
    }
  }
};

const putDelegationMetadataRecord = async (tablename, item) => {
  const params = buildDelegationMetadataPutParams(tablename, item);
  try {
    const command = new PutCommand(params);
    await docClient.send(command);
    console.log(`putItem successfully executed with pk: ${item.iun_recipientId_delegateId_groupId} and status: ${item.notificationStatus} on table: ${tablename}`);
  } catch (error) {
    if (error.name === "ConditionalCheckFailedException") {
      console.log(`update not necessary for item with pk: ${item.iun_recipientId_delegateId_groupId} and status: ${item.notificationStatus} on table: ${tablename}`);
    } else {
      console.log(`Error ${error.message} during putDelegationMetadataRecord with pk: ${item.iun_recipientId_delegateId_groupId} and status: ${item.notificationStatus} on table: ${tablename}`);
      throw error;
    }
  }
};

module.exports = { getItem, deleteItem, putNotificationMetadataRecord, putDelegationMetadataRecord, buildNotificationMetadataUpdateParams, buildDelegationMetadataPutParams };
