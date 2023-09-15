const { validateRequest, generateResponse } = require('./requestHelper')

exports.handleEvent = async (event) => {
    console.log('event', event)

    const isRequestValid = validateRequest(event)
    if(isRequestValid.length > 0 ){
        return generateResponse({ resultCode: '404.00', resultDescription: 'Not found', errorList: isRequestValid }, 404, {})
    }

    const eventValidationErrors = validateNewNotification(event.body)
    if(eventValidationErrors.length>0){
        return generateResponse({ resultCode: '400.00', resultDescription: 'Validation error', errorList: eventValidationErrors }, 400, {})
    }

    // creazione oggetto NewNotificationRequestV21

    // mapping tra NewNotificationRequest e NewNotificationRequestV21

    // post verso pn-delivery

    // creazione della response


    };