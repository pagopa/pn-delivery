const { LambdaClient, InvokeCommand } = require("@aws-sdk/client-lambda");
const { Buffer } = require('node:buffer')

async function searchSLAViolations(olderThan) {
  console.log('Calling searchSLAViolations with olderThan:', olderThan);
  const client = new LambdaClient();
  let allResults = [];

  async function recursiveSearch(lastScannedKey = null) {
    const payload = {
      type: "VALIDATION",
      active: true,
      olderThan: `${olderThan}`
    };
    if (lastScannedKey) {
      console.log('lastScannedKey:', lastScannedKey);
      payload.lastScannedKey = lastScannedKey;
    }

    const input = {
      FunctionName: process.env.SEARCH_SLA_LAMBDA_ARN,
      Payload: Buffer.from(JSON.stringify(payload))
    };
    const command = new InvokeCommand(input);

    try {
      const response = await client.send(command);
      const parsedResponse = JSON.parse(Buffer.from(response.Payload));
      allResults = allResults.concat(parsedResponse.results);

      if (parsedResponse.lastScannedKey) {
        await recursiveSearch(parsedResponse.lastScannedKey);
      }
    } catch (e) {
      console.error(e);
      throw e;
    }
  }

  await recursiveSearch();

  console.log('Found SLA violations:', allResults.length);
  return allResults;
}

module.exports = { searchSLAViolations };