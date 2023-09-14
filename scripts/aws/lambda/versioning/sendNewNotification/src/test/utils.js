exports.createEvent = function(path, httpMethod, body){
    return {
        path: path,
        httpMethod: httpMethod,
        body: body
    }
}