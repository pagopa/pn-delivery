const { handleEvent } = require("./src/app/eventHandler.js");



const axios = require("axios");
const axiosRetry = require("axios-retry").default;

axiosRetry(axios, {
    retries: `${process.env.NUM_RETRY}`,
    shouldResetTimeout: true ,
    retryCondition: (error) => {
      return axiosRetry.isNetworkOrIdempotentRequestError(error) || error.code === 'ECONNABORTED';
    },
    onRetry: retryCallback,
    onMaxRetryTimesExceeded: retryTimesExceededCallback
});

exports.handler = async (event) => {
  return handleEvent(event);
};