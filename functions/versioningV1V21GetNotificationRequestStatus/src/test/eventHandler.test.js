const { handleEvent } = require("../app/eventHandler.js");
const { expect } = require("chai");
const fs = require("fs");
const axios = require('axios');
var MockAdapter = require("axios-mock-adapter");
var mock = new MockAdapter(axios);
const proxyquire = require("proxyquire").noPreserveCache();


describe("eventHandler tests", function () {
    it("should return 404 when Invalid path/method/query parameters", async () => {
        
        const event = {
            path: '/delivery/requests',
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

    it("should return 400 when Invalid query parameters", async () => {
        
        const event = {
            path: '/delivery/requests',
            httpMethod: "GET",
            headers: {},
            requestContext: {
                authorizer: {},
            },
            queryStringParameters: {
                paProtocolNumber: 'fakePaProtocolNumber'
            }
        }
        
        const res = await handleEvent(event)
        expect(res).to.not.be.null;
        expect(res).to.not.be.undefined;
        expect(res.statusCode).to.equal(400);
    });

    it("should return 404 when fecth return not found", async () => {

        process.env = Object.assign(process.env, {
            PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it/delivery",
            ATTEMPT_TIMEOUT_SEC: 5,
            NUM_RETRY: 3
        });
        
        let url = `${process.env.PN_DELIVERY_URL}/requests?`;
        const params = new URLSearchParams({
            notificationRequestId: 'invalidNotificationRequestId'
        });

        mock.onGet(url, { params: params }).reply(404, {}, { "Content-Type": "application/json" });

        const event = {
            path: '/delivery/requests',
            httpMethod: "GET",
            headers: {},
            requestContext: {
                authorizer: {},
            },
            queryStringParameters: {
                notificationRequestId: 'invalidNotificationRequestId'
            }
        }
        
        const res = await handleEvent(event)
       
        expect(res.statusCode).to.equal(404);
    });

    it("should return 400 when fecth return bad request", async () => {

        process.env = Object.assign(process.env, {
            PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
        });
        
        let url = `${process.env.PN_DELIVERY_URL}/requests?`;
        const params = new URLSearchParams({
            notificationRequestId: 'invalidNotificationRequestId'
        });

        mock.onGet(url, { params: params }).reply(400, {}, { "Content-Type": "application/json" });

        const event = {
            path: '/delivery/requests',
            httpMethod: "GET",
            headers: {},
            requestContext: {
                authorizer: {},
            },
            queryStringParameters: {
                notificationRequestId: 'invalidNotificationRequestId'
            }
        }
        
        const res = await handleEvent(event)
       
        expect(res.statusCode).to.equal(400);
    });

    it("should return 200", async () => {
        const notificationRequestStatusJSON = fs.readFileSync("./src/test/notificationRequestStatusV23.json");
        let notificationRequestStatusV21 = JSON.parse(notificationRequestStatusJSON);

        process.env = Object.assign(process.env, {
            PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it/delivery",
        });
        
        let url = `${process.env.PN_DELIVERY_URL}/requests?`;
        const params = new URLSearchParams({
            notificationRequestId: 'validNotificationRequestId'
        });

        mock.onGet(url, { params: params }).reply(200, notificationRequestStatusV21, { "Content-Type": "application/json" });

        const event = {
            path: '/delivery/requests',
            httpMethod: "GET",
            headers: {},
            requestContext: {
                authorizer: {},
            },
            queryStringParameters: {
                notificationRequestId: 'validNotificationRequestId'
            }
        }
        
        const res = await handleEvent(event)
        let resJson = JSON.parse(res.body);
       
        expect(res.statusCode).to.equal(200);
        expect(resJson.additionalLanguages).to.be.undefined;
    });

    it("should return 200 for v2.1", async () => {
        const notificationRequestStatusJSON = fs.readFileSync("./src/test/notificationRequestStatusV23.json");
        let notificationRequestStatusV21 = JSON.parse(notificationRequestStatusJSON);

        process.env = Object.assign(process.env, {
            PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it/delivery/v2.1/",
        });
        
        let url = `${process.env.PN_DELIVERY_URL}/requests?`;
        const params = new URLSearchParams({
            notificationRequestId: 'validNotificationRequestId'
        });

        mock.onGet(url, { params: params }).reply(200, notificationRequestStatusV21, { "Content-Type": "application/json" });

        const event = {
            path: '/delivery/v2.1/requests',
            httpMethod: "GET",
            headers: {},
            requestContext: {
                authorizer: {},
            },
            queryStringParameters: {
                notificationRequestId: 'validNotificationRequestId'
            }
        }
        
        const res = await handleEvent(event)
        let resJson = JSON.parse(res.body);
       
        expect(res.statusCode).to.equal(200);
        expect(resJson.paFee).to.be.equal(100);
        expect(resJson.vat).to.be.equal(undefined);
        expect(resJson.additionalLanguages).to.be.undefined;
    });

    it("should return 200 for v2.4", async () => {
        const notificationRequestStatusJSON = fs.readFileSync("./src/test/notificationRequestStatusV23.json");
        let notificationRequestStatusV24 = JSON.parse(notificationRequestStatusJSON);

        process.env = Object.assign(process.env, {
            PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it/delivery/v2.4/",
        });
        
        let url = `${process.env.PN_DELIVERY_URL}/requests?`;
        const params = new URLSearchParams({
            notificationRequestId: 'validNotificationRequestId'
        });

        mock.onGet(url, { params: params }).reply(200, notificationRequestStatusV24, { "Content-Type": "application/json" });

        const event = {
            path: '/delivery/v2.4/requests',
            httpMethod: "GET",
            headers: {},
            requestContext: {
                authorizer: {},
            },
            queryStringParameters: {
                notificationRequestId: 'validNotificationRequestId'
            }
        }
        
        const res = await handleEvent(event)
        let resJson = JSON.parse(res.body);
       
        expect(res.statusCode).to.equal(200);
        expect(resJson.paFee).to.be.equal(100);
        expect(resJson.vat).to.be.equal(22);
        expect(resJson.additionalLanguages).to.be.undefined;
    });

    it("should return 200 with paProtocolNumber and idempotenceToken", async () => {
        const notificationRequestStatusJSON = fs.readFileSync("./src/test/notificationRequestStatusV23.json");
        let notificationRequestStatusV21 = JSON.parse(notificationRequestStatusJSON);


        process.env = Object.assign(process.env, {
            PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it/delivery",
        });
        
        let url = `${process.env.PN_DELIVERY_URL}/requests?`;
        const params = new URLSearchParams({
            paProtocolNumber: 'validPaProtocolNumber',
            idempotenceToken: 'validIdempotenceToken'
        });

        mock.onGet(url, { params: params }).reply(200, notificationRequestStatusV21, { "Content-Type": "application/json" });

        const event = {
            path: '/delivery/requests',
            httpMethod: "GET",
            headers: {},
            requestContext: {
                authorizer: {},
            },
            queryStringParameters: {
                paProtocolNumber: 'validPaProtocolNumber',
                idempotenceToken: 'validIdempotenceToken'
            }
        }
        
        const res = await handleEvent(event)
       
        expect(res.statusCode).to.equal(200);
    });

    it("Unable to map more than 2 payments", async () => {

        const notificationRequestStatusJSON = fs.readFileSync("./src/test/notificationRequestStatusV23.json");
        let notificationRequestStatusV21 = JSON.parse(notificationRequestStatusJSON);

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

        notificationRequestStatusV21.recipients[1].payments.push(extraPayment)

        process.env = Object.assign(process.env, {
            PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
        });
        
        let url = `${process.env.PN_DELIVERY_URL}/requests?`;

        const params = new URLSearchParams({
            notificationRequestId: 'validNotificationRequestId'
        });

        mock.onGet(url, { params: params }).reply(200, notificationRequestStatusV21, { "Content-Type": "application/json" });

        const event = {
            path: '/delivery/requests',
            httpMethod: "GET",
            headers: {},
            requestContext: {
                authorizer: {},
            },
            queryStringParameters: {
                notificationRequestId: 'validNotificationRequestId'
            }
        }
        const res = await handleEvent(event)
        expect(res.statusCode).to.equal(400);

    });

    it("Unable to map F24 payment", async () => {

        const notificationRequestStatusExtraJSON = fs.readFileSync("./src/test/notificationRequestStatusV23ExtraPayment.json");
        const notificationRequestStatusV21Extra = JSON.parse(notificationRequestStatusExtraJSON);

        process.env = Object.assign(process.env, {
            PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
        });
        
        let url = `${process.env.PN_DELIVERY_URL}/requests?`;
        const params = new URLSearchParams({
            notificationRequestId: 'validNotificationRequestId'
        });

        mock.onGet(url, { params: params }).reply(200, notificationRequestStatusV21Extra, { "Content-Type": "application/json" });

        const event = {
            path: '/delivery/requests',
            httpMethod: "GET",
            headers: {},
            requestContext: {
                authorizer: {},
            },
            queryStringParameters: {
                notificationRequestId: 'validNotificationRequestId'
            }
        }
        const res = await handleEvent(event)
        expect(res.statusCode).to.equal(400);

    });

    it("PagoPaIntMode value not supported", async () => {

        const notificationRequestStatusJSON = fs.readFileSync("./src/test/notificationRequestStatusV23.json");
        const notificationRequestStatusV21 = JSON.parse(notificationRequestStatusJSON);

        notificationRequestStatusV21.pagoPaIntMode = 'ASYNC'

        process.env = Object.assign(process.env, {
            PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
        });
        
        let url = `${process.env.PN_DELIVERY_URL}/requests?`;
        const params = new URLSearchParams({
            notificationRequestId: 'validNotificationRequestId'
        });

        mock.onGet(url, { params: params }).reply(200, notificationRequestStatusV21, { "Content-Type": "application/json" });

        const event = {
            path: '/delivery/requests',
            httpMethod: "GET",
            headers: {},
            requestContext: {
                authorizer: {},
            },
            queryStringParameters: {
                notificationRequestId: 'validNotificationRequestId'
            }
        }
        const res = await handleEvent(event)
        expect(res.statusCode).to.equal(400);

    });


});