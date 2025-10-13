const { validateRequest, generateResponse, validateNewNotification, findRequestVersion, fromNewNotificationRequestV1ToV21, fromNewNotificationRequestV21ToV23 } = require('./requestHelper')


const axios = require("axios");

exports.handleEvent = async (event) => {

    const isRequestValid = validateRequest(event)
    if(isRequestValid.length > 0 ){
        return generateResponse({ resultCode: '404.00', resultDescription: 'Not found', errorList: isRequestValid }, 404, {})
    }

    let finalVersionRequest = JSON.parse(event.body);

    let requestVersion = findRequestVersion(event);

    const eventValidationErrors = validateNewNotification(finalVersionRequest, requestVersion);
    if(eventValidationErrors.length > 0){
        return generateResponse({ resultCode: '400.00', resultDescription: 'Validation error', errorList: eventValidationErrors }, 400, {})
    }

    console.log("Versioning_V1-V21_SendNewNotification_Lambda function started");

    // post verso pn-delivery
    const url = process.env.PN_DELIVERY_URL.concat('/requests');
    const attemptTimeout = `${process.env.ATTEMPT_TIMEOUT_SEC}` * 1000;
    const numRetry = `${process.env.NUM_RETRY}`;

    const headers = JSON.parse(JSON.stringify(event["headers"]));
    headers["x-pagopa-pn-src-ch"] = "B2B";
    headers["x-pagopa-pn-notification-version"] = requestVersion;

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

    // nel caso v1.0 vengono effettuati entrambi gli step: v1.0 -> v2.1 -> v2.3
    switch(requestVersion) {
      case "1":
        finalVersionRequest = fromNewNotificationRequestV1ToV21(finalVersionRequest);
      case "2.1":
        finalVersionRequest = fromNewNotificationRequestV21ToV23(finalVersionRequest);
        break;
    }

    try {
        console.log ('calling ',url);
        let postTimeout = this.attemptTimeout * this.numRetry;
        let response = await axios.post(url, finalVersionRequest, { headers: headers , timeout: postTimeout});

        const ret = {
          statusCode: response.status,
          body: JSON.stringify(response.data)
        }
        return ret;
    } catch(error) {
        if (error.response) {
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
};
