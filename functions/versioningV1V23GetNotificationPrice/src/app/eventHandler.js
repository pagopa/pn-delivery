const { validateRequest, generateResponse } = require('./requestHelper')


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

    // get verso pn-delivery
    const url = `${process.env.PN_DELIVERY_URL}${path}${paTaxId}/${noticeCode}`;

    console.log ('calling ', url);
    
    try {
        let response = await axios.get(url);
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