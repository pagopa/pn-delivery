const { expect } = require("chai");
const fs = require("fs");
const fetchMock = require("fetch-mock");
const proxyquire = require("proxyquire").noPreserveCache();

describe("eventHandler tests", function () {
  it("statusCode 200", async () => {
    const notificationJSON = fs.readFileSync("./src/test/notification.json");
    let notification = JSON.parse(notificationJSON);

    beforeEach(() => {
      fetchMock.reset();
    });

    process.env = Object.assign(process.env, {
      PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
    });

    const iunValue = "12345";

    let url = `${process.env.PN_DELIVERY_URL}/notifications/sent/${iunValue}`;

    const eventHandler = proxyquire
      .noCallThru()
      .load("../app/eventHandler.js", {});

    fetchMock.mock(url, {
      status: 200,
      body: notification,
      headers: { "Content-Type": "application/json" },
    });

    const event = {
      pathParameters: { iun: iunValue },
      headers: {},
      requestContext: {
        authorizer: {},
      },
      resource: "/notifications/sent/{iun}",
      path: "/delivery/notifications/sent/MOCK_IUN",
      httpMethod: "GET",
    };
    const context = {};

    const response = await eventHandler.versioning(event, context);

    expect(response.statusCode).to.equal(200);
  });

  it("statusCode 400", async () => {
    const notificationJSON = fs.readFileSync("./src/test/notification.json");
    let notification = JSON.parse(notificationJSON);
    notification.notificationStatus = "AAAAA";

    beforeEach(() => {
      fetchMock.reset();
    });

    process.env = Object.assign(process.env, {
      PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
    });

    const iunValue = "12345";

    let url = `${process.env.PN_DELIVERY_URL}/notifications/sent/${iunValue}`;

    const eventHandler = proxyquire
      .noCallThru()
      .load("../app/eventHandler.js", {});

    fetchMock.mock(url, {
      status: 200,
      body: notification,
      headers: { "Content-Type": "application/json" },
    });

    const event = {
      pathParameters: { iun: iunValue },
      headers: {},
      requestContext: {
        authorizer: {},
      },
      resource: "/notifications/sent/{iun}",
      path: "/delivery/notifications/sent/MOCK_IUN",
      httpMethod: "GET",
    };
    const context = {};

    const response = await eventHandler.versioning(event, context);

    expect(response.statusCode).to.equal(400);
  });

  it("statusCode 200 headers setting", async () => {
    const notificationJSON = fs.readFileSync("./src/test/notification.json");
    let notification = JSON.parse(notificationJSON);

    beforeEach(() => {
      fetchMock.reset();
    });

    process.env = Object.assign(process.env, {
      PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
    });

    const iunValue = "12345";

    let url = `${process.env.PN_DELIVERY_URL}/notifications/sent/${iunValue}`;

    const eventHandler = proxyquire
      .noCallThru()
      .load("../app/eventHandler.js", {});

    fetchMock.mock(url, {
      status: 200,
      body: notification,
      headers: { "Content-Type": "application/json" },
    });

    const event = {
      pathParameters: { iun: iunValue },
      headers: {},
      requestContext: {
        authorizer: {
          cx_groups: "aaa",
          cx_id: "bbb",
          cx_role: "ccc",
          cx_type: "ddd",
          cx_jti: "eee",
          sourceChannelDetails: "fff",
          uid: "ggg",
        },
      },
      resource: "/notifications/sent/{iun}",
      path: "/delivery/notifications/sent/MOCK_IUN",
      httpMethod: "GET",
    };

    const headersToCompare = {
      "x-pagopa-pn-src-ch": "B2B",
      "x-pagopa-pn-cx-groups": "aaa",
      "x-pagopa-pn-cx-id": "bbb",
      "x-pagopa-pn-cx-role": "ccc",
      "x-pagopa-pn-cx-type": "ddd",
      "x-pagopa-pn-jti": "eee",
      "x-pagopa-pn-src-ch-detail": "fff",
      "x-pagopa-pn-uid": "ggg",
    };

    const context = {};

    const response = await eventHandler.versioning(event, context);

    console.log("\n Heders 1", fetchMock.lastCall()[1].headers);

    expect(response.statusCode).to.equal(200);
    expect(
      JSON.stringify(fetchMock.lastCall()[1].headers) ===
        JSON.stringify(headersToCompare)
    ).to.equal(true);
    //lodash
  });

  it("statusCode 500 - fetch problem", async () => {
    beforeEach(() => {
      fetchMock.reset();
    });

    process.env = Object.assign(process.env, {
      PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
    });

    const iunValue = "12345";

    let url = `${process.env.PN_DELIVERY_URL}/notifications/sent/${iunValue}`;

    const eventHandler = proxyquire
      .noCallThru()
      .load("../app/eventHandler.js", {});

    fetchMock.mock(url, {
      status: 500,
      body: JSON.stringify({ error: "ERROR" }),
      headers: { "Content-Type": "application/json" },
    });

    const event = {
      pathParameters: { iun: iunValue },
      headers: {},
      requestContext: {
        authorizer: {},
      },
      resource: "/notifications/sent/{iun}",
      path: "/delivery/notifications/sent/MOCK_IUN",
      httpMethod: "GET",
    };
    const context = {};

    const response = await eventHandler.versioning(event, context);

    expect(response.statusCode).to.equal(500);
  });

  it("statusCode 502 - invalid endpoint", async () => {
    beforeEach(() => {
      fetchMock.reset();
    });

    process.env = Object.assign(process.env, {
      PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
    });

    const iunValue = "12345";

    let url = `${process.env.PN_DELIVERY_URL}/notifications/sent/${iunValue}`;

    const eventHandler = proxyquire
      .noCallThru()
      .load("../app/eventHandler.js", {});

    fetchMock.mock(url, {
      status: 500,
      body: "ERROR",
      headers: { "Content-Type": "application/json" },
    });

    const event = {
      pathParameters: { iun: iunValue },
      headers: {},
      requestContext: {
        authorizer: {},
      },
      resource: "/notifications/sent/", // wrong
      path: "/delivery/notifications/sent/MOCK_IUN", // correct
      httpMethod: "GET", // correct
    };
    const context = {};

    const response = await eventHandler.versioning(event, context);
    expect(response.statusCode).to.equal(502);

    event.resource = "/notifications/sent/{iun}"; // correct
    event.path = "/deliverypush/notifications/sent/MOCK_IUN"; // wrong
    const response2 = await eventHandler.versioning(event, context);
    expect(response2.statusCode).to.equal(502);

    event.path = "/delivery/notifications/sent/MOCK_IUN"; // correct
    event.httpMethod = "POST"; // wrong
    const response3 = await eventHandler.versioning(event, context);
    expect(response3.statusCode).to.equal(502);
  });

  it("Enum Not supported", async () => {
    const notificationJSON = fs.readFileSync("./src/test/notification.json");
    let notification = JSON.parse(notificationJSON);

    beforeEach(() => {
      fetchMock.reset();
    });

    process.env = Object.assign(process.env, {
      PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
    });

    const iunValue = "12345";

    let url = `${process.env.PN_DELIVERY_URL}/notifications/sent/${iunValue}`;

    const eventHandler = proxyquire
      .noCallThru()
      .load("../app/eventHandler.js", {});

    notification.notificationStatus = "NOT_SUPPORTED";

    fetchMock.mock(url, {
      status: 200,
      body: notification,
      headers: { "Content-Type": "application/json" },
    });

    const event = {
      pathParameters: { iun: iunValue },
      headers: {},
      requestContext: {
        authorizer: {},
      },
    };
    const context = {};

    expect(async () => await eventHandler.versioning(event, context)).to.throw;
  });

  it("Enum Not supported digitalAddress", async () => {
    const notificationJSON = fs.readFileSync("./src/test/notification.json");
    let notification = JSON.parse(notificationJSON);

    beforeEach(() => {
      fetchMock.reset();
    });

    process.env = Object.assign(process.env, {
      PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
    });

    const iunValue = "12345";

    let url = `${process.env.PN_DELIVERY_URL}/notifications/sent/${iunValue}`;

    const eventHandler = proxyquire
      .noCallThru()
      .load("../app/eventHandler.js", {});

    notification.recipients[0].digitalDomicile = {
      type: "NOT_SUPPORTED",
      address: "test@OK-pecFirstFailSecondSuccess.it",
    };

    fetchMock.mock(url, {
      status: 200,
      body: notification,
      headers: { "Content-Type": "application/json" },
    });

    const event = {
      pathParameters: { iun: iunValue },
      headers: {},
      requestContext: {
        authorizer: {},
      },
    };
    const context = {};

    expect(async () => await eventHandler.versioning(event, context)).to.throw;
  });

  it("fetch throw error", async () => {
    beforeEach(() => {
      fetchMock.reset();
    });

    process.env = Object.assign(process.env, {
      PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
    });

    const iunValue = "12345";

    let url = `${process.env.PN_DELIVERY_URL}/notifications/sent/${iunValue}`;

    const eventHandler = proxyquire
      .noCallThru()
      .load("../app/eventHandler.js", {});

    fetchMock.mock(url, () => {
      throw new Error("errore fetch");
    });

    const event = {
      pathParameters: { iun: iunValue },
      headers: {},
      requestContext: {
        authorizer: {},
      },
      resource: "/notifications/sent/{iun}",
      path: "/delivery/notifications/sent/MOCK_IUN",
      httpMethod: "GET",
    };
    const context = {};

    const response = await eventHandler.versioning(event, context);
    console.log("response ", response)

    expect(response.statusCode).to.equal(500);
  });
});
