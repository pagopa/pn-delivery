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

    it("should return 404 when fecth return not found", async () => {
        process.env = Object.assign(process.env, {
            PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
        });

        const paTaxId = "77777777777";
        const noticeCode = "302011695374606354";

        let url = `${process.env.PN_DELIVERY_URL}/delivery/price/${paTaxId}/${noticeCode}`;

        mock.onGet(url).reply(404, {}, {"Content-Type": "application/json"});

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

        let url = `${process.env.PN_DELIVERY_URL}/delivery/price/${paTaxId}/${noticeCode}`;

        mock.onGet(url).reply(200, deliveryPrice, {"Content-Type": "application/json"});

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

    
})