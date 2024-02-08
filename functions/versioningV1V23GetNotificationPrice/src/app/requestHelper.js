exports.validateRequest = function(event){
    const { path, httpMethod } = event
    const errors = []
    if(httpMethod === 'GET' && path && path.startsWith('/delivery/price')){
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