const axios = require("axios");

class RestClient {
  static async getLastVersion(consentType,cxType) {
    const response = await axios.get(
      `${process.env.PN_EXTERNAL_REGISTRIES_BASE_URL}/ext-registry-private/privacynotice/${consentType}/${cxType}`
    );
    return response.data.lastVersion;
  }
  static async putConsents(consentType, lastVersion, uid, cxType) {
    const response = await axios.put(
      `${process.env.PN_USER_ATTRIBUTES_BASE_URL}/user-consents/v1/consents/${consentType}`,
      {
        headers: {
          "x-pagopa-pn-uid": uid,
          "x-pagopa-pn-cx-type": cxType
        }
      }
    );
    return response.data;
  }

}

module.exports = RestClient;
