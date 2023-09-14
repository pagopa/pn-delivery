exports.validateRequest = function(event){
    const { path, httpMethod, body } = event
    const errors = []
    if(httpMethod==='POST' && path && path==='/delivery/requests' && body){
        return []
    }
    
    errors.push('Invalid path/method')
    return errors
}

exports.generateResponse = function(errorDetails, statusCode, headers){
    return {
        statusCode: statusCode,
        headers,
        body: JSON.stringify(errorDetails)
    }
}