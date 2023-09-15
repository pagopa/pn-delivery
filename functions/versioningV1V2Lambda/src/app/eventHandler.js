import http from 'http';


exports.versioning = async (event, context) => {

  console.log("TestLambdaProxy function started:", JSON.stringify(event), JSON.stringify(context));

  const IUN = event.pathParameters['iun'];

  const url = process.env.PN_DELIVERY_URL.concat('/notifications/sent/').concat(IUN);
  const CATEGORIES = ['SENDER_ACK_CREATION_REQUEST','VALIDATE_NORMALIZE_ADDRESSES_REQUEST','NORMALIZED_ADDRESS','REQUEST_ACCEPTED','SEND_COURTESY_MESSAGE','GET_ADDRESS','PUBLIC_REGISTRY_CALL','PUBLIC_REGISTRY_RESPONSE','SCHEDULE_ANALOG_WORKFLOW','SCHEDULE_DIGITAL_WORKFLOW','PREPARE_DIGITAL_DOMICILE','SEND_DIGITAL_DOMICILE','SEND_DIGITAL_FEEDBACK','SEND_DIGITAL_PROGRESS','REFINEMENT','SCHEDULE_REFINEMENT','DIGITAL_DELIVERY_CREATION_REQUEST','DIGITAL_SUCCESS_WORKFLOW','DIGITAL_FAILURE_WORKFLOW','ANALOG_SUCCESS_WORKFLOW','ANALOG_FAILURE_WORKFLOW','COMPLETELY_UNREACHABLE_CREATION_REQUEST','PREPARE_SIMPLE_REGISTERED_LETTER','SEND_SIMPLE_REGISTERED_LETTER','NOTIFICATION_VIEWED_CREATION_REQUEST','NOTIFICATION_VIEWED','PREPARE_ANALOG_DOMICILE','SEND_ANALOG_DOMICILE','SEND_ANALOG_PROGRESS','SEND_ANALOG_FEEDBACK','PAYMENT','COMPLETELY_UNREACHABLE','REQUEST_REFUSED','AAR_CREATION_REQUEST','AAR_GENERATION','NOT_HANDLED','SEND_SIMPLE_REGISTERED_LETTER_PROGRESS'].split(",");

  /*
  for(let index = 0; index < CATEGORIES.length; index ++){
    console.log("Elemento ", CATEGORIES[index]);
  };*/

  const headers = //event.headers;
  {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    'x-pagopa-pn-cx-groups': '63f359bc72337440a40f537e,63f35a3d72337440a40f537f,642d7dfa76f10c7617353c78,642d7e9e76f10c7617353c7a',
    'x-pagopa-pn-cx-id': '5b994d4a-0fa8-47ac-9c7b-354f1d44a1ce',
    'x-pagopa-pn-cx-role': 'admin',
    'x-pagopa-pn-cx-type': 'PA',
    'x-pagopa-pn-jti': '58d0f32d-f760-43ff-b615-9ec7d88f4a67',
    'x-pagopa-pn-src-ch': 'WEB',
    'x-pagopa-pn-src-ch-details': 'test',
    'x-pagopa-pn-uid': 'e9e4a9c7-9586-4b92-a7dd-ee1a0e77d398'
  };

  const options = {
    method: 'GET',
    headers
  };

  console.log ('calling ',url);
  return fetch(url, {
      method: "GET",
      headers: headers
  })
    .then(response => {
        console.log('risposta da fetch');

        if (response.ok){
          return response.json().then(res => {
              const transformedObject = transformObject(res);
              console.log('ritorno risposta trasformata ',transformedObject);

              const ret = {
                statusCode: 200,
                body: JSON.stringify(transformedObject)
              };
              return ret;
            });
        }else{
          console.log('risposta negativa: ', response);
          const err = {
            statusCode: response.status,
            body: response.statusText
          };
          //throw new Error(JSON.stringify(err));
          //context.fail(JSON.stringify(err));

          return err;
          //context.fail(JSON.stringify(err))
        }

    });


function transformObject(rest) {

  console.log('transformin object',JSON.stringify(rest));


  const notificationStatus_ENUM =  ['IN_VALIDATION','ACCEPTED','REFUSED','DELIVERING','DELIVERED','VIEWED','EFFECTIVE_DATE','PAID','UNREACHABLE','CANCELLED'];

  if ( !notificationStatus_ENUM.includes(rest.notificationStatus)){
    throw new Error("Status not supported");
  }

  const iun = rest.iun;
  const sentAt = rest.sentAt;
  const senderPaId = rest.senderPaId;
  const cancelledByIun = rest.cancelledByIun;
  const documentsAvailable = rest.documentsAvailable;
  const idempotenceToken = rest.idempotenceToken;
  const recipients= [];
  rest.recipients.forEach( r => recipients.push(transformRecipient(r)));


  const notificationStatus = rest.notificationStatus;
  const documents=[];
  rest.documents.forEach( d => documents.push(transformNotificationDocument(d)));

  const notificationFeePolicy = transformNotificationFeePolicy(rest.notificationFeePolicy);
  const physicalCommunicationType = transformPhysicalCommunicationType(rest.physicalCommunicationType);
  const pagoPaIntMode = transformPagoPaIntMode (rest.pagoPaIntMode);

  const timeline = rest.timeline.filter(tl => CATEGORIES.includes(tl.category));
  const timelineIds = [];
  for (const tl of timeline) timelineIds.push(tl.elementId);

  const notificationStatusHistory = rest.notificationStatusHistory.filter(nsh=>{
    let keep = true;
    for (const timelineElement of nsh.relatedTimelineElements){
      keep = keep && timelineIds.includes(timelineElement);
      if (!keep){
        console.log("Skipping timelineId ",timelineElement, timelineIds);
      }
    }
    return keep;
  });

  // Crea il nuovo oggetto risultante senza payments
  const newObject = {
    abstract: rest.abstract,
    subject: rest.subject,
    senderPaId: senderPaId,
    iun: iun,
    sentAt: sentAt,
    cancelledByIun: cancelledByIun,
    idempotenceToken: idempotenceToken,
    paProtocolNumber: rest.paProtocolNumber,
    documentsAvailable: documentsAvailable,
    notificationStatus: notificationStatus,
    recipients: recipients,
    documents: documents,
    notificationFeePolicy: notificationFeePolicy,
    cancelledIun: rest.cancelledIun,
    notificationStatusHistory: notificationStatusHistory,
    timeline: timeline,
    physicalCommunicationType: physicalCommunicationType,
    senderDenomination: rest.senderDenomination,
    senderTaxId: rest.senderTaxId,
    group: rest.group,
    amount: rest.amount,
    paymentExpirationDate: rest.paymentExpirationDate,
    taxonomyCode: rest.taxonomyCode,
    pagoPaIntMode: pagoPaIntMode
  };

  console.log('return transformed object ',newObject);

  return newObject;
}


  function transformRecipient(recipient){
    const recipientType = recipient.recipientType;
    if (recipientType !== 'PG' && recipientType !== 'PF') {
      return {};
    }

    const taxId = recipient.taxId;
    const denomination = recipient.denomination;
    const digitalDomicile = recipient.digitalDomicile ? transormDigitalAddress(recipient.digitalDomicile) : null
    const physicalAddress = recipient.physicalAddress ? transormPhysicalAddress(recipient.physicalAddress) : {};
    const payment = transformPayment(recipient.payment);

    const ret = {
      recipientType:recipientType,
      taxId: taxId,
      denomination: denomination,
      payment: payment
    };

    if (digitalDomicile){
      ret.digitalDomicile = digitalDomicile;
    }
    if (physicalAddress){
      ret.physicalAddress = physicalAddress;
    }

    return ret;

  }

  function transormDigitalAddress(address){
    if (!address.type || address.type != 'PEC') {
      console.log('ERROR transormDigitalAddress ', address);
      throw Error('address type not supported ');
    }

    return {
      type: address.type,
      address: address.address
    }
  }

  function transormPhysicalAddress(address){
    return {
      at: address.at,
      address: address.address,
      addressDetails: address.addressDetails,
      zip: address.zip,
      municipality: address.municipality,
      municipalityDetails: address.municipalityDetails,
      province: address.province,
      foreignState: address.foreignState
    }
  }

  function transformPayment(payment){

    return {
      noticeCode: payment.noticeCode,
      creditorTaxId: payment.creditorTaxId,
      noticeCodeAlternative: payment.noticeCodeAlternative,
      pagoPaForm: {
        digests: {
          sha256: payment.pagoPaForm.digests.sha256
        },
        contentType: payment.pagoPaForm.contentType,
        ref: {
          key: payment.pagoPaForm.ref.key,
          versionToken: payment.pagoPaForm.ref.versionToken
        }
      }
    }
  }

  function transformNotificationDocument(doc){
    const digests = {
      sha256 : doc.digests.sha256
    }

    const contentType = doc.contentType;
    const ref = {
      key: doc.ref.key,
      versionToken: doc.ref.versionToken
    };
    const title = doc.title;
    const docIdx = doc.docIdx;

    return {
      contentType: contentType,
      digests: digests,
      ref: ref,
      title: title,
      docIdx: docIdx
    }
  }

  function transformNotificationFeePolicy(policy){
    if (policy != 'FLAT_RATE' && policy != 'DELIVERY_MODE'){
      throw new Error("NotificationFeePolicy value not supported");
    }

    return policy;
  }

  function transformPhysicalCommunicationType(type){
    if (type != 'AR_REGISTERED_LETTER' && type != 'REGISTERED_LETTER_890'){
      throw new Error("PhysicalCommunicationType value not supported");
    }

    return type;
  }

  function transformPagoPaIntMode (intmode){
    if (intmode != 'SYNC' && intmode != 'NONE'){
      throw new Error("PagoPaIntMode value not supported");
    }
    return intmode;
  }
};
