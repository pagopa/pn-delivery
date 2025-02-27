const { LambdaClient, InvokeCommand } = require("@aws-sdk/client-lambda");
const { Buffer } = require('node:buffer')

async function searchSLAViolations() {
  console.log('Calling searchSLAViolations');
  const client = new LambdaClient();
  let allResults = [];

  async function recursiveSearch(lastScannedKey = null) {
    const payload = {
      type: "VALIDATION",
      active: true
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

  console.log('Total SLA violations found:', allResults.length);
  return allResults;
}

module.exports = { searchSLAViolations };