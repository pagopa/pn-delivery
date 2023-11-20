// converte la risposta V2.0 a V1 e verrà estesa per convertire V2.1 a V1
const {ValidationException} = require("./exceptions.js");

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

  console.log("Versioning_V1-V2_GetNotification_Lambda function started");

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
    "SEND_SIMPLE_REGISTERED_LETTER_PROGRESS",
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
    "PROBABLE_ANALOG_SCHEDULING_DATE"
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
    if (error instanceof ValidationException) {
      console.info("Validation Exception: ", error)
      return {
        statusCode: 400,
        body: JSON.stringify(generateProblem(400, error.message))
      }
    } else {
      console.warn("Error on url " + url, error)
      return {
        statusCode: 500,
        body: JSON.stringify(generateProblem(502, error.message))
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

  function transformObject(responseV2) {

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
      throw new ValidationException("Status not supported");
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
        for (const relatedTimelineElement of nsh.relatedTimelineElements) {
          keep = timelineIds.includes(relatedTimelineElement);
          if (!keep) {
            console.log("NotificationStatusHistory - skipping status:", nsh.status, "caused by relatedTimelineElement:", relatedTimelineElement);
            return keep;
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
    const payment = recipient.payment ? transformPayment(recipient.payment) : undefined;

    const ret = {
      recipientType: recipientType,
      taxId: taxId,
      denomination: denomination,
      payment: payment,
    };

    if (digitalDomicile) {
      ret.digitalDomicile = digitalDomicile;
    }
    if (physicalAddress) {
      ret.physicalAddress = physicalAddress;
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

  function transformPayment(payment) {
    return {
      noticeCode: payment.noticeCode,
      creditorTaxId: payment.creditorTaxId,
      noticeCodeAlternative: payment.noticeCodeAlternative,
      pagoPaForm: payment.pagoPaForm ? 
      {
        digests: {
          sha256: payment.pagoPaForm.digests?.sha256,
        },
        contentType: payment.pagoPaForm.contentType,
        ref: {
          key: payment.pagoPaForm.ref.key,
          versionToken: payment.pagoPaForm.ref?.versionToken,
        },
      } : undefined,
    };
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
};
