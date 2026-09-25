const RestClient = require("./services");
const { getUserInfoFromEvent, retrieveHeadersToForward, retrieveAuthorizerHeaders } = require("./utils");
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
    const resourcePath = (event.requestContext || {}).resourcePath || "";
    const isNotificationFlow = resourcePath === "/delivery/notifications/received/{iun}";
    const channel = isNotificationFlow ? "IO" : null;
    const promiseList = consentsToAccept.map(consent => acceptConsent(consent, userInfo, channel));
    await Promise.all(promiseList);
    logger.info("All consents accepted successfully.");
    const headersToForward = {
       ...retrieveHeadersToForward(event.headers || {}),
       ...retrieveAuthorizerHeaders((event.requestContext || {}).authorizer || {}  || {})
    };
    if (isNotificationFlow) {
      const iun = (event.pathParameters || {}).iun;
      return await RestClient.getNotificationByIun(iun, headersToForward, userInfo);
    }
    return await RestClient.checkQrCode(event.body, headersToForward, userInfo);
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

async function acceptConsent(consent, userInfo, channel) {
  let lastVersion = consent.version;
  if (!lastVersion) {
    lastVersion = await cacheManager.get(userInfo.cxType, consent.consentType);
  }
  await RestClient.putConsents(
    consent.consentType,
    lastVersion,
    userInfo.uid,
    userInfo.cxType,
    userInfo.cxId,
    channel
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
