const axios = require("axios");

class RestClient {
  static async getLastVersion(consentType, cxType) {
    console.log(`Retrieving last version for consent ${consentType}`);
    try {
      const response = await axios.get(
        `${process.env.PN_EXTERNAL_REGISTRIES_BASE_URL}/ext-registry-private/privacynotice/${consentType}/${cxType}`
      );
      return response.data.version;
    } catch(error) {
      console.error(`Error invoking api getLastVersion for consentType: ${consentType}`, error);
      throw error
    }
  }
  static async putConsents(consentType, lastVersion, uid, cxType) {
    console.log(`Accepting consent ${consentType} (version: ${lastVersion}) for user ${uid} and cxType ${cxType}`);
    try {
      const response = await axios.put(
        `${process.env.PN_USER_ATTRIBUTES_BASE_URL}/user-consents/v1/consents/${consentType}?version=${encodeURIComponent(lastVersion)}`,
        { action: "ACCEPT" },
        {
          headers: {
            "x-pagopa-pn-uid": uid,
            "x-pagopa-pn-cx-type": cxType
          }
        }
      );
       return response.data;
    } catch(error) {
      console.error(`Error invoking putConsents with consentType : ${consentType} and version : ${lastVersion}`, error);
      throw error;
    }
  }

  static async checkQrCode(body, lollipopHeaders, userInfo) {
    try {
      const response = await axios.post(
        `${process.env.PN_DELIVERY_BASE_URL}/delivery/notifications/received/check-qr-code`,
        body,
        {
          headers: {
            "x-pagopa-pn-cx-type": userInfo.cxType,
            "x-pagopa-pn-cx-id": userInfo.cxId,
            "x-pagopa-cx-taxid": userInfo.taxId,
            "Content-Type": "application/json",
            ...lollipopHeaders
          }
        }
      );

      return{
       statusCode: response.status,
       body: response.data
      };
    } catch (error) {
      if(error.response){
          console.log("Error response from server");
          return {
            statusCode: error.response.status,
            body: JSON.stringify(error.response.data)
          };
      }
      else if(error.request){
          console.log("Error for the request:", error.request);
          throw error;
        }
      }
  }
}

module.exports = RestClient;
