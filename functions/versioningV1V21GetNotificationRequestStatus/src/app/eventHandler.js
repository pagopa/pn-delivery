const { validateRequest, generateResponse, validateQueryParameters } = require('./requestHelper')

exports.handleEvent = async (event) => {
    console.log('event', event)
    
    const isRequestValid = validateRequest(event)
    if(isRequestValid.length > 0 ){
        return generateResponse({ resultCode: '404.00', resultDescription: 'Not found', errorList: isRequestValid }, 404, {})
    }
    
    const eventValidationErrors = validateQueryParameters(event.queryParameters)
    console.log("eventValidationErrors ", eventValidationErrors)
    if(eventValidationErrors.length > 0){
        return generateResponse({ resultCode: '400.00', resultDescription: 'Validation error', errorList: eventValidationErrors }, 400, {})
    }
    
    console.log("Versioning_V1-V21_GetNotificationRequestStatus_Lambda function started");
    
    // notificationRequestId
    const notificationRequestId = event.queryParameters['notificationRequestId'];
    
    // paProtocolNumber && idempotenceToken
    const paProtocolNumber = event.queryParameters['paProtocolNumber'];
    const idempotenceToken = event.queryParameters['idempotenceToken'];
    
    // get verso pn-delivery
    const url = process.env.PN_DELIVERY_URL.concat('/delivery/requests');
    
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
    let response;
    try {
        response = await fetch(url, { method: "GET", headers: headers });
        let responseV21 = await response.json();
        if (response.ok) {
            const transformedObject = transformFromV21ToV1(responseV21);
            console.log("ritorno risposta trasformata ", transformedObject);
            const ret = {
                statusCode: response.status,
                body: JSON.stringify(transformedObject),
            };
            return ret;
        }
        console.log("risposta negativa: ", response);
        const ret = {
            statusCode: response.status,
            body: JSON.stringify(responseV21),
        };
        return ret;
        
    } catch (error) {
        const ret = {
            statusCode: response?.status ?? 502,
            body: response?.statusText ?? "problem calling fetch",
        };
        return ret;
    }

    function transformFromV21ToV1(responseV21) {
        console.log("transformFromV21ToV1 - responseV21", JSON.stringify(responseV21));

        const recipientsV1 = [];
        responseV21.recipients.forEach(recipientV21 => {
            recipientsV1.push(transformRecipientFromV21ToV1(recipientV21))
        });

        const documentsV1 = [];
        responseV21.documents.forEach(document => {
            documentsV1.push(transformNotificationDocument(document))
        });

        const responseV1 = {
            notificationRequestId: responseV21.notificationRequestId,
            notificationRequestStatus: responseV21.notificationRequestStatus,
            retryAfter: responseV21.retryAfter,
            iun: responseV21.iun,
            errors: responseV21.errors,
            idempotenceToken: responseV21.idempotenceToken,
            paProtocolNumber: responseV21.paProtocolNumber,
            subject: responseV21.subject,
            abstract: responseV21.abstract,
            recipients: recipientsV1,
            documents: documentsV1,
            notificationFeePolicy: responseV21.notificationFeePolicy,
            cancelledIun: responseV21.cancelledIun,
            physicalCommunicationType: responseV21.physicalCommunicationType,
            senderDenomination: responseV21.senderDenomination,
            senderTaxId: responseV21.senderTaxId,
            group: responseV21.group,
            amount: responseV21.amount,
            paymentExpirationDate: responseV21.paymentExpirationDate,
            taxonomyCode: responseV21.taxonomyCode,
            pagoPaIntMode: responseV21.pagoPaIntMode
        }

        console.log("transformFromV21ToV1 - responseV1", JSON.stringify(responseV1));
        return responseV1;
    }

    function transformRecipientFromV21ToV1(recipientV21) {

        const digitalDomicileV1 = recipientV21.digitalDomicile ? transformDigitalDomicile(recipientV21.digitalDomicile) : undefined
        const physicalAddressV1 = transformPhysicalAddress(recipientV21.physicalAddress)
        const paymentV1 = recipientV21.payments.length > 0 ? transformPaymentFromV21ToV1(recipientV21.payments) : undefined

        const recipientV1 = {
            recipientType: recipientV21.recipientType,
            taxId: recipientV21.taxId,
            denomination: recipientV21.denomination,
            physicalAddress: physicalAddressV1
        }

        if(digitalDomicileV1) {
            recipientV1.digitalDomicile = digitalDomicileV1;
        }

        if(paymentV1) {
            recipientV1.payment = paymentV1;
        }

        return recipientV1;
        
    }

    // TODO da portare a fattor comune
    function transformDigitalDomicile(digitalDomicile) {
        return {
            type: digitalDomicile.type,
            address: digitalDomicile.address
        }
    }

    // TODO da portare a fattor comune
    function transformPhysicalAddress(physicalAddress) {
        return {
            at: physicalAddress.at,
            address: physicalAddress.address,
            addressDetails: physicalAddress.addressDetails,
            zip: physicalAddress.zip,
            municipality: physicalAddress.municipality,
            municipalityDetails: physicalAddress.municipalityDetails,
            province: physicalAddress.province,
            foreignState: physicalAddress.foreignState,
        }
    }

    function transformPaymentFromV21ToV1(paymentsV21) {
        const paymentV1 = {

        }

        return paymentV1;
    }

    // TODO da portare a fattor comune
    function transformNotificationDocument(doc) {
        const digests = {
          sha256: doc.digests?.sha256,
        };
    
        const contentType = doc.contentType;
        const ref = doc.ref ? {
          key: doc.ref.key,
          versionToken: doc.ref.versionToken,
        } : undefined;
        const title = doc.title;
        const docIdx = doc.docIdx;
    
        return {
          contentType: contentType,
          digests: digests,
          ref: ref,
          title: title,
          docIdx: docIdx,
        };
      }
};