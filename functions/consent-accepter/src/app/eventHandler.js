const RestClient = require("./services");
const defaultProblem = "Error executing request";

exports.handle = async (event) => {
  try {
    const userInfo = getUserInfoFromEvent(event);
    const consentsToAccept = validateConsentsToAccept();

    const promiseList = consentsToAccept.map(consent => acceptConsent(consent, userInfo));
    await Promise.all(promiseList);
    console.log("All consents accepted successfully.");
    return deliveryResponse = await RestClient.checkQrCode(event, userInfo);
  } catch (error) {
    console.error("Error: ", error.message);
    return {
      statusCode: 500,
      body: JSON.stringify(generateProblem(500, defaultProblem)),
    };
  }
};

async function acceptConsent(consent, userInfo) {
  let lastVersion = consent.version;
  if (!lastVersion) {
    lastVersion = await RestClient.getLastVersion(
      consent.consentType,
      userInfo.cxType
    );
  }
  await RestClient.putConsents(
    consent.consentType,
    lastVersion,
    userInfo.uid,
    userInfo.cxType
  );
}

function getUserInfoFromEvent(event) {
  console.log("Trying to get user info from event...");
  const requestContext = event.requestContext || {};
  const authorizer = requestContext.authorizer || {};
  const headers = event.headers || {};
  const cxId = authorizer["cx_id"];
  const cxType = authorizer["cx_type"];
  const taxId = headers["x-pagopa-cx-taxid"];
  checkRequiredUserInfo([
    { name: "cxId", value: cxId },
    { name: "cxType", value: cxType },
    { name: "taxId", value: taxId }
  ]);
  const uid = removeCxPrefix(cxId);
  return { uid, cxType, cxId, taxId };
}

function checkRequiredUserInfo(userInfoData) {
  let missingFields = "";
  for(const field of userInfoData) {
    if(!field.value){
      missingFields += field.name + " ";
    }
  }
  if (missingFields !== "") {
    throw new Error("Missing required info: " + missingFields);
  }
}

function removeCxPrefix(cxId) {
  return cxId.replace(/^(PF|PG|PA)-/, "");
}

function validateConsentsToAccept() {
  const envVar = process.env.CONSENTS_TO_ACCEPT;
  console.log("Checking if the env is set and every element contains at least field consentType.");
  if (!envVar) {
    throw new Error("CONSENTS_TO_ACCEPT env not set");
  }
  let consents = JSON.parse(envVar);
  if (!Array.isArray(consents))
    throw new Error("CONSENTS_TO_ACCEPT is not formatted as a json array");
  for (const consent of consents) {
    if (!consent.consentType)
      throw new Error("Each array element must have the consentType field");
  }
  return consents;
}

function generateProblem(status, message) {
  return {
    type: "GENERIC_ERROR",
    status: status,
    title: "Handled error",
    timestamp: new Date().toISOString(),
    errors: [
      {
        code: "INTERNAL_ERROR",
        element: null,
        detail: message,
      },
    ],
  };
}
