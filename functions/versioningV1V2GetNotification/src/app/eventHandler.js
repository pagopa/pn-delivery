// converte la risposta V2.x a V1
const { transformFromV28ToV27 } = require('./mapper/mapperV28ToV27.js');
const { transformFromV27ToV26 } = require('./mapper/mapperV27ToV26.js');
const { transformFromV26ToV25 } = require('./mapper/mapperV26ToV25.js');
const { transformFromV25ToV24 } = require('./mapper/mapperV25ToV24.js');
const { transformFromV24ToV23 } = require('./mapper/mapperV24ToV23.js');
const { transformFromV23ToV21 } = require('./mapper/mapperV23ToV21.js');
const { transformFromV21ToV20 } = require('./mapper/mapperV21ToV20.js');
const { transformFromV20ToV1 } = require('./mapper/mapperV20ToV1.js');
const { ValidationException, DeceasedWorkflowException } = require("./exceptions.js");

const axios = require("axios");
const axiosRetry = require("axios-retry").default;

exports.versioning = async (event, context) => {
  const path = "/notifications/sent/";

  if (
    event["resource"].indexOf(`${path}{iun}`) < 0 ||
    !event["path"].startsWith("/delivery/") ||
    event["httpMethod"].toUpperCase() !== "GET"
  ) {
    console.log(
      "ERROR ENDPOINT ERRATO: {resource, path, httpMethod} ",
      event["resource"],
      event["path"],
      event["httpMethod"]
    );
    const err = {
      statusCode: 502,
      body: JSON.stringify(generateProblem(502, "ENDPOINT ERRATO"))
    };

    return err;
  }

  console.log("Versioning_V1-V2.x_GetNotification_Lambda function started");

  const IUN = event.pathParameters["iun"];

  const url = `${process.env.PN_DELIVERY_URL}${path}${IUN}`;

  const attemptTimeout = `${process.env.ATTEMPT_TIMEOUT_SEC}` * 1000;

  const numRetry = `${process.env.NUM_RETRY}`;

  console.log(`attemptTimeout ${attemptTimeout} millis  ${numRetry} retry`);

  axiosRetry(axios, {
    retries: numRetry,
    shouldResetTimeout: true,
    retryCondition: (error) => {
      return axiosRetry.isNetworkOrIdempotentRequestError(error) || error.code === 'ECONNABORTED';
    },
    onRetry: retryCallback,
    onMaxRetryTimesExceeded: retryTimesExceededCallback
  });

  // ora è necessario sapere da che versione sto invocando, per prendere le decisioni corrette.
  let version = 10;

  // v2.0 must add never categories to the allowed ones
  if (event["path"].startsWith("/delivery/v2.0/")) {
    version = 20;
  }

  // v2.1 must add never categories to the allowed ones
  if (event["path"].startsWith("/delivery/v2.1/")) {
    version = 21;
  }

  // v2.3 must add never categories to the allowed ones
  if (event["path"].startsWith("/delivery/v2.3/")) {
    version = 23;
  }

  // NB: sebbene (a oggi) la 2.4 non passa di qua, in futuro potrebbe e quindi si è già implementata
  // la logica di traduzione (che probabilmente andrà aggiornata nel futuro)
  if (event["path"].startsWith("/delivery/v2.4/")) {
    version = 24;
  }

  if (event["path"].startsWith("/delivery/v2.5/")) {
    version = 25;
  }

  if (event["path"].startsWith("/delivery/v2.6/")) {
    version = 26;
  }

  if (event["path"].startsWith("/delivery/v2.7/")) {
    version = 27;
  }

  const headers = JSON.parse(JSON.stringify(event["headers"]));
  headers["x-pagopa-pn-src-ch"] = "B2B";

  if (event.requestContext.authorizer["cx_groups"]) {
    headers["x-pagopa-pn-cx-groups"] =
      event.requestContext.authorizer["cx_groups"];
  }
  if (event.requestContext.authorizer["cx_id"]) {
    headers["x-pagopa-pn-cx-id"] = event.requestContext.authorizer["cx_id"];
  }
  if (event.requestContext.authorizer["cx_role"]) {
    headers["x-pagopa-pn-cx-role"] = event.requestContext.authorizer["cx_role"];
  }
  if (event.requestContext.authorizer["cx_type"]) {
    headers["x-pagopa-pn-cx-type"] = event.requestContext.authorizer["cx_type"];
  }
  if (event.requestContext.authorizer["cx_jti"]) {
    headers["x-pagopa-pn-jti"] = event.requestContext.authorizer["cx_jti"];
  }
  if (event.requestContext.authorizer["sourceChannelDetails"]) {
    headers["x-pagopa-pn-src-ch-details"] =
      event.requestContext.authorizer["sourceChannelDetails"];
  }
  if (event.requestContext.authorizer["uid"]) {
    headers["x-pagopa-pn-uid"] = event.requestContext.authorizer["uid"];
  }
  if(process.env._X_AMZN_TRACE_ID){
    headers["X-Amzn-Trace-Id"] = process.env._X_AMZN_TRACE_ID;
  }else{
    console.log("No _X_AMZN_TRACE_ID found in evnvironment variables");
  }
  
  console.log("calling ", url);
  let response;
  let lastError = null;

  try {
    response = await axios.get(url, { headers: headers, timeout: attemptTimeout });

    const notificationStatus_ENUM = [
      "IN_VALIDATION",
      "ACCEPTED",
      "REFUSED",
      "DELIVERING",
      "DELIVERED",
      "VIEWED",
      "EFFECTIVE_DATE",
      "PAID",
      "UNREACHABLE",
      "CANCELLED",
      "RETURNED_TO_SENDER"
    ];

    if (!notificationStatus_ENUM.includes(response.data.notificationStatus)) {
      throw new ValidationException("Status not supported");
    }

    const transformedObject = applyMappings(version, response);

    const ret = {
      statusCode: response.status,
      body: JSON.stringify(transformedObject),
    };
    return ret;
  } catch (error) {
    if (error instanceof ValidationException) {
      console.info("Validation Exception: ", error)
      return {
        statusCode: 400,
        body: JSON.stringify(generateProblem(400, error.message))
      }
    } else if (error instanceof DeceasedWorkflowException) {
      console.info("Deceased Workflow Exception: ", error)
      return {
        statusCode: 400,
        body: JSON.stringify(generateProblem(400, error.message))
      }
    } else if (error.response) {
      console.log("risposta negativa: ", error.response.data);
      const ret = {
        statusCode: error.response.status,
        body: JSON.stringify(error.response.data)
      };
      return ret;
    } else {
      console.warn("Error on url " + url, error)
      return {
        statusCode: 500,
        body: JSON.stringify(generateProblem(500, error.message))
      }
    }
  }

  function generateProblem(status, message) {
    return {
      status: status,
      errors: [
        {
          code: message
        }
      ]
    }
  }

  function retryCallback(retryCount, error, requestConfig) {
    console.warn(`Retry num ${retryCount} - error:${error.message}`);
  }

  function retryTimesExceededCallback(error, retryCount) {
    console.warn(`Retries exceeded: ${retryCount} - error:${error.message}`);
  }
};

function applyMappings(version, response) {
  let transformedObject = response.data;
  switch (version) {
    case 10:
      transformedObject = transformFromV20ToV1(applyMappings(20, response));
      break;
    case 20:
      transformedObject = transformFromV21ToV20(applyMappings(21, response));
      break;
    case 21:
      transformedObject = transformFromV23ToV21(applyMappings(23, response));
      break;
    case 23:
      transformedObject = transformFromV24ToV23(applyMappings(24, response));
      break;
    case 24:
      transformedObject = transformFromV25ToV24(applyMappings(25, response));
      break;
    case 25:
      transformedObject = transformFromV26ToV25(applyMappings(26, response));
      break;
    case 26:
      transformedObject = transformFromV27ToV26(applyMappings(27, response));
      break;
    case 27:
      transformedObject = transformFromV28ToV27(response.data);
      break;
  }
  return transformedObject;
}