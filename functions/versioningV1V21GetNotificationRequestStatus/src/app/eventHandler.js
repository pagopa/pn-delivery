const { validateRequest, generateResponse, validateQueryStringParameters, findRequestVersion } = require('./requestHelper')
const {ValidationException} = require("./exceptions.js");


const axios = require("axios");
const axiosRetry = require("axios-retry").default;

exports.handleEvent = async (event) => {
    
    const isRequestValid = validateRequest(event)
    if(isRequestValid.length > 0 ){
        return generateResponse({ resultCode: '404.00', resultDescription: 'Not found', errorList: isRequestValid }, 404, {})
    }
    
    const eventValidationErrors = validateQueryStringParameters(event.queryStringParameters)
    if(eventValidationErrors.length > 0){
        return generateResponse({ resultCode: '400.00', resultDescription: 'Validation error', errorList: eventValidationErrors }, 400, {})
    }
    
    console.log("Versioning_V1-V21_GetNotificationRequestStatus_Lambda function started");
    
    // get verso pn-delivery
    const url = process.env.PN_DELIVERY_URL.concat('/requests?');
    const attemptTimeout = `${process.env.ATTEMPT_TIMEOUT_SEC}` * 1000;
    const numRetry = `${process.env.NUM_RETRY}`;
    axiosRetry(axios, {
        retries: numRetry,
        shouldResetTimeout: true ,
        retryCondition: (error) => {
          return axiosRetry.isNetworkOrIdempotentRequestError(error) || error.code === 'ECONNABORTED';
        },
        onRetry: retryCallback,
        onMaxRetryTimesExceeded: retryTimesExceededCallback
      });

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
        headers["x-pagopa-pn-src-ch-details"] =
        event.requestContext.authorizer["sourceChannelDetails"];
    }
    if (event.requestContext.authorizer["uid"]) {
        headers["x-pagopa-pn-uid"] = event.requestContext.authorizer["uid"];
    }

    // notificationRequestId
    const notificationRequestId = event.queryStringParameters['notificationRequestId'];
    
    // paProtocolNumber && idempotenceToken
    const paProtocolNumber = event.queryStringParameters['paProtocolNumber'];
    const idempotenceToken = event.queryStringParameters['idempotenceToken'];

    let searchParams;

    if (notificationRequestId) {
        searchParams = new URLSearchParams({
            notificationRequestId: notificationRequestId
        });
    }

    if ( paProtocolNumber &&  idempotenceToken) {
        searchParams = new URLSearchParams({
            paProtocolNumber: paProtocolNumber,
            idempotenceToken: idempotenceToken
        });
    }

    let version = findRequestVersion(event);
    
    console.log ('calling ',url + searchParams);
    let response;
    let lastError = null;
    try {
        response = await axios.get(url, { params: searchParams, headers: headers , timeout: attemptTimeout});

        let finalVersionObject = response.data;
        switch(version) {
            case 10:
                finalVersionObject = transformFromV21ToV1(transformFromV23ToV21(transformFromV24ToV23(transformFromV25ToV24(response.data))));
                break;
            case 21:
                finalVersionObject = transformFromV23ToV21(transformFromV24ToV23(transformFromV25ToV24(response.data)));
                break;
            case 23:
                finalVersionObject = transformFromV24ToV23(transformFromV25ToV24(response.data));
                break;
            case 24:
                finalVersionObject = transformFromV25ToV24(response.data);
                break;
        }

        const ret = {
            statusCode: response.status,
            body: JSON.stringify(finalVersionObject),
        };
        return ret;
    } catch (error) {
        if (error instanceof ValidationException) {
            console.info("Validation Exception: ", error)
            return {
                statusCode: 400,
                body: JSON.stringify(generateProblem(400, error.message))
            }
        } else if(error.response) {
            console.log("risposta negativa: ", error.response.data);
            const ret = {
                statusCode: error.response.status,
                body: JSON.stringify(error.response.data)
            };
            return ret;
        }
        else {
            console.warn("Error on url " + url + searchParams, error)
            return {
                statusCode: 500,
                body: JSON.stringify(generateProblem(500, error.message))
            }
        }
    }

    function generateProblem(status, message) {
        return {
            status: status,
            errors: [
                {
                    code: message
                }
            ]
        }
    }

    function transformFromV25ToV24(responseV25) {
        console.log("transformFromV25ToV24");
        const responseV24 = { ...responseV25 };

        if (responseV25.errors && responseV25.errors.length > 0) {
            responseV24.errors = responseV25.errors.map(error => {
                const { recIndex, ...rest } = error;
                return rest;
            });
        }

        return responseV24;
    }

    function transformFromV24ToV23(responseV24) {
        console.log("transformFromV24ToV23");
        const responseV23 = responseV24;
        responseV23.additionalLanguages = undefined;
        return responseV23;
    }

    function transformFromV23ToV21(responseV23) {
        const responseV21 = responseV23;
        responseV21.vat = undefined;
        return responseV21;
    }

    function transformFromV21ToV1(responseV21) {

        const pagoPaIntMode = transformPagoPaIntMode(responseV21.pagoPaIntMode);

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
            pagoPaIntMode: pagoPaIntMode
        }

        return responseV1;
    }

    function transformPagoPaIntMode(intmode) {
        if (intmode != "SYNC" && intmode != "NONE") {
          throw new ValidationException("PagoPaIntMode value not supported");
        }
        return intmode;
      }

    function transformRecipientFromV21ToV1(recipientV21) {

        const digitalDomicileV1 = recipientV21.digitalDomicile ? transformDigitalDomicile(recipientV21.digitalDomicile) : undefined
        const physicalAddressV1 = transformPhysicalAddress(recipientV21.physicalAddress)

        let paymentV1 = undefined
        if (recipientV21.payments) {
            paymentV1 = recipientV21.payments.length > 0 ? transformPaymentFromV21ToV1(recipientV21.payments) : undefined
        }

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
            at: physicalAddress.at ? physicalAddress.at : undefined,
            address: physicalAddress.address,
            addressDetails: physicalAddress.addressDetails ? physicalAddress.addressDetails : undefined,
            zip: physicalAddress.zip,
            municipality: physicalAddress.municipality,
            municipalityDetails: physicalAddress.municipalityDetails ? physicalAddress.municipalityDetails : undefined,
            province: physicalAddress.province,
            foreignState: physicalAddress.foreignState,
        }
    }

    function transformPaymentFromV21ToV1(paymentsV21) {
        console.log("transformPaymentFromV21ToV1 - paymentsV21", paymentsV21);
        
        // max 2 pagamenti else throw exception
        if (paymentsV21.length > 2) {
            throw new ValidationException("Unable to map payments, more than 2");
        }
        // se una tipologia di pagamento presente é F24 errore
        if (paymentsV21.some( paymentV21 => paymentV21.f24 )) {
            throw new ValidationException("Unable to map payment f24 type");
        }

        // riempio noticeCode e in caso noticeCodeAlternative
        const paymentV1 = {
            noticeCode: paymentsV21[0].pagoPa.noticeCode,
            creditorTaxId: paymentsV21[0].pagoPa.creditorTaxId
        }

        if (paymentsV21.length > 1) {
            paymentV1.noticeCodeAlternative = paymentsV21[1].pagoPa.noticeCode;
        }

        if (paymentsV21[0].pagoPa.attachment) {
            paymentV1.pagoPaForm = {
                digests: {
                    sha256: paymentsV21[0].pagoPa.attachment.digests.sha256
                },
                contentType: paymentsV21[0].pagoPa.attachment.contentType,
                ref: {
                    key: paymentsV21[0].pagoPa.attachment.ref.key,
                    versionToken: paymentsV21[0].pagoPa.attachment.ref.versionToken
                }
            }
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

      function retryCallback(retryCount, error, requestConfig) {
        console.warn(`Retry num ${retryCount} - error:${error.message}`);
      }

      function retryTimesExceededCallback(error, retryCount) {
        console.warn(`Retries exceeded: ${retryCount} - error:${error.message}`);
      }
};