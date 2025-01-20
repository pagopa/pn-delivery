const {ValidationException} = require("../exceptions.js");

exports.transformFromV21ToV20 = function (responseV21) {
  console.log("transformFromV21ToV20")
  // eliminazione multipagamento F24
  let responseV20 = responseV21;
  responseV20.paFee = undefined;
  
  const recipientsV20 = [];
  responseV20.recipients.forEach((r) =>
  recipientsV20.push(transformRecipient(r))
  );
  responseV20.recipients = recipientsV20;
  
  const documentsV20 = [];
  responseV20.documents.forEach((d) =>
  documentsV20.push(transformNotificationDocument(d))
  );
  responseV20.documents = documentsV20;
  
  responseV20.notificationFeePolicy = transformNotificationFeePolicy(responseV20.notificationFeePolicy);
  responseV20.physicalCommunicationType = transformPhysicalCommunicationType(responseV20.physicalCommunicationType);
  responseV20.pagoPaIntMode = transformPagoPaIntMode(responseV20.pagoPaIntMode);        
  responseV20.notificationStatus = adjustedTimelineAndHistory.history[adjustedTimelineAndHistory.history.length -1].status;

  //responseV21.timeline = adjustTimeline( responseV21.timeline, CATEGORY_TO_EXCLUDE, null);

  return responseV20;
}

function transformRecipient(recipient) {
  const recipientType = recipient.recipientType;
  if (recipientType !== "PG" && recipientType !== "PF") {
    return {};
  }
  
  const taxId = recipient.taxId;
  const denomination = recipient.denomination;
  const digitalDomicile = recipient.digitalDomicile
  ? transformDigitalAddress(recipient.digitalDomicile)
  : undefined;
  const physicalAddress = recipient.physicalAddress
  ? transformPhysicalAddress(recipient.physicalAddress)
  : undefined;
  
  let paymentV1 = undefined;
  if (recipient.payments) {
    paymentV1 =
    recipient.payments.length > 0
    ? transformPaymentFromV21ToV1(recipient.payments)
    : undefined;
  }
  
  const ret = {
    recipientType: recipientType,
    taxId: taxId,
    denomination: denomination,
  };
  
  if (digitalDomicile) {
    ret.digitalDomicile = digitalDomicile;
  }
  if (physicalAddress) {
    ret.physicalAddress = physicalAddress;
  }
  if (paymentV1) {
    ret.payment = paymentV1;
  }
  
  return ret;
}

function transformDigitalAddress(address) {
  if (!address.type || address.type != "PEC") {
    throw new ValidationException("Address type not supported ");
  }
  
  return {
    type: address.type,
    address: address.address,
  };
}

function transformPhysicalAddress(address) {
  return {
    at: address.at,
    address: address.address,
    addressDetails: address.addressDetails,
    zip: address.zip,
    municipality: address.municipality,
    municipalityDetails: address.municipalityDetails,
    province: address.province,
    foreignState: address.foreignState,
  };
}

function transformPaymentFromV21ToV1(paymentsV21) {
  console.log("transformPaymentFromV21ToV1 - paymentsV21", paymentsV21);
  
  // max 2 pagamenti else throw exception
  if (paymentsV21.length > 2) {
    throw new ValidationException("Unable to map payments, more than 2");
  }
  // se una tipologia di pagamento presente é F24 errore
  if (paymentsV21.some((paymentV21) => paymentV21.f24)) {
    throw new ValidationException("Unable to map payment f24 type");
  }
  
  // riempio noticeCode e in caso noticeCodeAlternative
  const paymentV1 = {
    noticeCode: paymentsV21[0].pagoPa.noticeCode,
    creditorTaxId: paymentsV21[0].pagoPa.creditorTaxId,
  };
  
  if (paymentsV21.length > 1) {
    paymentV1.noticeCodeAlternative = paymentsV21[1].pagoPa.noticeCode;
  }
  
  if (paymentsV21[0].pagoPa.attachment) {
    paymentV1.pagoPaForm = {
      digests: {
        sha256: paymentsV21[0].pagoPa.attachment.digests.sha256,
      },
      contentType: paymentsV21[0].pagoPa.attachment.contentType,
      ref: {
        key: paymentsV21[0].pagoPa.attachment.ref.key,
        versionToken: paymentsV21[0].pagoPa.attachment.ref.versionToken,
      },
    };
  }
  return paymentV1;
}

function transformNotificationDocument(doc) {
  const digests = {
    sha256: doc.digests?.sha256,
  };
  
  const contentType = doc.contentType;
  const ref = doc.ref
  ? {
    key: doc.ref.key,
    versionToken: doc.ref.versionToken,
  }
  : undefined;
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

function transformNotificationFeePolicy(policy) {
  if (policy != "FLAT_RATE" && policy != "DELIVERY_MODE") {
    throw new ValidationException("NotificationFeePolicy value not supported");
  }
  
  return policy;
}

function transformPhysicalCommunicationType(type) {
  if (type != "AR_REGISTERED_LETTER" && type != "REGISTERED_LETTER_890") {
    throw new ValidationException("PhysicalCommunicationType value not supported");
  }
  
  return type;
}

function transformPagoPaIntMode(intmode) {
  if (intmode != "SYNC" && intmode != "NONE") {
    throw new ValidationException("PagoPaIntMode value not supported");
  }
  return intmode;
}