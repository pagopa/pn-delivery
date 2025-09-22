const axios = require("axios");
const RestClient = require('./app/services');

exports.handleEvent = async (event) => {
  try {
    // Recupero headers parameter
    const headerResult = getHeaderParameters(event);
    // Recupero consent
    const consentsResult = validateConsentsToAccept();
    await Promise.all(consentsResult.map(async (consent) => {
      let lastVersion = consent.lastVersion;
      if (!lastVersion) {
        lastVersion = await RestClient.getLastVersion(consent.consentType, headerResult.cxType);
      }
      await RestClient.putConsents(consent.consentType, lastVersion, headerResult.uid, headerResult.cxType);
    }));

    return { statusCode: 200, body: JSON.stringify({ result: "OK" }) };

  } catch (error) {
    console.error("Errore:", error.message);
    return {
      statusCode: 500,
      body: JSON.stringify(generateProblem(500, error.message))
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
    console.warn("Header x-pagopa-pn-uid o x-pagopa-pn-cx-type mancante");
    return {
      statusCode: 400,
      body: JSON.stringify(generateProblem(400, "Missing required headers: x-pagopa-pn-uid or x-pagopa-pn-cx-type"))
    };
  }
  return { uid, cxType };
}

function validateConsentsToAccept() {
  const envVar = process.env.CONSENTS_TO_ACCEPT;
  console.log("Checking if the env is set and recover the field consentType..");
  if (!envVar) {
    console.error("CONSENTS_TO_ACCEPT non settata");
    return {
      statusCode: 500,
      body: JSON.stringify(generateProblem(500, "CONSENTS_TO_ACCEPT non settata"))
    };
  }
  let consents;
  try {
    consents = JSON.parse(envVar);
    if (!Array.isArray(consents)) throw new Error("CONSENTS_TO_ACCEPT non è un array");
    for (const consent of consents) {
      if (!consent.consentType) throw new Error("Ogni elemento deve avere consentType");
    }
  } catch (error) {
    console.error("Errore parsing CONSENTS_TO_ACCEPT:", error.message);
    return {
      statusCode: 500,
      body: JSON.stringify(generateProblem(500, "CONSENTS_TO_ACCEPT non valida: " + error.message))
    };
  }
  return consents;
}
