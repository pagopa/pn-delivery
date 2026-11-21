const { handleEvent } = require("../app/eventHandler.js");
const { expect } = require("chai");
const fs = require("fs");
const axios = require('axios');
var MockAdapter = require("axios-mock-adapter");
var mock = new MockAdapter(axios);


describe("eventHandler tests", function () {
    it("should return 404 when Invalid path/method", async () => {

        const event = {
            path: '/delivery/price',
            httpMethod: "POST",
            headers: {},
            requestContext: {
                authorizer: {},
            },
            queryStringParameters: {}
        }

        const res = await handleEvent(event)
        expect(res).to.not.be.null;
        expect(res).to.not.be.undefined;
        expect(res.statusCode).to.equal(404);
    });

    it("should return 404 when fetch return not found", async () => {
        process.env = Object.assign(process.env, {
            PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
            ATTEMPT_TIMEOUT_SEC: 5,
            NUM_RETRY: 3
        });

        const paTaxId = "77777777777";
        const noticeCode = "302011695374606354";

        let url = `${process.env.PN_DELIVERY_URL}/price/${paTaxId}/${noticeCode}`;

        mock.onGet(url).reply(404, {}, { "Content-Type": "application/json" });

        const event = {
            pathParameters: { paTaxId: paTaxId, noticeCode: noticeCode },
            headers: {},
            requestContext: {
                authorizer: {},
            },
            resource: "/delivery/price/{paTaxId}/{noticeCode}",
            path: "/delivery/price",
            httpMethod: "GET"
        };

        const response = await handleEvent(event);
        expect(response.statusCode).to.equal(404);
    });

    it("statusCode 200", async () => {
        const deliveryPriceJSON = fs.readFileSync("./src/test/deliveryPriceResponse.json");
        let deliveryPrice = JSON.parse(deliveryPriceJSON);

        process.env = Object.assign(process.env, {
            PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
        });

        const paTaxId = "77777777777";
        const noticeCode = "302011695374606354";

        let url = `${process.env.PN_DELIVERY_URL}/price/${paTaxId}/${noticeCode}`;

        mock.onGet(url).reply(200, deliveryPrice, { "Content-Type": "application/json" });


        const event = {
            pathParameters: { paTaxId: paTaxId, noticeCode: noticeCode },
            headers: {},
            requestContext: {
                authorizer: {},
            },
            resource: "/delivery/price/{paTaxId}/{noticeCode}",
            path: "/delivery/price",
            httpMethod: "GET"
        };

        const response = await handleEvent(event);
        expect(response.statusCode).to.equal(200);
    });


    it("have only one timeout", async () => {
        process.env = Object.assign(process.env, {
            PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
            ATTEMPT_TIMEOUT_SEC: "7",
            NUM_RETRY: "3"
        });

        const paTaxId = "77777777777";
        const noticeCode = "302011695374606354";
        const url = `${process.env.PN_DELIVERY_URL}/price/${paTaxId}/${noticeCode}`;

        // index.js registers axios-retry when it is first imported.
        const { handler } = require("../../index.js");
mock.reset();
        mock.onGet(url).reply(200, {});


        const event = {
            pathParameters: { paTaxId, noticeCode },
            headers: {},
            requestContext: { authorizer: {} },
            resource: "/delivery/price/{paTaxId}/{noticeCode}",
            path: "/delivery/price",
            httpMethod: "GET"
        };
        const originalWarn = console.warn;
const warnings = [];
console.warn = (...args) => warnings.push(args);

        await handleEvent(event);

mock.onGet(url).timeout();


        await handleEvent(event);

console.warn = originalWarn;
let warningsCount = 0;
warnings.forEach(warning => { if (warning.toString().includes("Retries exceeded: 3 - error:timeout of 7000ms exceeded") )warningsCount++ });
       console.log("********** " + warningsCount + " warnings with timeout of 7000ms exceeded");
       expect(warningsCount).to.equal(1); // 1 initial request + 1 retry
    });

    it("does not register retry interceptors on every invocation", async () => {
        process.env = Object.assign(process.env, {
            PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
            ATTEMPT_TIMEOUT_SEC: "7",
            NUM_RETRY: "3"
        });

        const paTaxId = "77777777777";
        const noticeCode = "302011695374606354";
        const url = `${process.env.PN_DELIVERY_URL}/price/${paTaxId}/${noticeCode}`;

        // index.js registers axios-retry when it is first imported.
        const { handler } = require("../../index.js");
        mock.onGet(url).reply(200, {});

        const event = {
            pathParameters: { paTaxId, noticeCode },
            headers: {},
            requestContext: { authorizer: {} },
            resource: "/delivery/price/{paTaxId}/{noticeCode}",
            path: "/delivery/price",
            httpMethod: "GET"
        };


        await handleEvent(event);
        const requestCount = axios.interceptors.request.length;
        const responseCount = axios.interceptors.request.length;

        await handleEvent(event);

        console.debug("Request interceptors count: ", axios.interceptors.request);
        expect(axios.interceptors.request.length).to.equal(requestCount);
        console.debug("Response interceptors count: ", axios.interceptors.response);
        expect(axios.interceptors.response.length).to.equal(responseCount);
    });


    it("retry once called", async () => {
        process.env = Object.assign(process.env, {
            PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
            ATTEMPT_TIMEOUT_SEC: "7",
            NUM_RETRY: "3"
        });

        // index.js registers axios-retry when it is first imported.
        const { handler } = require("../../index.js");

        const paTaxId = "77777777777";
        const noticeCode = "302011695374606354";
        const url = `${process.env.PN_DELIVERY_URL}/price/${paTaxId}/${noticeCode}`;
        mock.reset();
        mock.onGet(url).networkErrorOnce().onGet(url).reply(200, {});

        const event = {
            pathParameters: { paTaxId, noticeCode },
            headers: {},
            requestContext: { authorizer: {} },
            resource: "/delivery/price/{paTaxId}/{noticeCode}",
            path: "/delivery/price",
            httpMethod: "GET"
        };

        mock.resetHistory();
        let requestCount = 0;
        axios.interceptors.request.use(
            (config) => {
                requestCount += 1;
                return config;
            }

        );
        let responseCount = 0;
        axios.interceptors.response.use((
            (config) => {
                responseCount += 1;
                return config;
            }

        ));

        await handleEvent(event);
        expect(mock.history.get.length).to.equal(2); // 1 initial request + 1 retry
        console.debug(" ->Request interceptors count: ", requestCount);
        expect(requestCount).to.equal(2);
        console.debug(" ->Response interceptors count: ", responseCount);
        expect(responseCount).to.equal(2);

    });

    it("retry callback called", async () => {
        process.env = Object.assign(process.env, {
            PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
            ATTEMPT_TIMEOUT_SEC: "7",
            NUM_RETRY: "3"
        });

        // index.js registers axios-retry when it is first imported.
        const { handler } = require("../../index.js");

        const paTaxId = "77777777777";
        const noticeCode = "302011695374606354";
        const url = `${process.env.PN_DELIVERY_URL}/price/${paTaxId}/${noticeCode}`;
        mock.reset();
        mock.onGet(url).networkError();

        const event = {
            pathParameters: { paTaxId, noticeCode },
            headers: {},
            requestContext: { authorizer: {} },
            resource: "/delivery/price/{paTaxId}/{noticeCode}",
            path: "/delivery/price",
            httpMethod: "GET"
        };

        mock.resetHistory();
        let requestCount = 0;
        axios.interceptors.request.use(
            (config) => {
                requestCount += 1;
                return config;
            }

        );
        let responseCount = 0;
        axios.interceptors.response.use((
            (config) => {
                responseCount += 1;
                return config;
            }

        ));

        await handleEvent(event);
        expect(mock.history.get.length).to.equal(4); // 1 initial request + 3 retries
        console.debug("Request interceptors count: ", requestCount);
        expect(requestCount).to.equal(4);
        console.debug("Response interceptors count: ", responseCount);
        expect(responseCount).to.equal(0);
    });
})