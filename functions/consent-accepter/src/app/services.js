const axios = require("axios");
const logger = require("./logger");

const getTraceIdFromEv = () => {
  return process.env.X_AMZN_TRACE_ID ? process.env.X_AMZN_TRACE_ID : logger.debug("X_AMZN_TRACE_ID non presente");
};

class RestClient {
  static async getLastVersion(cxType, consentType) {
    logger.info(`Retrieving last version for consent ${consentType}`);
    try {
      const response = await axios.get(
        `${process.env.API_BASE_URL}/ext-registry-private/privacynotice/${consentType}/${cxType}`,
        { headers:{"X-Amzn-Trace-Id": getTraceIdFromEv()} }
      );
      return response.data.version;
    } catch(error) {
      logger.error(`Error invoking api getLastVersion for consentType: ${consentType}`, error);
      throw error
    }
  }
  static async putConsents(consentType, lastVersion, uid, cxType) {
    logger.info(`Accepting consent ${consentType} (version: ${lastVersion}) for user ${uid} and cxType ${cxType}`);
    try {
      const response = await axios.put(
        `${process.env.API_BASE_URL}/user-consents/v1/consents/${consentType}?version=${encodeURIComponent(lastVersion)}`,
        { action: "ACCEPT" },
        {
          headers: {
            "x-pagopa-pn-uid": uid,
            "x-pagopa-pn-cx-type": cxType,
            "X-Amzn-Trace-Id": getTraceIdFromEv()
          }
        }
      );
       return response.data;
    } catch(error) {
      logger.error(`Error invoking putConsents with consentType : ${consentType} and version : ${lastVersion}`, error);
      throw error;
    }
  }

  static async checkQrCode(body, headersToForward, userInfo) {
    try {
      const response = await axios.post(
        `${process.env.API_BASE_URL}/delivery/notifications/received/check-qr-code`,
        body,
        {
          headers: {
            "x-pagopa-pn-cx-type": userInfo.cxType,
            "x-pagopa-pn-cx-id": userInfo.cxId,
            "x-pagopa-cx-taxid": userInfo.taxId,
            "Content-Type": "application/json",
            "x-pagopa-pn-src-ch": "IO",
            "X-Amzn-Trace-Id": getTraceIdFromEv(),
            ...headersToForward
          }
        }
      );

      return{
       statusCode: response.status,
       body: JSON.stringify(response.data)
      };
    } catch (error) {
      if(error.response){
          logger.info("Error response from server");
          return {
            statusCode: error.response.status,
            body: JSON.stringify(error.response.data)
          };
      }
      else {
          logger.info("Error for the request:", error.request);
          throw error;
        }
      }
  }
}

module.exports = RestClient;
