const { handleEvent } = require("./src/app/eventHandler.js");


if (process.env.OLD_AXIOS_RETRY === undefined || process.env.OLD_AXIOS_RETRY == 'false') {
  const axios = require("axios");
  const axiosRetry = require("axios-retry").default;

  function retryCallback(retryCount, error, requestConfig) {
    console.warn(`Retry num ${retryCount} - error:${error.message}`);;
  }

  function retryTimesExceededCallback(error, retryCount) {
    console.warn(`Retries exceeded: ${retryCount} - error:${error.message}`);
  }

  axiosRetry(axios, {
    retries: `${process.env.NUM_RETRY}`,
    shouldResetTimeout: true,
    retryCondition: (error) => {
      return axiosRetry.isNetworkOrIdempotentRequestError(error) || error.code === 'ECONNABORTED';
    },
    onRetry: retryCallback,
    onMaxRetryTimesExceeded: retryTimesExceededCallback
  });
  console.info("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
}

exports.handler = async (event) => {
  return handleEvent(event);
};