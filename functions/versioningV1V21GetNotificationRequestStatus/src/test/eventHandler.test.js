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
                notificationRequestId: 'validNotificationRequestId'
            }
        }
        
        const res = await handleEvent(event)
       
        expect(res.statusCode).to.equal(200);
    });

    it("Unable to map more than 2 payments", async () => {

        const notificationRequestStatusJSON = fs.readFileSync("./src/test/notificationRequestStatusV21.json");
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
                notificationRequestId: 'validNotificationRequestId'
            }
        }
        const res = await handleEvent(event)
        expect(res.statusCode).to.equal(400);

    });

    it("Unable to map more attachment different sha", async () => {

        const notificationRequestStatusJSON = fs.readFileSync("./src/test/notificationRequestStatusV21.json");
        let notificationRequestStatusV21 = JSON.parse(notificationRequestStatusJSON);

        notificationRequestStatusV21.recipients[1].payments[1].pagoPa.attachment.digests.sha256 = 'differentSha';
        
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
                notificationRequestId: 'validNotificationRequestId'
            }
        }
        const res = await handleEvent(event)
        expect(res.statusCode).to.equal(400);

    });

    it("Unable to map F24 payment", async () => {

        const notificationRequestStatusExtraJSON = fs.readFileSync("./src/test/notificationRequestStatusV21ExtraPayment.json");
        const notificationRequestStatusV21Extra = JSON.parse(notificationRequestStatusExtraJSON);
        
        beforeEach(() => {
            fetchMock.reset();
        });

        process.env = Object.assign(process.env, {
            PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
        });
        
        let url = `${process.env.PN_DELIVERY_URL}/delivery/requests`;

        fetchMock.mock(url, {
            status: 200,
            body: notificationRequestStatusV21Extra,
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
                notificationRequestId: 'validNotificationRequestId'
            }
        }
        const res = await handleEvent(event)
        expect(res.statusCode).to.equal(400);

    });


});