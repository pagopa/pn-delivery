const axios = require("axios");

class RestClient {
  static async getRootSenderId(senderPaId) {
    const response = await axios.get(
      `${process.env.PN_EXTERNAL_REGISTRIES_BASE_URL}/ext-registry-private/pa/v1/${senderPaId}/root-id`
    );
    return response.data.rootId;
  }
}

module.exports = RestClient;
