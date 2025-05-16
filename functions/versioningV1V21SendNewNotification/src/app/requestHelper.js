exports.validateRequest = function(event){
  const { path, httpMethod, body } = event
  const errors = []
  if(httpMethod==='POST' && path && path.startsWith('/delivery/') && body){
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

exports.findRequestVersion = function(event) {
  // a partire dalla versione 1.0 trasformare la request alla versione piú recente sul ms
  let version = 10;

  // a partire dalla versione 2.1 trasformare la request alla versione piú recente sul ms
  if (event["path"].startsWith("/delivery/v2.1/")) {
      version = 21;
  }

  // a partire dalla versione 2.3 trasformare la request alla versione piú recente sul ms
  // NB: sebbene (a oggi) la 2.3 non passa di qua, in futuro potrebbe e quindi si è già implementata
  // la logica di traduzione (che probabilmente andrà aggiornata nel futuro)
  if (event["path"].startsWith("/delivery/v2.3/")) {
      version = 23;
  }

  if (event["path"].startsWith("/delivery/v2.4/")) {
    version = 24;
}
  return version;
}

exports.validateNewNotification = function(newNotificationRequest, requestVersion) {
  switch(requestVersion) {
    case 10:
      return validateNewNotificationV1(newNotificationRequest);
    case 21:
      return validateNewNotificationV21(newNotificationRequest);
    case 23:
    case 24:
      return validateNewNotificationV24(newNotificationRequest);
    default:
      return newNotificationRequest;
  }
}

function validateNewNotificationV1(newNotificationRequestV1) {
  const errors = []

  if (!newNotificationRequestV1.pagoPaIntMode){
    newNotificationRequestV1.pagoPaIntMode = 'NONE';
    if (newNotificationRequestV1.notificationFeePolicy === 'DELIVERY_MODE'
        ){
            newNotificationRequestV1.recipients.forEach( recipient => {
              if (recipient.payment){
                newNotificationRequestV1.pagoPaIntMode = 'SYNC';
              }
            });
        
    }
  }

  if (newNotificationRequestV1.pagoPaIntMode != 'SYNC' && newNotificationRequestV1.pagoPaIntMode != 'NONE') {
    errors.push('Invalid pagoPaIntMode')
    return errors;
  }

  newNotificationRequestV1.recipients.forEach(recipient => {
    if (recipient.payment && recipient.payment.noticeCodeAlternative && recipient.payment.noticeCodeAlternative == recipient.payment.noticeCode) {
      console.log("noticeCodeAlternative ", recipient.payment.noticeCodeAlternative)
      console.log("noticeCode ", recipient.payment.noticeCode)

      errors.push('Alternative notice code equals to notice code')
      return errors
    }
  });

  checkPhysicalAddress(newNotificationRequestV1, errors);
  return errors;
}

function validateNewNotificationV21(newNotificationRequestV21) {
  const errors = []
  if(newNotificationRequestV21.notificationFeePolicy === 'DELIVERY_MODE' &&
   (newNotificationRequestV21.pagoPaIntMode === 'ASYNC' || haveF24Payment(newNotificationRequestV21)) ) {
    // controlla presenza vat e paFee
    if (!newNotificationRequestV21.paFee || !newNotificationRequestV21.vat) {
      errors.push('Vat and paFee fields are required');
    }
  }

  checkPhysicalAddress(newNotificationRequestV21, errors);
  return errors;
}

function checkPhysicalAddress(newNotificationRequest, errors) {
  let missingPhysicalAddress = newNotificationRequest.recipients.some(recipient => !recipient.physicalAddress);
  if (missingPhysicalAddress) {
    errors.push("Validation errors: [object has missing required properties ([\"physicalAddress\"])]");
  }
}

function validateNewNotificationV24(newNotificationRequestV24) { 
  const errors = []
  checkPhysicalAddress(newNotificationRequestV24, errors);
  return errors;
}

function haveF24Payment(newNotificationRequestV21) {
  let haveSomeF24 = false;
  newNotificationRequestV21.recipients.forEach(recipient => {
    if (recipient.payments) {
      recipient.payments.forEach(payment => {
        if(payment.f24) {
          return haveSomeF24 = true;
        }
      })
    }
  })
  return haveSomeF24;
}

function fromRecipientV1ToRecipientV21(recipientV1, applyCostFlag) {
  const digitalDomicileV21 = createDigitalDomicile(recipientV1.digitalDomicile);
  const physicalAddressV21 = createPhysicalAddress(recipientV1.physicalAddress);
  const paymentsV21 = fromPaymentV1toPaymentsV21(recipientV1.payment, applyCostFlag);
  
  
  const recipientV21 = {
    recipientType: recipientV1.recipientType,
    taxId: recipientV1.taxId,
    denomination: recipientV1.denomination,
    digitalDomicile: digitalDomicileV21,
    physicalAddress: physicalAddressV21
  }

  if (paymentsV21) {
    recipientV21.payments = paymentsV21;
  }
  
  return recipientV21;
  
}

function createDigitalDomicile(digitalDomicile) {
  if (!digitalDomicile) return null;
  return {
    type: digitalDomicile.type,
    address: digitalDomicile.address,
  };
}

function createPhysicalAddress(physicalAddress) {
  return {
    at: physicalAddress.at,
    address: physicalAddress.address,
    addressDetails: physicalAddress.addressDetails,
    zip: physicalAddress.zip,
    municipality: physicalAddress.municipality,
    municipalityDetails: physicalAddress.municipalityDetails,
    province: physicalAddress.province,
    foreignState: physicalAddress.foreignState,
  };
}

function fromPaymentV1toPaymentsV21(paymentV1, applyCostFlag) {
  if (!paymentV1) return null;
  console.log("fromPaymentV1toPaymentsV21 - paymentV1 ", paymentV1);
  
  const paymentAttachment = fromPagoPaFormToPaymentAttachment(paymentV1.pagoPaForm)
  
  const paymentsV21 = [];
  const pagoPaPayment = {
    noticeCode: paymentV1.noticeCode,
    creditorTaxId: paymentV1.creditorTaxId,
    applyCost: applyCostFlag,
    attachment: paymentAttachment
  };
  const paymentV21 = {
    pagoPa: pagoPaPayment
  };
  
  paymentsV21.push(paymentV21);
  
  if (paymentV1.noticeCodeAlternative) {
    const pagoPaPaymentAlternative = {
      noticeCode: paymentV1.noticeCodeAlternative,
      creditorTaxId: paymentV1.creditorTaxId,
      applyCost: applyCostFlag,
    };
    const paymentV21Alternative = {
      pagoPa: pagoPaPaymentAlternative
    };
    
    paymentsV21.push(paymentV21Alternative);
  }
  
  console.log("fromPaymentV1toPaymentsV21 - paymentsV21 ", paymentsV21);
  
  return paymentsV21;
  
}

function fromPagoPaFormToPaymentAttachment(pagoPaForm) {
  if(!pagoPaForm) return null;
  console.log("fromPagoPaFormToPaymentAttachment - pagoPaForm ", pagoPaForm);
  const pagoPaFormDigests = {
    sha256: pagoPaForm.digests.sha256
  };
  
  const pagoPaFormRef = {
    key: pagoPaForm.ref.key,
    versionToken: pagoPaForm.ref.versionToken
  };
  
  const paymentAttachment = {
    digests: pagoPaFormDigests,
    contentType: pagoPaForm.contentType,
    ref: pagoPaFormRef
  };
  
  console.log("fromPagoPaFormToPaymentAttachment - paymentAttachment ", paymentAttachment);
  
  return paymentAttachment;
}

// TODO: Mettere a fattor comune
function transformNotificationDocument(doc) {
  const digests = {
    sha256: doc.digests.sha256,
  };
  
  const contentType = doc.contentType;
  const ref = {
    key: doc.ref.key,
    versionToken: doc.ref.versionToken,
  };
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

exports.fromNewNotificationRequestV21ToV23 = function(newNotificationRequestV21){
  let newNotificationRequestV23 = newNotificationRequestV21;
  // se non presente metto default
  // N.B. anche per la tipologia FLAT_RATE viene aggiunto default anche se non utilizzato al momento
  newNotificationRequestV23.paFee = newNotificationRequestV21.paFee? newNotificationRequestV21.paFee : 100;
  newNotificationRequestV23.vat = newNotificationRequestV21.vat? newNotificationRequestV21.vat : 22;
  return newNotificationRequestV23;
}

exports.fromNewNotificationRequestV1ToV21 = function(newNotificationRequestV1){
  let applyCostFlag = false;
  if (newNotificationRequestV1.notificationFeePolicy === 'DELIVERY_MODE') {
    applyCostFlag = true
  }

  const recipientsV21 = [];
  newNotificationRequestV1.recipients.forEach(recipient => {
    recipientsV21.push(fromRecipientV1ToRecipientV21(recipient, applyCostFlag))
  });
  
  const documents = [];
  newNotificationRequestV1.documents.forEach(doc => {
    documents.push(transformNotificationDocument(doc))
  });
  
  // creazione oggetto NewNotificationRequestV21
  const newNotificationRequestV21 = {
    idempotenceToken: newNotificationRequestV1.idempotenceToken,
    paProtocolNumber: newNotificationRequestV1.paProtocolNumber,
    subject: newNotificationRequestV1.subject,
    abstract: newNotificationRequestV1.abstract,
    recipients: recipientsV21,
    documents: documents,
    notificationFeePolicy: newNotificationRequestV1.notificationFeePolicy,
    cancelledIun: newNotificationRequestV1.cancelledIun,
    physicalCommunicationType: newNotificationRequestV1.physicalCommunicationType,
    senderDenomination: newNotificationRequestV1.senderDenomination,
    senderTaxId: newNotificationRequestV1.senderTaxId,
    group: newNotificationRequestV1.group,
    amount: newNotificationRequestV1.amount,
    paymentExpirationDate: newNotificationRequestV1.paymentExpirationDate,
    taxonomyCode: newNotificationRequestV1.taxonomyCode,
    paFee: newNotificationRequestV1.paFee,
    pagoPaIntMode: newNotificationRequestV1.pagoPaIntMode
  }
    
  
  return newNotificationRequestV21
};