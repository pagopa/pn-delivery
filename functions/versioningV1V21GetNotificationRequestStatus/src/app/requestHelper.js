exports.validateRequest = function(event){
    const { path, httpMethod, queryParameters } = event
    const errors = []
    if(httpMethod === 'GET' && path && path === '/delivery/requests' && queryParameters){
      return []
    }
  
    errors.push('Invalid path/method/query parameters')
    return errors
}

exports.generateResponse = function(errorDetails, statusCode, headers){
    return {
      statusCode: statusCode,
      headers,
      body: JSON.stringify(errorDetails)
    }
}

exports.validateQueryStringParameters = function(queryStringParameters){
    const errors = [];

    // notificationRequestId
    const notificationRequestId = queryStringParameters['notificationRequestId'];

    // paProtocolNumber && idempotenceToken
    const paProtocolNumber = queryStringParameters['paProtocolNumber'];
    const idempotenceToken = queryStringParameters['idempotenceToken'];

    if ( !notificationRequestId ) {
        if ( !(paProtocolNumber && idempotenceToken) ) {
            errors.push('Please specify notificationRequestId or paProtocolNumber, idempotenceToken');
        }
    }

    return errors;
}