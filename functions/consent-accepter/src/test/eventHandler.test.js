const { expect } = require("chai");
const axios = require("axios");
const MockAdapter = require("axios-mock-adapter");
const { handle } = require("../app/eventHandler");
const consentsToAccept = require("./consents_to_accept.json");
const consentsToAcceptWithoutVersion = require("./consents_to_accept_without_version.json");
const consentsToAcceptWithoutConsentType = require("./consents_to_accept_without_consentType.json");
const event = require("./event.json");
const eventWithoutHeaders = require("./event_without_headers.json");

describe("eventHandler test", () => {
  let mock, oldEnv;
  beforeEach(() => {
    mock = new MockAdapter(axios);
    oldEnv = { ...process.env };
  });
  afterEach(() => {
    mock.restore();
    process.env = oldEnv;
  });

  it("200 ok", async () => {
    process.env.CONSENTS_TO_ACCEPT = JSON.stringify(consentsToAccept);
    mock.onPut(/.*/).reply(200);
    mock.onPost(/\/check-qr-code/).reply(200, "ok");

    const res = await handle(event);

    expect(res.statusCode).to.equal(200);
    expect(res.body).to.equal("ok");
  });

  it("200 ok with get version from getLastVersion", async () => {
    process.env.CONSENTS_TO_ACCEPT = JSON.stringify(consentsToAcceptWithoutVersion);
    mock.onGet(/\/privacynotice/).reply(200, { version: "2.0.0" });
    mock.onPut(/.*/).reply(200);
    mock.onPost(/\/check-qr-code/).reply(201, "done");

    const res = await handle(event);

    expect(res.statusCode).to.equal(201);
    expect(res.body).to.equal("done");
  });

  it("it should return error if CONSENTS_TO_ACCEPT environment variable is missing", async () => {
    delete process.env.CONSENTS_TO_ACCEPT;

    const res = await handle(event);

    expect(res.statusCode).to.equal(500);
    const error = JSON.parse(res.body).errors[0].code;
    expect(error).to.equal("Error executing request");
  });

  it("it should return error if CONSENTS_TO_ACCEPT is not a JSON array", async () => {
    process.env.CONSENTS_TO_ACCEPT = "{}";
    const res = await handle(event);
    expect(res.statusCode).to.equal(500);
    const error = JSON.parse(res.body).errors[0].code;
    expect(error).to.equal("Error executing request");
  });

  it("it should return 500 error if at least one consent is missing the consentType field", async () => {
    process.env.CONSENTS_TO_ACCEPT = JSON.stringify(consentsToAcceptWithoutConsentType);
    const res = await handle(event);
    expect(res.statusCode).to.equal(500);
    const error = JSON.parse(res.body).errors[0].code;
    expect(error).to.equal("Error executing request");
  });

  it("it should return 500 error if required headers are missing in the event", async () => {
    process.env.CONSENTS_TO_ACCEPT = JSON.stringify(consentsToAccept);
    const res = await handle(eventWithoutHeaders);
    expect(res.statusCode).to.equal(500);
    const error = JSON.parse(res.body).errors[0].code;
    expect(error).to.equal("Error executing request");
  });

  it("it should return 500 error if putConsents throws an error", async () => {
    process.env.CONSENTS_TO_ACCEPT = JSON.stringify(consentsToAccept);
    mock.onPut(/.*/).reply(500);
    const res = await handle(event);
    expect(res.statusCode).to.equal(500);
    const error = JSON.parse(res.body).errors[0].code;
    expect(error).to.equal("Error executing request");
  });

  it("it should return 403 error message because checkQrCode fail and lambda returns generic error", async () => {
    process.env.CONSENTS_TO_ACCEPT = JSON.stringify(consentsToAccept);
    mock.onPut(/.*/).reply(200);
    mock.onPost(/\/check-qr-code/).reply(500, { message: "Internal Server Error" });
    const res = await handle(event);
    expect(res.statusCode).to.equal(500);
    const errorBody = JSON.parse(res.body);
    expect(errorBody.message).to.equal("Internal Server Error");
  });

  it("it should return 404 error message because checkQrCode return error", async () => {
    process.env.CONSENTS_TO_ACCEPT = JSON.stringify(consentsToAccept);
    mock.onPut(/.*/).reply(200);
    mock.onPost(/\/check-qr-code/).reply(404, { message: "Not Found" });
    const res = await handle(event);
    expect(res.statusCode).to.equal(404);
    const errorBody = JSON.parse(res.body);
    expect(errorBody.message).to.equal("Not Found");
  });

  it("it should return 403 error message because checkQrCode return error ", async () => {
    process.env.CONSENTS_TO_ACCEPT = JSON.stringify(consentsToAccept);
    mock.onPut(/.*/).reply(200);
    mock.onPost(/\/check-qr-code/).reply(403, { message: "Forbidden" });
    const res = await handle(event);
    expect(res.statusCode).to.equal(403);
    const errorBody = JSON.parse(res.body);
    expect(errorBody.message).to.equal("Forbidden");
  });
});
