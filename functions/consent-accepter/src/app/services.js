const axios = require("axios");

class RestClient {
  static async getLastVersion(consentType,cxType) {
    const response = await axios.get(
      `${process.env.PN_EXTERNAL_REGISTRIES_BASE_URL}/ext-registry-private/privacynotice/${consentType}/${cxType}`
    );
    return response.data.version;
  }
  static async putConsents(consentType, lastVersion, uid, cxType) {
    const response = axios.put(
      `${process.env.PN_USER_ATTRIBUTES_BASE_URL}/user-consents/v1/consents/${consentType}?lastVersion=${encodeURIComponent(lastVersion)}`,
      { action: "ACCEPT" },
      {
        headers: {
          "x-pagopa-pn-uid": uid,
          "x-pagopa-pn-cx-type": cxType
        }
      }
    );
    return response.data;
  }
  static async createMandateFromAppIo(event) {
    const response = await axios.post(
      `${process.env.PN_DELIVERY_BASE_URL}/delivery/notifications/received/check-qr-code`,
      event.body
    );
    return response.data;
  }
}

module.exports = RestClient;
