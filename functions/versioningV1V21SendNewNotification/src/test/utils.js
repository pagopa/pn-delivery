exports.createEvent = function(path, httpMethod, body){
    return {
        path: path,
        headers: {},
        requestContext: {
            authorizer: {},
        },
        httpMethod: httpMethod,
        body: body
    }
}