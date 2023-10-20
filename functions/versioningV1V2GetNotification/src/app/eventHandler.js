// converte la risposta V2.1 a V1

exports.versioning = async (event, context) => {
  const path = "/notifications/sent/";

  if (
    event["resource"] !== `${path}{iun}` ||
    !event["path"].startsWith("/delivery/") ||
    event["httpMethod"].toUpperCase() !== "GET"
  ) {
    console.log(
      "ERROR ENDPOINT ERRATO: {resource, path, httpMethod} ",
      event["resource"],
      event["path"],
      event["httpMethod"]
    );
    const err = {
      statusCode: 502,
      body: "ENDPOINT ERRATO",
    };

    return err;
  }

  console.log("Versioning_V1-V21_GetNotification_Lambda function started");

  const IUN = event.pathParameters["iun"];

  const url = `${process.env.PN_DELIVERY_URL}${path}${IUN}`;

  const CATEGORIES = [
    "SENDER_ACK_CREATION_REQUEST",
    "VALIDATE_NORMALIZE_ADDRESSES_REQUEST",
    "NORMALIZED_ADDRESS",
    "REQUEST_ACCEPTED",
    "SEND_COURTESY_MESSAGE",
    "GET_ADDRESS",
    "PUBLIC_REGISTRY_CALL",
    "PUBLIC_REGISTRY_RESPONSE",
    "SCHEDULE_ANALOG_WORKFLOW",
    "SCHEDULE_DIGITAL_WORKFLOW",
    "PREPARE_DIGITAL_DOMICILE",
    "SEND_DIGITAL_DOMICILE",
    "SEND_DIGITAL_FEEDBACK",
    "SEND_DIGITAL_PROGRESS",
    "REFINEMENT",
    "SCHEDULE_REFINEMENT",
    "DIGITAL_DELIVERY_CREATION_REQUEST",
    "DIGITAL_SUCCESS_WORKFLOW",
    "DIGITAL_FAILURE_WORKFLOW",
    "ANALOG_SUCCESS_WORKFLOW",
    "ANALOG_FAILURE_WORKFLOW",
    "COMPLETELY_UNREACHABLE_CREATION_REQUEST",
    "PREPARE_SIMPLE_REGISTERED_LETTER",
    "SEND_SIMPLE_REGISTERED_LETTER",
    "NOTIFICATION_VIEWED_CREATION_REQUEST",
    "NOTIFICATION_VIEWED",
    "PREPARE_ANALOG_DOMICILE",
    "SEND_ANALOG_DOMICILE",
    "SEND_ANALOG_PROGRESS",
    "SEND_ANALOG_FEEDBACK",
    "PAYMENT",
    "COMPLETELY_UNREACHABLE",
    "REQUEST_REFUSED",
    "AAR_CREATION_REQUEST",
    "AAR_GENERATION",
    "NOT_HANDLED",
    "SEND_SIMPLE_REGISTERED_LETTER_PROGRESS",
  ];

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

  console.log("calling ", url);
  let response;
  try {
    response = await fetch(url, { method: "GET", headers: headers });
    let responseV2 = await response.json();
    if (response.ok) {
      const transformedObject = transformObject(responseV2);
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
      body: JSON.stringify(responseV2),
    };
    return ret;
  } catch (error) {
    const ret = {
      statusCode: 400,
      body: error,
    };
    return ret;
  }

  function transformObject(responseV2) {
    console.log("transforming object", JSON.stringify(responseV2));

    const notificationStatus_ENUM = [
      "IN_VALIDATION",
      "ACCEPTED",
      "REFUSED",
      "DELIVERING",
      "DELIVERED",
      "VIEWED",
      "EFFECTIVE_DATE",
      "PAID",
      "UNREACHABLE",
      "CANCELLED",
    ];

    if (!notificationStatus_ENUM.includes(responseV2.notificationStatus)) {
      throw new Error("Status not supported");
    }

    const iun = responseV2.iun;
    const sentAt = responseV2.sentAt;
    const senderPaId = responseV2.senderPaId;
    const cancelledByIun = responseV2.cancelledByIun;
    const documentsAvailable = responseV2.documentsAvailable;
    const idempotenceToken = responseV2.idempotenceToken;
    const recipients = [];
    responseV2.recipients.forEach((r) =>
      recipients.push(transformRecipient(r))
    );

    const notificationStatus = responseV2.notificationStatus;
    const documents = [];
    responseV2.documents.forEach((d) =>
      documents.push(transformNotificationDocument(d))
    );

    const notificationFeePolicy = transformNotificationFeePolicy(
      responseV2.notificationFeePolicy
    );
    const physicalCommunicationType = transformPhysicalCommunicationType(
      responseV2.physicalCommunicationType
    );
    const pagoPaIntMode = transformPagoPaIntMode(responseV2.pagoPaIntMode);

    const timeline = responseV2.timeline.filter((tl) =>
      CATEGORIES.includes(tl.category)
    );
    const timelineIds = [];
    for (const tl of timeline) timelineIds.push(tl.elementId);

    // elimina dalla status hostory tutti gli elementi che includono come related timeline
    // elements elementi non sono presenti nella timeline
    const notificationStatusHistory =
      responseV2.notificationStatusHistory.filter((nsh) => {
        let keep = true;
        for (const timelineElement of nsh.relatedTimelineElements) {
          keep = keep && timelineIds.includes(timelineElement);
          if (!keep) {
            console.log("Skipping timelineId ", timelineElement, timelineIds);
          }
        }
        return keep;
      });

    // Crea il nuovo oggetto risultante senza payments
    const responseV1 = {
      abstract: responseV2.abstract,
      subject: responseV2.subject,
      senderPaId: senderPaId,
      iun: iun,
      sentAt: sentAt,
      cancelledByIun: cancelledByIun,
      idempotenceToken: idempotenceToken,
      paProtocolNumber: responseV2.paProtocolNumber,
      documentsAvailable: documentsAvailable,
      notificationStatus: notificationStatus,
      recipients: recipients,
      documents: documents,
      notificationFeePolicy: notificationFeePolicy,
      cancelledIun: responseV2.cancelledIun,
      notificationStatusHistory: notificationStatusHistory,
      timeline: timeline,
      physicalCommunicationType: physicalCommunicationType,
      senderDenomination: responseV2.senderDenomination,
      sourceChannelDetails: responseV2.sourceChannelDetails,
      senderTaxId: responseV2.senderTaxId,
      group: responseV2.group,
      amount: responseV2.amount,
      paymentExpirationDate: responseV2.paymentExpirationDate,
      taxonomyCode: responseV2.taxonomyCode,
      pagoPaIntMode: pagoPaIntMode,
    };

    console.log("return transformed object ", responseV1);

    return responseV1;
  }

  function transformRecipient(recipient) {
    const recipientType = recipient.recipientType;
    if (recipientType !== "PG" && recipientType !== "PF") {
      return {};
    }

    const taxId = recipient.taxId;
    const denomination = recipient.denomination;
    const digitalDomicile = recipient.digitalDomicile ? transformDigitalAddress(recipient.digitalDomicile) : undefined;
    const physicalAddress = recipient.physicalAddress ? transformPhysicalAddress(recipient.physicalAddress) : undefined;

    let paymentV1 = undefined;
    if(recipient.payments) {
      paymentV1 = recipient.payments.length > 0 ? transformPaymentFromV21ToV1(recipient.payments) : undefined;
    }

    const ret = {
      recipientType: recipientType,
      taxId: taxId,
      denomination: denomination
    };

    if (digitalDomicile) {
      ret.digitalDomicile = digitalDomicile;
    }
    if (physicalAddress) {
      ret.physicalAddress = physicalAddress;
    }
    if(paymentV1) {
      ret.payment = paymentV1;
  }

    return ret;
  }

  function transformDigitalAddress(address) {
    if (!address.type || address.type != "PEC") {
      console.log("ERROR transformDigitalAddress ", address);
      throw Error("address type not supported ");
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
      throw new Error("Unable to map payments, more than 2");
    }
    // se una tipologia di pagamento presente é F24 errore
    if (paymentsV21.some( paymentV21 => paymentV21.f24 )) {
      throw new Error("Unable to map payment f24 type");
    }
    // allegati di pagamento devono essere uguali (stesso sha) else throw exception
    if ( paymentsV21.length > 1 && paymentsV21[0].pagoPa.attachment && paymentsV21[1].pagoPa.attachment &&
       paymentsV21[0].pagoPa.attachment.digests.sha256 !== paymentsV21[1].pagoPa.attachment.digests.sha256 ) {
      throw new Error("Unable to map payments with different attachment");
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

  function transformNotificationFeePolicy(policy) {
    if (policy != "FLAT_RATE" && policy != "DELIVERY_MODE") {
      throw new Error("NotificationFeePolicy value not supported");
    }

    return policy;
  }

  function transformPhysicalCommunicationType(type) {
    if (type != "AR_REGISTERED_LETTER" && type != "REGISTERED_LETTER_890") {
      throw new Error("PhysicalCommunicationType value not supported");
    }

    return type;
  }

  function transformPagoPaIntMode(intmode) {
    if (intmode != "SYNC" && intmode != "NONE") {
      throw new Error("PagoPaIntMode value not supported");
    }
    return intmode;
  }
};
