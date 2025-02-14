const axios = require("axios");

class RestClient {
  static async getRootSenderId(senderPaId) {
    const response = await axios.get(
      `${process.env.PN_EXTERNAL_REGISTRIES_BASE_URL}/ext-registry-private/pa/v1/${senderPaId}/root-id`
    );
    return response.data.rootId;
  }

  static async getMandates(recipientId) {
    const response = await axios.get(
      `${process.env.PN_MANDATE_BASE_URL}/mandate-private/api/v1/mandates-by-internaldelegator/${recipientId}`,
      {
        params: { delegateType: "PG" },
      }
    );
    return response.data;
  }
}

module.exports = RestClient;
