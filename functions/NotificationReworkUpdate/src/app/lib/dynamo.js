const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const {
  DynamoDBDocumentClient,
  UpdateCommand
} = require("@aws-sdk/lib-dynamodb");
const client = new DynamoDBClient({ region: process.env.AWS_REGION });
const docClient = DynamoDBDocumentClient.from(client, {
  marshallOptions: { removeUndefinedValues: true },
});

const updateWithConditionalStatus = async (tablename, item, partitionKeyName, expectedState) => {
  const params = {
    TableName: tablename,
    Item: item,
    ConditionExpression: "status = :expectedState",
    ExpressionAttributeValues: {
      ":expectedState": expectedState,
    },
  };
  try {
    const command = new UpdateCommand(params);
    const result = await docClient.send(command);
    console.log(`putItem successfully executed with pk: ${item.iun} and reworkId: ${item.reworkId} on table: ${tablename}`);
  } catch (error) {
    if (error.name === "ConditionalCheckFailedException") {
      console.log(
        `update not necessary for item with pk: ${item.iun} and reworkId: ${item.reworkId} on table: ${tablename}`
      );
    } else {
      console.log(`Error ${error.message} during updateWithConditionalStatus with pk: ${item.iun} and reworkId: ${item.reworkId} on table: ${tablename}`);
      throw error;
    }
  }
};

module.exports = { updateWithConditionalStatus };
