const axios = require("axios");
const RestClient = require('./app/services');
const defaultProblem = "Error executing request";

exports.handleEvent = async (event) => {
  try {
    // Recupero headers parameter
    const headerResult = getHeaderParameters(event);
    // Recupero consent
    const consentsResult = validateConsentsToAccept();
    await Promise.all(consentsResult.map(async (consent) => {
      let lastVersion = consent.version;
      if (!lastVersion) {
        lastVersion = await RestClient.getLastVersion(consent.consentType, headerResult.cxType);
      }
      await RestClient.putConsents(consent.consentType, lastVersion, headerResult.uid, headerResult.cxType);
    }));

    return { statusCode: 200, body: JSON.stringify({ result: "OK" }) }; //modifica qui per tornare la chiamata verso l'api richiesta

  } catch (error) {
    console.error(defaultProblem, error.message);
    return {
      statusCode: 500,
      body: JSON.stringify(generateProblem(500, defaultProblem))
    };
  }
};

function generateProblem(status, message) {
  return {
    status: status,
    errors: [
      {
        code: message
      }
    ]
  };
}

function getHeaderParameters(event) {
  console.log('try to get header parameters..');
  const headers = JSON.parse(JSON.stringify(event["headers"] || {}));
  //const headers = event["headers"] || {};
  const uid = headers["x-pagopa-pn-uid"];
  const cxType = headers["x-pagopa-pn-cx-type"];
  if (!uid || !cxType) {
    console.warn("Missing required headers: x-pagopa-pn-uid or x-pagopa-pn-cx-type");
    return {
      statusCode: 500,
      body: JSON.stringify(generateProblem(500, "Missing required headers: x-pagopa-pn-uid or x-pagopa-pn-cx-type"))
    };
  }
  return { uid, cxType };
}

function validateConsentsToAccept() {
  const envVar = process.env.CONSENTS_TO_ACCEPT;
  console.log("Checking if the env is set and recover the field consentType..");
  if (!envVar) {
    console.error("CONSENTS_TO_ACCEPT not set");
    return {
      statusCode: 500,
      body: JSON.stringify(generateProblem(500, defaultProblem))
    };
  }
  let consents;
  try {
    consents = JSON.parse(envVar);
    if (!Array.isArray(consents)) throw new Error("CONSENTS_TO_ACCEPT is not formatted as a json array");
    for (const consent of consents) {
      if (!consent.consentType) throw new Error("Each array element must have the consentType field");
    }
  } catch (error) {
    console.error("Errore parsing CONSENTS_TO_ACCEPT:", error.message);
    return {
      statusCode: 500,
      body: JSON.stringify(generateProblem(500, defaultProblem))
    };
  }
  return consents;
}
