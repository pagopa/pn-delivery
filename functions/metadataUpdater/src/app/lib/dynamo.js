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
  console.log(`[metadataUpdater] DynamoDB GetItem started: table=${TableName}, key=${JSON.stringify(Key)}`);
  const params = {
    TableName,
    Key,
  };
  const command = new GetCommand(params);
  const result = await docClient.send(command);

  if (!result.Item) {
    console.error(`[metadataUpdater] DynamoDB GetItem returned no item: table=${TableName}, key=${JSON.stringify(Key)}`);
    throw new ItemNotFoundException(JSON.stringify(Key), TableName);
  }
  console.log(`[metadataUpdater] DynamoDB GetItem completed: table=${TableName}, key=${JSON.stringify(Key)}`);
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
    console.log(`[metadataUpdater] DynamoDB UpdateItem skipped: no fields to update, table=${tableName}, key=${JSON.stringify(key)}`);
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
  try {
    console.log(`[metadataUpdater] DynamoDB UpdateItem started: table=${tableName}, key=${JSON.stringify(key)}, fields=${Object.keys(fieldsToUpdate).join(",")}`);
    const result = await docClient.send(command);
    console.log(`[metadataUpdater] DynamoDB UpdateItem completed: table=${tableName}, key=${JSON.stringify(key)}, fields=${Object.keys(fieldsToUpdate).join(",")}`);
    return result;
  } catch (error) {
    console.error(`[metadataUpdater] DynamoDB UpdateItem failed: table=${tableName}, key=${JSON.stringify(key)}, fields=${Object.keys(fieldsToUpdate).join(",")}, error=${error.message}`, error.stack);
    throw error;
  }
};

module.exports = { getItem, updateMetadata };
