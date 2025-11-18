const { expect } = require("chai");
const sinon = require("sinon");
const axios = require("axios");
const MockAdapter = require("axios-mock-adapter");
const RestClient = require("../app/services");

describe("RestClient", () => {
  let mock;

  beforeEach(() => {
    // Create axios mock adapter
    mock = new MockAdapter(axios);

    process.env.API_BASE_URL = "https://api.test.com";
  });

  afterEach(() => {
    mock.restore();
    sinon.restore();
    delete process.env.API_BASE_URL;
  });

  describe("getLastVersion", () => {
    it("should successfully retrieve last version", async () => {
      const mockResponse = {
        version: "v1.2.3",
      };

      mock
        .onGet("https://api.test.com/ext-registry-private/privacynotice/TOS/PF")
        .reply(200, mockResponse);

      const result = await RestClient.getLastVersion("PF", "TOS");

      expect(result).to.equal("v1.2.3");
    });

    it("should handle different consent types and context types", async () => {
      const mockResponse = {
        version: "v2.0.0",
      };

      mock
        .onGet(
          "https://api.test.com/ext-registry-private/privacynotice/PRIVACY/PG"
        )
        .reply(200, mockResponse);

      const result = await RestClient.getLastVersion("PG", "PRIVACY");

      expect(result).to.equal("v2.0.0");
    });

    it("should handle API errors and throw them", async () => {
      mock
        .onGet("https://api.test.com/ext-registry-private/privacynotice/TOS/PF")
        .networkError();

      try {
        await RestClient.getLastVersion("PF", "TOS");
        expect.fail("Should have thrown an error");
      } catch (error) {
        expect(error).to.be.an.instanceOf(Error); // Cambiato da .be.an('error')
        expect(error.message).to.include("Network Error"); // Verifica il messaggio
      }
    });

    it("should handle HTTP 500 error responses", async () => {
      mock
        .onGet("https://api.test.com/ext-registry-private/privacynotice/TOS/PF")
        .reply(500, { error: "Internal server error" });

      try {
        await RestClient.getLastVersion("PF", "TOS");
        expect.fail("Should have thrown an error");
      } catch (error) {
        expect(error).to.be.an.instanceOf(Error);
        expect(error.response.status).to.equal(500);
      }
    });
  });

  describe("putConsents", () => {
    it("should successfully accept consent", async () => {
      const mockResponse = {
        success: true,
        consentId: "consent-123",
      };

      mock
        .onPut(
          "https://api.test.com/user-consents/v1/consents/TOS?version=v1.0"
        )
        .reply(200, mockResponse);

      const result = await RestClient.putConsents(
        "TOS",
        "v1.0",
        "user123",
        "PF"
      );

      expect(result).to.deep.equal(mockResponse);

      // Verify request body and headers
      expect(mock.history.put[0].data).to.equal(
        JSON.stringify({ action: "ACCEPT" })
      );
      expect(mock.history.put[0].headers["x-pagopa-pn-uid"]).to.equal(
        "user123"
      );
      expect(mock.history.put[0].headers["x-pagopa-pn-cx-type"]).to.equal("PF");
    });

    it("should handle different consent types", async () => {
      mock
        .onPut(
          "https://api.test.com/user-consents/v1/consents/MARKETING?version=v3.1"
        )
        .reply(200, { success: true });

      await RestClient.putConsents("MARKETING", "v3.1", "company789", "PG");
    });

    it("should handle API errors and throw them", async () => {
      mock
        .onPut(
          "https://api.test.com/user-consents/v1/consents/TOS?version=v1.0"
        )
        .networkError();

      try {
        await RestClient.putConsents("TOS", "v1.0", "user123", "PF");
        expect.fail("Should have thrown an error");
      } catch (error) {
        expect(error).to.be.an.instanceOf(Error);
        expect(error.message).to.include("Network Error");
      }
    });

    it("should verify request payload structure", async () => {
      mock.onPut().reply(200, { success: true });

      await RestClient.putConsents("TOS", "v1.0", "user123", "PF");

      const request = mock.history.put[0];
      expect(JSON.parse(request.data)).to.deep.equal({ action: "ACCEPT" });
      expect(request.headers["Content-Type"]).to.include("application/json");
    });
  });

  describe("checkQrCode", () => {
    let mockUserInfo;
    let mockHeadersToForward;
    let mockBody;

    beforeEach(() => {
      mockUserInfo = {
        cxType: "PF",
        cxId: "user123",
        taxId: "RSSMRA80A01H501U",
      };
      mockHeadersToForward = {
        "x-custom-header": "custom-value",
        "x-trace-id": "trace-123",
      };
      mockBody = "QR_CODE_CONTENT";
    });

    it("should successfully check QR code", async () => {
      const mockResponse = {
        valid: true,
        notificationId: "notification-123",
      };

      mock
        .onPost(
          "https://api.test.com/delivery/notifications/received/check-qr-code"
        )
        .reply(200, mockResponse);

      const result = await RestClient.checkQrCode(
        mockBody,
        mockHeadersToForward,
        mockUserInfo
      );

      expect(result).to.deep.equal({
        statusCode: 200,
        body: JSON.stringify(mockResponse),
      });

      // Verify request headers
      const request = mock.history.post[0];
      expect(request.headers["x-pagopa-pn-cx-type"]).to.equal("PF");
      expect(request.headers["x-pagopa-pn-cx-id"]).to.equal("user123");
      expect(request.headers["x-pagopa-cx-taxid"]).to.equal("RSSMRA80A01H501U");
      expect(request.headers["Content-Type"]).to.equal("application/json");
      expect(request.headers["x-pagopa-pn-src-ch"]).to.equal("IO");
      expect(request.headers["x-custom-header"]).to.equal("custom-value");
      expect(request.headers["x-trace-id"]).to.equal("trace-123");
    });

    it("should handle HTTP error responses from server", async () => {
      const errorResponse = {
        error: "Invalid QR code",
        code: "INVALID_QR",
      };

      mock
        .onPost(
          "https://api.test.com/delivery/notifications/received/check-qr-code"
        )
        .reply(400, errorResponse);

      const result = await RestClient.checkQrCode(
        mockBody,
        mockHeadersToForward,
        mockUserInfo
      );

      expect(result).to.deep.equal({
        statusCode: 400,
        body: JSON.stringify(errorResponse),
      });
    });

    it("should handle 404 error responses", async () => {
      const notFoundError = {
        error: "QR code not found",
      };

      mock
        .onPost(
          "https://api.test.com/delivery/notifications/received/check-qr-code"
        )
        .reply(404, notFoundError);

      const result = await RestClient.checkQrCode(
        mockBody,
        mockHeadersToForward,
        mockUserInfo
      );

      expect(result).to.deep.equal({
        statusCode: 404,
        body: JSON.stringify(notFoundError),
      });
    });

    it("should handle 500 error responses", async () => {
      const serverError = {
        error: "Internal server error",
      };

      mock
        .onPost(
          "https://api.test.com/delivery/notifications/received/check-qr-code"
        )
        .reply(500, serverError);

      const result = await RestClient.checkQrCode(
        mockBody,
        mockHeadersToForward,
        mockUserInfo
      );

      expect(result).to.deep.equal({
        statusCode: 500,
        body: JSON.stringify(serverError),
      });
    });

    it("should handle request errors and throw them", async () => {
      // Simula un errore di richiesta (network error senza risposta dal server)
      mock
        .onPost(
          "https://api.test.com/delivery/notifications/received/check-qr-code"
        )
        .timeout();

      let consoleLogStub = sinon.stub(console, "log");

      try {
        await RestClient.checkQrCode(
          mockBody,
          mockHeadersToForward,
          mockUserInfo
        );
        expect.fail("Should have thrown an error");
      } catch (error) {
        expect(error).to.be.an.instanceOf(Error);
        expect(error.message).to.include("timeout");
        expect(
          consoleLogStub.calledWith("Error for the request:", sinon.match.any)
        ).to.be.true;
      }

      consoleLogStub.restore();
    });

    it("should preserve all forwarded headers", async () => {
      const manyHeaders = {
        "x-trace-id": "trace-123",
        "x-request-id": "req-456",
        "x-correlation-id": "corr-789",
        "accept-language": "it-IT",
        "user-agent": "TestAgent/1.0",
      };

      mock
        .onPost(
          "https://api.test.com/delivery/notifications/received/check-qr-code"
        )
        .reply(200, { valid: true });

      await RestClient.checkQrCode(mockBody, manyHeaders, mockUserInfo);

      const request = mock.history.post[0];
      expect(request.headers["x-trace-id"]).to.equal("trace-123");
      expect(request.headers["x-request-id"]).to.equal("req-456");
      expect(request.headers["x-correlation-id"]).to.equal("corr-789");
      expect(request.headers["accept-language"]).to.equal("it-IT");
      expect(request.headers["user-agent"]).to.equal("TestAgent/1.0");
    });
  });
});
