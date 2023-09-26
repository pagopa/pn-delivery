const { handleEvent } = require("../app/eventHandler.js");
const { expect } = require("chai");
const fetchMock = require("fetch-mock");
const fs = require("fs");



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

        beforeEach(() => {
            fetchMock.reset();
        });

        process.env = Object.assign(process.env, {
            PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
        });
        
        let url = `${process.env.PN_DELIVERY_URL}/delivery/requests`;

        fetchMock.mock(url, {
            status: 404,
            body: {},
            headers: { "Content-Type": "application/json" },
        });

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

        beforeEach(() => {
            fetchMock.reset();
        });

        process.env = Object.assign(process.env, {
            PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
        });
        
        let url = `${process.env.PN_DELIVERY_URL}/delivery/requests`;

        fetchMock.mock(url, {
            status: 400,
            body: {},
            headers: { "Content-Type": "application/json" },
        });

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
        const notificationRequestStatusJSON = fs.readFileSync("./src/test/notificationRequestStatusV21.json");
        let notificationRequestStatusV21 = JSON.parse(notificationRequestStatusJSON);

        beforeEach(() => {
            fetchMock.reset();
        });

        process.env = Object.assign(process.env, {
            PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
        });
        
        let url = `${process.env.PN_DELIVERY_URL}/delivery/requests`;

        fetchMock.mock(url, {
            status: 200,
            body: notificationRequestStatusV21,
            headers: { "Content-Type": "application/json" },
        });

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
       
        expect(res.statusCode).to.equal(200);
    });


});