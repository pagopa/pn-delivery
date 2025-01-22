const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const {
  DynamoDBDocumentClient,
  GetCommand,
  DeleteCommand,
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
    throw new ItemNotFoundException(Key, TableName);
  }
  return result.Item;
};

const deleteItem = async (TableName, Key) => {
  const params = {
    TableName,
    Key,
  };
  const command = new DeleteCommand(params);
  const result = await docClient.send(command);
  console.log("Elemento eliminato con successo:", Key);
};

const putMetadata = async (tablename, item, partitionKeyName) => {
  const params = {
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
  };
  try {
    const command = new PutCommand(params);
    const result = await docClient.send(command);
    console.log("Elemento inserito con successo:", item[partitionKeyName]);
  } catch (error) {
    if (error.name === "ConditionalCheckFailedException") {
      console.log(
        `update not necessary for item with key: ${item[partitionKeyName]} in table: ${tablename}`
      );
    } else {
      throw error;
    }
  }
};

module.exports = { getItem, deleteItem, putMetadata };
