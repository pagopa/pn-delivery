const RestClient = require("./services");
const { getUserInfoFromEvent, retrieveOnlyLollipopHeaders } = require("./utils");
const defaultProblem = "Error executing request";

exports.handle = async (event) => {
  try {
    const userInfo = getUserInfoFromEvent(event);
    const consentsToAccept = validateConsentsToAccept();

    const promiseList = consentsToAccept.map(consent => acceptConsent(consent, userInfo));
    await Promise.all(promiseList);
    console.log("All consents accepted successfully.");
    const lollipopHeaders = retrieveOnlyLollipopHeaders(event.headers || {});
    return deliveryResponse = await RestClient.checkQrCode(event.body, lollipopHeaders, userInfo);
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
