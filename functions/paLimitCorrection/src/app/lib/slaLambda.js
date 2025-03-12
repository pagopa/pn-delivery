const { LambdaClient, InvokeCommand } = require("@aws-sdk/client-lambda");
const { Buffer } = require('node:buffer')

async function searchSLAViolations(dateToVerifyLimit) {
  console.log('Calling searchSLAViolations ' + dateToVerifyLimit);
  const client = new LambdaClient();
  let allResults = [];
  let response = null;
  let parsedResponse = null;
  let resultsFiltered = null;
  
  const payload = {
    type: "VALIDATION",
    active: true
  };

  async function recursiveSearch(lastScannedKey = null) {
    console.log('Start recursiveSearch for ', lastScannedKey);

    if (lastScannedKey) {
      payload.lastScannedKey = lastScannedKey;
    }

    const input = {
      FunctionName: process.env.SEARCH_SLA_LAMBDA_ARN,
      Payload: Buffer.from(JSON.stringify(payload))
    };
    const command = new InvokeCommand(input);

    try {
      response = await client.send(command);
      parsedResponse = JSON.parse(Buffer.from(response.Payload));
      resultsFiltered = filterResultsByDate(parsedResponse.results, dateToVerifyLimit);
      console.log('resultsFiltered', JSON.stringify(resultsFiltered));

      allResults = allResults.concat(resultsFiltered);

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

function filterResultsByDate(results, dateToVerifyLimit) {
  return results.filter(result => {
      const resultDate = result.startTimestamp.substring(0, 10);
      const verifyDate = dateToVerifyLimit.toISOString().substring(0, 10);
      return resultDate === verifyDate;
  });
}

module.exports = { searchSLAViolations };