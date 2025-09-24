const RestClient = require("./services");
const defaultProblem = "Error executing request";

exports.handle = async (event) => {
  try {
    // Recupero parametri dagli header
    const headerResult = getHeaderParameters(event);
    // Recupero consensi da accettare
    const consentsToAccept = validateConsentsToAccept();

    const promiseList = consentsToAccept.map(consent => acceptConsent(consent, headerResult));
    await Promise.all(promiseList);
    console.log("All consents accepted successfully.");
    // Chiamata API delivery dopo aver gestito i consensi
    return deliveryResponse = await RestClient.checkQrCode(event);
  } catch (error) {
    console.error("Error: ", error.message);
    return {
      statusCode: 500,
      body: JSON.stringify(generateProblem(500, defaultProblem)),
    };
  }
};

async function acceptConsent(consent, headerResult) {
  let lastVersion = consent.version;
  if (!lastVersion) {
    lastVersion = await RestClient.getLastVersion(
      consent.consentType,
      headerResult.cxType
    );
  }
  await RestClient.putConsents(
    consent.consentType,
    lastVersion,
    headerResult.uid,
    headerResult.cxType
  );
}

function generateProblem(status, message) {
  return {
    status: status,
    errors: [
      {
        code: message,
      },
    ],
  };
}

function getHeaderParameters(event) {
  console.log("Trying to get header parameters...");
  const headers = JSON.parse(JSON.stringify(event.headers || {}));
  const uid = headers["x-pagopa-pn-uid"];
  const cxType = headers["x-pagopa-pn-cx-type"];
  if (!uid || !cxType) {
    console.warn(
      "Missing required headers: x-pagopa-pn-uid or x-pagopa-pn-cx-type"
    );
    throw new Error("Missing required headers: x-pagopa-pn-uid or x-pagopa-pn-cx-type");
  }
  return { uid, cxType };
}

function validateConsentsToAccept() {
  const envVar = process.env.CONSENTS_TO_ACCEPT;
  console.log("Checking if the env is set and every element contains at least field consentType..");
  if (!envVar) {
    throw new Error("CONSENTS_TO_ACCEPT not set");
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
