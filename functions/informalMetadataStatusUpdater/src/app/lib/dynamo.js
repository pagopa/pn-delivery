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

const putMetadata = async (tablename, item, partitionKeyName) => {
  const params = buildUpdateParams(tablename, item, partitionKeyName);
  try {
    const command = new UpdateCommand(params);
    const result = await docClient.send(command);
    console.log(`putItem successfully executed with pk: ${item[partitionKeyName]} and status: ${item.notificationStatus} on table: ${tablename}`);
  } catch (error) {
    if (error.name === "ConditionalCheckFailedException") {
      console.log(
        `update not necessary for item with pk: ${item[partitionKeyName]} and status: ${item.notificationStatus} on table: ${tablename}`
      );
    } else {
      console.log(`Error ${error.message} during putMetadata with pk: ${item[partitionKeyName]} and status: ${item.notificationStatus} on table: ${tablename}`);
      throw error;
    }
  }
};

const buildUpdateParams = (tablename, item, partitionKeyName) => {
  const key = { [partitionKeyName]: item[partitionKeyName] };

  const setExpressions = [];
  const expressionAttributeValues = { ":statusChangeTimestamp": item.notificationStatusTimestamp };
  const expressionAttributeNames = { "#notificationStatusTimestamp": "notificationStatusTimestamp" };

  for (const [field, value] of Object.entries(item)) {
    if (field !== partitionKeyName && value !== null && value !== undefined) {
      setExpressions.push(`#${field} = :${field}`);
      expressionAttributeValues[`:${field}`] = value;
      expressionAttributeNames[`#${field}`] = field;
    }
  }

  return {
    TableName: tablename,
    Key: key,
    UpdateExpression: `SET ${setExpressions.join(", ")}`,
    ConditionExpression:
      "attribute_not_exists(#notificationStatusTimestamp) OR #notificationStatusTimestamp < :statusChangeTimestamp",
    ExpressionAttributeNames: expressionAttributeNames,
    ExpressionAttributeValues: expressionAttributeValues,
  };
};

module.exports = { getItem, deleteItem, putMetadata, buildUpdateParams };
