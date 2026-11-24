const { expect } = require("chai");
const fs = require("fs");
const axios = require("axios");

process.env = Object.assign(process.env, {
    PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
    ATTEMPT_TIMEOUT_SEC: 5,
    NUM_RETRY: 3
});

const PA_TAX_ID = "77777777777";
const NOTICE_CODE = "302011695374606354";
const NOTICE_CODE_URL = `${process.env.PN_DELIVERY_URL}/price/${PA_TAX_ID}/${NOTICE_CODE}`;

const { handler } = require("../../index.js");


var MockAdapter = require("axios-mock-adapter");
var mock = new MockAdapter(axios);


describe("eventHandler tests", function () {
    beforeEach(function () {
        mock.reset();
    });

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
        const res = await handler(event)
        expect(res).to.not.be.null;
        expect(res).to.not.be.undefined;
        expect(res.statusCode).to.equal(404);
    });

    it("should return 404 when fetch return not found", async () => {

        mock.onGet(NOTICE_CODE_URL).reply(404, {}, { "Content-Type": "application/json" });

        const event = {
            pathParameters: { paTaxId: PA_TAX_ID, noticeCode: NOTICE_CODE },
            headers: {},
            requestContext: {
                authorizer: {},
            },
            resource: "/delivery/price/{paTaxId}/{noticeCode}",
            path: "/delivery/price",
            httpMethod: "GET"
        };

        const response = await handler(event);
        expect(response.statusCode).to.equal(404);
    });

    it("statusCode 200", async () => {
        const deliveryPriceJSON = fs.readFileSync("./src/test/deliveryPriceResponse.json");
        let deliveryPrice = JSON.parse(deliveryPriceJSON);



        mock.onGet(NOTICE_CODE_URL).reply(200, deliveryPrice, { "Content-Type": "application/json" });


        const event = {
            pathParameters: { paTaxId: PA_TAX_ID, noticeCode: NOTICE_CODE },
            headers: {},
            requestContext: {
                authorizer: {},
            },
            resource: "/delivery/price/{paTaxId}/{noticeCode}",
            path: "/delivery/price",
            httpMethod: "GET"
        };

        const response = await handler(event);
        expect(response.statusCode).to.equal(200);
    });

    it("does not register retry interceptors on every invocation", async () => {
        process.env = Object.assign(process.env, {
            PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
            ATTEMPT_TIMEOUT_SEC: "7",
            NUM_RETRY: "3"
        });

        mock.onGet(NOTICE_CODE_URL).reply(200, {});

        const event = {
            pathParameters: { paTaxId: PA_TAX_ID, noticeCode: NOTICE_CODE },
            headers: {},
            requestContext: { authorizer: {} },
            resource: "/delivery/price/{paTaxId}/{noticeCode}",
            path: "/delivery/price",
            httpMethod: "GET"
        };


        await handler(event);

        const requestCount = axios.interceptors.request.handlers.length
        const responseCount = axios.interceptors.response.handlers.length

        await handler(event);

        expect(axios.interceptors.request.handlers.length).to.equal(requestCount);
        expect(axios.interceptors.response.handlers.length).to.equal(responseCount);
    });


    it("retry once called", async () => {
        process.env = Object.assign(process.env, {
            PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
            ATTEMPT_TIMEOUT_SEC: "7",
            NUM_RETRY: "3"
        });


        mock.onGet(NOTICE_CODE_URL).networkErrorOnce().onGet(NOTICE_CODE_URL).reply(200, {});

        const event = {
            pathParameters: { paTaxId: PA_TAX_ID, noticeCode: NOTICE_CODE },
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

        await handler(event);
        expect(mock.history.get.length).to.equal(2); // 1 initial request + 1 retry
        expect(requestCount).to.equal(2);
        expect(responseCount).to.equal(2);

    });

    it("retry callback called", async () => {
        process.env = Object.assign(process.env, {
            PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
            ATTEMPT_TIMEOUT_SEC: "7",
            NUM_RETRY: "3"
        });

        const paTaxId = "77777777777";
        const noticeCode = "302011695374606354";
        const url = `${process.env.PN_DELIVERY_URL}/price/${paTaxId}/${noticeCode}`;
        mock.onGet(NOTICE_CODE_URL).networkError();

        const event = {
            pathParameters: { paTaxId: PA_TAX_ID, noticeCode: NOTICE_CODE },
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

        await handler(event);
        expect(mock.history.get.length).to.equal(4); // 1 initial request + 3 retries
        expect(requestCount).to.equal(4);
        expect(responseCount).to.equal(0);
    });

})