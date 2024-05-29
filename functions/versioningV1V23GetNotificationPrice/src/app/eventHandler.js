const { validateRequest, generateResponse } = require('./requestHelper')
const AWSXRay = require("aws-xray-sdk-core");

AWSXRay.captureHTTPsGlobal(require('http'));
AWSXRay.captureHTTPsGlobal(require('https'));
AWSXRay.capturePromise();

const axios = require("axios");

exports.handleEvent = async (event) => {
    const path = "/price/";

    const validationErrors = validateRequest(event)
    if(validationErrors.length > 0 ){
        return generateResponse({ resultCode: '404.00', resultDescription: 'Not found', errorList: validationErrors }, 404, {})
    }

    console.log("Versioning_V1-V23_GetNotificationPrice_Lambda function started");

    const paTaxId = event.pathParameters["paTaxId"];
    const noticeCode = event.pathParameters["noticeCode"];

    const url = `${process.env.PN_DELIVERY_URL}${path}${paTaxId}/${noticeCode}`;
    const attemptTimeout = `${process.env.ATTEMPT_TIMEOUT}` * 1000;
    const numRetry = `${process.env.NUM_RETRY}`;
    console.log ('calling ', url);
    // get verso pn-delivery
    let response;
    let lastError = null;
    try {
        for (var i=0; i<numRetry; i++) {
            console.log('attempt #',i);
          try {
            response = await axios.get(url);
            if (response) {
              lastError = null;
              break;
            } else {
              console.log('cannot fetch data');
            }
          } catch (error) {
            lastError = error;
            console.log('cannot fetch data');
          }
        }
        if (lastError != null) {
            throw lastError;
        }
        const transformedObject = transformFromV23ToV1(response.data);
        const ret = {
            statusCode: response.status,
            body: JSON.stringify(transformedObject),
        };
        return ret;

    } catch (error) {
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

    function transformFromV23ToV1(responseV23) {
        const responseV1 = {
            iun: responseV23.iun,
            amount: responseV23.partialPrice,
            refinementDate: responseV23.refinementDate,
            notificationViewDate: responseV23.notificationViewDate
        }

        return responseV1;
    }

};