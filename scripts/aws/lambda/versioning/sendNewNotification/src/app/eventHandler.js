const { validateRequest, generateResponse } = require('./requestHelper')

exports.handleEvent = async (event) => {
    console.log('event', event)

    const isRequestValid = validateRequest(event)
    if(isRequestValid.length > 0 ){
        return generateResponse({ resultCode: '404.00', resultDescription: 'Not found', errorList: isRequestValid }, 404, {})
    }


    };