exports.validateRequest = function(event){
    const { path, httpMethod, queryStringParameters } = event
    const errors = []
    if(httpMethod === 'GET' && path && path.startsWith('/delivery/') && queryStringParameters){
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

exports.findRequestVersion = function(event) {
    // a partire dalla versione piú recente sul ms riportare la response alla v1
    let version = 10;

    // a partire dalla versione piú recente sul ms riportare la response alla v2.1
    if (event["path"].startsWith("/delivery/v2.1/")) {
        version = 21;
    }

    // a partire dalla versione piú recente sul ms riportare alla v2.3
    if (event["path"].startsWith("/delivery/v2.3/")) {
        version = 23;
    }

    // NB: sebbene (a oggi) la 2.4 non passa di qua, in futuro potrebbe e quindi si è già implementata
    // la logica di traduzione (che probabilmente andrà aggiornata nel futuro)
    if (event["path"].startsWith("/delivery/v2.4/")) {
        version = 24;
    }

    return version;
}