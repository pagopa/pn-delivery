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

exports.validateNewNotification = function(newNotificationRequestV1){
  console.log("validateNewNotification - newNotificationRequestV1 ", newNotificationRequestV1);
  const errors = []

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
  
  return errors;
}

function fromRecipientV1ToRecipientV21(recipientV1, applyCostFlag) {
  console.log("fromRecipientV1ToRecipientV21 - recipientV1 ", recipientV1)
  
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
  
  console.log("fromRecipientV1ToRecipientV21 - recipientV21 ", recipientV21);
  
  return recipientV21;
  
}

function createDigitalDomicile(digitalDomicile) {
  console.log("createDigitalDomicile - digitalDomicile ", digitalDomicile);
  return {
    type: digitalDomicile.type,
    address: digitalDomicile.address,
  };
}

function createPhysicalAddress(physicalAddress) {
  console.log("createPhysicalAddress - physicalAddress ", physicalAddress);
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
      attachment: paymentAttachment
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

exports.createNewNotificationRequesV21 = function(newNotificationRequestV1){
  console.log("createNewNotificationRequesV21 - newNotificationRequestV1 ", newNotificationRequestV1);

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
  
  console.log("createNewNotificationRequesV21 - newNotificationRequestV21 ", newNotificationRequestV21);
  
  
  return newNotificationRequestV21
};