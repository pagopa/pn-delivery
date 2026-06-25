const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const {
  DynamoDBDocumentClient,
  GetCommand,
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

const updateMetadata = async (tableName, key, fieldsToUpdate) => {
  const setExpressions = [];
  const expressionAttributeValues = {};
  const expressionAttributeNames = {};

  for (const [field, value] of Object.entries(fieldsToUpdate)) {
    if (value !== null && value !== undefined) {
      const valuePlaceholder = `:${field}`;
      const namePlaceholder = `#${field}`;
      setExpressions.push(`${namePlaceholder} = ${valuePlaceholder}`);
      expressionAttributeValues[valuePlaceholder] = value;
      expressionAttributeNames[namePlaceholder] = field;
    }
  }

  if (setExpressions.length === 0) {
    console.log("No fields to update");
    return;
  }

  const params = {
    TableName: tableName,
    Key: key,
    UpdateExpression: `SET ${setExpressions.join(", ")}`,
    ExpressionAttributeValues: expressionAttributeValues,
    ExpressionAttributeNames: expressionAttributeNames,
  };

  const command = new UpdateCommand(params);
  const result = await docClient.send(command);
  console.log(
    `updateMetadata executed on key: ${JSON.stringify(key)} on table: ${tableName}`
  );
  return result;
};

module.exports = { getItem, updateMetadata };
