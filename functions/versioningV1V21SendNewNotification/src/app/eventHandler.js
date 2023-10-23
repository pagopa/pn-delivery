const { validateRequest, generateResponse, validateNewNotification, createNewNotificationRequesV21 } = require('./requestHelper')

exports.handleEvent = async (event) => {

    const isRequestValid = validateRequest(event)
    if(isRequestValid.length > 0 ){
        return generateResponse({ resultCode: '404.00', resultDescription: 'Not found', errorList: isRequestValid }, 404, {})
    }

    const eventBody = JSON.parse(event.body)

    const eventValidationErrors = validateNewNotification(eventBody)
    console.log("eventValidationErrors ", eventValidationErrors)
    if(eventValidationErrors.length > 0){
        return generateResponse({ resultCode: '400.00', resultDescription: 'Validation error', errorList: eventValidationErrors }, 400, {})
    }

    console.log("Versioning_V1-V21_SendNewNotification_Lambda function started");

    var newNotificationRequestV21 = createNewNotificationRequesV21(eventBody);

    // post verso pn-delivery
    const url = process.env.PN_DELIVERY_URL.concat('/requests');

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
      headers["x-pagopa-pn-src-ch-detail"] =
        event.requestContext.authorizer["sourceChannelDetails"];
    }
    if (event.requestContext.authorizer["uid"]) {
      headers["x-pagopa-pn-uid"] = event.requestContext.authorizer["uid"];
    }


    console.log ('calling ',url);
    const response = await fetch(url, {
        method: "POST",
        body: JSON.stringify(newNotificationRequestV21),
        headers: headers
    });

    const responseJson = await response.json(); 
    console.log('Response: ' + JSON.stringify(responseJson));
    
    const ret = {
      statusCode: response.status,
      body: JSON.stringify(responseJson)
    }
    return ret;
    };