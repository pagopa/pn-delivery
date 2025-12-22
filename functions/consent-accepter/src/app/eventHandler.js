const RestClient = require("./services");
const { getUserInfoFromEvent, retrieveHeadersToForward } = require("./utils");
const defaultProblem = "Error executing request";
const CacheManager = require('./cache/CacheManager');
const logger = require("./logger");

/**
 * Inizializza il CacheManager con configurazione
 */
const cacheManager = new CacheManager({
  secondsTTL: parseInt(process.env.CACHE_ITEM_TTL_SECONDS),
  externalFetcher: RestClient.getLastVersion
});

exports.handle = async (event) => {
  try {
    const userInfo = getUserInfoFromEvent(event);
    const consentsToAccept = validateConsentsToAccept();
    await cacheManager.connect();
    const promiseList = consentsToAccept.map(consent => acceptConsent(consent, userInfo));
    await Promise.all(promiseList);
    logger.info("All consents accepted successfully.");
    const headersToForward = retrieveHeadersToForward(event.headers || {});
    return deliveryResponse = await RestClient.checkQrCode(event.body, headersToForward, userInfo);
  } catch (error) {
    logger.error("Error: ", error.message);
    return {
      statusCode: 500,
      body: JSON.stringify(generateProblem(500, defaultProblem)),
    };
  } finally {
    await cacheManager.disconnect();
  }
};

async function acceptConsent(consent, userInfo) {
  let lastVersion = consent.version;
  if (!lastVersion) {
    lastVersion = await cacheManager.get(userInfo.cxType, consent.consentType);
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
  logger.info("Checking if the env is set and every element contains at least field consentType.");
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
