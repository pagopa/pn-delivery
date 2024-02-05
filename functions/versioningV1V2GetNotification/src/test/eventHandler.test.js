const { expect } = require("chai");
const fs = require("fs");
const axios = require('axios');
var MockAdapter = require("axios-mock-adapter");
var mock = new MockAdapter(axios);
const proxyquire = require("proxyquire").noPreserveCache();

// valori collegati alla definizione del notification.json
// NB se si aggiungono elementi prima del viewed, aggiornare questo indice
const NOTIFICATION_VIEWED_IDX = 20;
const SEND_ANALOG_PROGRESS_IDX = 14;

describe("eventHandler tests", function () {
  it("statusCode 200", async () => {
    const notificationJSON = fs.readFileSync("./src/test/notification.json");
    let notification = JSON.parse(notificationJSON);

    process.env = Object.assign(process.env, {
      PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
    });

    const iunValue = "12345";

    let url = `${process.env.PN_DELIVERY_URL}/notifications/sent/${iunValue}`;

    const eventHandler = proxyquire
      .noCallThru()
      .load("../app/eventHandler.js", {});

    mock.onGet(url).reply(200, notification, { "Content-Type": "application/json" });

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

  it("statusCode 200 v2.0", async () => {
    const notificationJSON = fs.readFileSync("./src/test/notification.json");
    let notification = JSON.parse(notificationJSON);

    process.env = Object.assign(process.env, {
      PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
    });

    const iunValue = "12345";

    let url = `${process.env.PN_DELIVERY_URL}/notifications/sent/${iunValue}`;

    const eventHandler = proxyquire
      .noCallThru()
      .load("../app/eventHandler.js", {});

    mock.onGet(url).reply(200, notification, { "Content-Type": "application/json" });

    const event = {
      pathParameters: { iun: iunValue },
      headers: {},
      requestContext: {
        authorizer: {},
      },
      resource: "v2.0/notifications/sent/{iun}",
      path: "/delivery/v2.0/notifications/sent/MOCK_IUN",
      httpMethod: "GET",
    };
    const context = {};

    const response = await eventHandler.versioning(event, context);

    expect(response.statusCode).to.equal(200);
    expect(response.body.indexOf('NOTIFICATION_CANCELLATION_REQUEST' )).to.be.greaterThanOrEqual(0)
    expect(response.body.indexOf('NOTIFICATION_RADD_RETRIEVED' )).to.be.equal(-1)

    // check che NON sia presente l'eventTimestamp nel notificationViewed
    let resJson = JSON.parse(response.body);
    const viewElement = resJson.timeline[NOTIFICATION_VIEWED_IDX];
    expect(viewElement.category).to.be.equal('NOTIFICATION_VIEWED');
    expect(viewElement.details.eventTimestamp).to.be.equal(undefined);
    // check che NON sia presente l'serviceLevel nel SEND_ANALOG_PROGRESS
    const analogProgElement = resJson.timeline[SEND_ANALOG_PROGRESS_IDX];
    expect(analogProgElement.category).to.be.equal('SEND_ANALOG_PROGRESS');
    expect(analogProgElement.details.serviceLevel).to.be.equal(undefined);
  });


  it("statusCode 200 v2.1", async () => {
    const notificationJSON = fs.readFileSync("./src/test/notification.json");
    let notification = JSON.parse(notificationJSON);

    process.env = Object.assign(process.env, {
      PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
    });

    const iunValue = "12345";

    let url = `${process.env.PN_DELIVERY_URL}/notifications/sent/${iunValue}`;

    const eventHandler = proxyquire
      .noCallThru()
      .load("../app/eventHandler.js", {});

    mock.onGet(url).reply(200, notification, { "Content-Type": "application/json" });

    const event = {
      pathParameters: { iun: iunValue },
      headers: {},
      requestContext: {
        authorizer: {},
      },
      resource: "v2.1/notifications/sent/{iun}",
      path: "/delivery/v2.1/notifications/sent/MOCK_IUN",
      httpMethod: "GET",
    };
    const context = {};

    const response = await eventHandler.versioning(event, context);

    expect(response.statusCode).to.equal(200);
    expect(response.body.indexOf('NOTIFICATION_CANCELLATION_REQUEST' )).to.be.greaterThanOrEqual(0)
    expect(response.body.indexOf('NOTIFICATION_RADD_RETRIEVED' )).to.be.equal(-1)

    // check che NON sia presente l'eventTimestamp nel notificationViewed
    let resJson = JSON.parse(response.body);
    const viewElement = resJson.timeline[NOTIFICATION_VIEWED_IDX];
    expect(viewElement.category).to.be.equal('NOTIFICATION_VIEWED');
    expect(viewElement.details.eventTimestamp).to.be.equal(undefined);
    // check che NON sia presente l'serviceLevel nel SEND_ANALOG_PROGRESS
    const analogProgElement = resJson.timeline[SEND_ANALOG_PROGRESS_IDX];
    expect(analogProgElement.category).to.be.equal('SEND_ANALOG_PROGRESS');
    expect(analogProgElement.details.serviceLevel).to.be.equal(undefined);
  });

  it("statusCode 200 v2.3", async () => {
      const notificationJSON = fs.readFileSync("./src/test/notification.json");
      let notification = JSON.parse(notificationJSON);

      process.env = Object.assign(process.env, {
        PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
      });

      const iunValue = "12345";

      let url = `${process.env.PN_DELIVERY_URL}/notifications/sent/${iunValue}`;

      const eventHandler = proxyquire
        .noCallThru()
        .load("../app/eventHandler.js", {});

      mock.onGet(url).reply(200, notification, { "Content-Type": "application/json" });

      const event = {
        pathParameters: { iun: iunValue },
        headers: {},
        requestContext: {
          authorizer: {},
        },
        resource: "v2.3/notifications/sent/{iun}",
        path: "/delivery/v2.3/notifications/sent/MOCK_IUN",
        httpMethod: "GET",
      };
      const context = {};

      const response = await eventHandler.versioning(event, context);

      expect(response.statusCode).to.equal(200);
      expect(response.body.indexOf('NOTIFICATION_CANCELLATION_REQUEST' )).to.be.greaterThanOrEqual(0)
      expect(response.body.indexOf('NOTIFICATION_RADD_RETRIEVED' )).to.be.greaterThanOrEqual(0)

      // check che SIA presente l'eventTimestamp nel notificationViewed
      let resJson = JSON.parse(response.body);
      const viewElement = resJson.timeline[NOTIFICATION_VIEWED_IDX+1];    //+1 perchè qui la RADD c'è
      expect(viewElement.category).to.be.equal('NOTIFICATION_VIEWED');
      expect(viewElement.details.eventTimestamp).to.be.equal("2023-09-14T10:59:34.366420178Z");
      // check che SIA presente l'serviceLevel nel SEND_ANALOG_PROGRESS
      const analogProgElement = resJson.timeline[SEND_ANALOG_PROGRESS_IDX];
      expect(analogProgElement.category).to.be.equal('SEND_ANALOG_PROGRESS');
      expect(analogProgElement.details.serviceLevel).to.be.equal("REGISTERED_LETTER_890");
    });

  it("statusCode 200 v1", async () => {
    const notificationJSON = fs.readFileSync("./src/test/notification.json");
    let notification = JSON.parse(notificationJSON);

    process.env = Object.assign(process.env, {
      PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
    });

    const iunValue = "12345";

    let url = `${process.env.PN_DELIVERY_URL}/notifications/sent/${iunValue}`;

    const eventHandler = proxyquire
      .noCallThru()
      .load("../app/eventHandler.js", {});

    mock.onGet(url).reply(200, notification, { "Content-Type": "application/json" });

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
    expect(response.body.indexOf('NOTIFICATION_CANCELLATION_REQUEST' )).to.be.equal(-1)
    expect(response.body.indexOf('NOTIFICATION_RADD_RETRIEVED' )).to.be.equal(-1)

    // check che NON sia presente l'eventTimestamp nel notificationViewed
    let resJson = JSON.parse(response.body);
    const viewElement = resJson.timeline[NOTIFICATION_VIEWED_IDX];
    expect(viewElement.category).to.be.equal('NOTIFICATION_VIEWED');
    expect(viewElement.details.eventTimestamp).to.be.equal(undefined);
    // check che NON sia presente l'serviceLevel nel SEND_ANALOG_PROGRESS
    const analogProgElement = resJson.timeline[SEND_ANALOG_PROGRESS_IDX];
    expect(analogProgElement.category).to.be.equal('SEND_ANALOG_PROGRESS');
    expect(analogProgElement.details.serviceLevel).to.be.equal(undefined);
  });

  it("statusCode 400", async () => {
    const notificationJSON = fs.readFileSync("./src/test/notification.json");
    let notification = JSON.parse(notificationJSON);
    notification.notificationStatus = "AAAAA";

    process.env = Object.assign(process.env, {
      PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
    });

    const iunValue = "12345";

    let url = `${process.env.PN_DELIVERY_URL}/notifications/sent/${iunValue}`;

    const eventHandler = proxyquire
      .noCallThru()
      .load("../app/eventHandler.js", {});

    mock.onGet(url).reply(200, notification, { "Content-Type": "application/json" });

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

    process.env = Object.assign(process.env, {
      PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
    });

    const iunValue = "12345";

    let url = `${process.env.PN_DELIVERY_URL}/notifications/sent/${iunValue}`;

    const eventHandler = proxyquire
      .noCallThru()
      .load("../app/eventHandler.js", {});

    mock.onGet(url).reply(200, notification, { "Content-Type": "application/json" });

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

    expect(response.statusCode).to.equal(200);
    //lodash
  });

  it("statusCode 500 - fetch problem", async () => {

    process.env = Object.assign(process.env, {
      PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
    });

    const iunValue = "12345";

    let url = `${process.env.PN_DELIVERY_URL}/notifications/sent/${iunValue}`;

    const eventHandler = proxyquire
      .noCallThru()
      .load("../app/eventHandler.js", {});

    mock.onGet(url).reply(500, { error: "ERROR" }, { "Content-Type": "application/json" });

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

    process.env = Object.assign(process.env, {
      PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
    });

    const iunValue = "12345";

    let url = `${process.env.PN_DELIVERY_URL}/notifications/sent/${iunValue}`;

    const eventHandler = proxyquire
      .noCallThru()
      .load("../app/eventHandler.js", {});

    mock.onGet(url).reply(500, { error: "ERROR" }, { "Content-Type": "application/json" });

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

    process.env = Object.assign(process.env, {
      PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
    });

    const iunValue = "12345";

    let url = `${process.env.PN_DELIVERY_URL}/notifications/sent/${iunValue}`;

    const eventHandler = proxyquire
      .noCallThru()
      .load("../app/eventHandler.js", {});

    notification.notificationStatus = "NOT_SUPPORTED";

    mock.onGet(url).reply(200, notification, { "Content-Type": "application/json" });

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

    mock.onGet(url).reply(200, notification, { "Content-Type": "application/json" });

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

    process.env = Object.assign(process.env, {
      PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
    });

    const iunValue = "12345";

    let url = `${process.env.PN_DELIVERY_URL}/notifications/sent/${iunValue}`;

    const eventHandler = proxyquire
      .noCallThru()
      .load("../app/eventHandler.js", {});

    mock.onGet(url).abortRequest();

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

  it("Unable to map more than 2 payments", async () => {
    const notificationJSON = fs.readFileSync("./src/test/notification.json");
    let notification = JSON.parse(notificationJSON);

    process.env = Object.assign(process.env, {
      PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
    });

    const iunValue = "12345";

    let url = `${process.env.PN_DELIVERY_URL}/notifications/sent/${iunValue}`;

    const eventHandler = proxyquire
      .noCallThru()
      .load("../app/eventHandler.js", {});

    const extraPayment = {
      pagoPa: {
        noticeCode: "302011695374606354",
        creditorTaxId: "77777777777",
        applyCost: true,
        attachment: {
            digests: {
                "sha256": "jezIVxlG1M1woCSUngM6KipUN3/p8cG5RMIPnuEanlE="
            },
            contentType: "application/pdf",
            ref: {
                key: "PN_NOTIFICATION_ATTACHMENTS-7e5e2a329ead4a8aa57240cde190710a.pdf",
                versionToken: "v1"
            }
        }
      }
    }

    notification.recipients[0].payments.push(extraPayment);

    mock.onGet(url).reply(200, notification, { "Content-Type": "application/json" });

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

    const res = await eventHandler.versioning(event, context);
    expect(res.statusCode).to.equal(400)
  });

  it("Unamble to map f24 type payment", async () => {
    const notificationJSON = fs.readFileSync("./src/test/notification.json");
    let notification = JSON.parse(notificationJSON);

    process.env = Object.assign(process.env, {
      PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
    });

    const iunValue = "12345";

    let url = `${process.env.PN_DELIVERY_URL}/notifications/sent/${iunValue}`;

    const eventHandler = proxyquire
      .noCallThru()
      .load("../app/eventHandler.js", {});

    const extraPayment = {
      f24: {
        applyCost: true,
        attachmentMetadata: {
            digests: {
                "sha256": "jezIVxlG1M1woCSUngM6KipUN3/p8cG5RMIPnuEanlE="
            },
            contentType: "application/json",
            ref: {
                key: "PN_F24_METADATA-7e5e2a329ead4a8aa57240cde190710a.pdf",
                versionToken: "v1"
            }
        }
      }
    }

    notification.recipients[0].payments[0] = extraPayment;

    mock.onGet(url).reply(200, notification, { "Content-Type": "application/json" });

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

    const res = await eventHandler.versioning(event, context);
    console.log("RESULT: ", res)
    expect(res.statusCode).to.equal(400)
  });

});
