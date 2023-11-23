const { expect } = require("chai");
const { createEvent } = require('./utils.js');
const { handleEvent } = require("../app/eventHandler.js");
const fetchMock = require("fetch-mock");
const proxyquire = require("proxyquire").noPreserveCache();
const fs = require("fs");
const newNotificationRequesV1 = require("./newNotificationRequestV1.json");

describe('EventHandler Testing', () => {
    describe('handleEvent Testing', () => {
        it('should return 404 when httpMethod/path isn\'t right', async () => {
            const event = createEvent('/delivery/new-notification', 'GET', newNotificationRequesV1)
            const res = await handleEvent(event)
            expect(res).to.not.be.null;
            expect(res).to.not.be.undefined;
            expect(res.statusCode).to.equal(404);
        });

        it('should return 400 when noticeCode equals to noticeCodeAlternative', async () => {
            const notificationJSON = fs.readFileSync("./src/test/newNotificationRequestV1.json");
            let badNewNotificationRequest = JSON.parse(notificationJSON);
            badNewNotificationRequest.recipients[0].payment.noticeCode = 'fakeNoticeCode';
            badNewNotificationRequest.recipients[0].payment.noticeCodeAlternative = 'fakeNoticeCode';

            const event = {
                headers: {},
                requestContext: {
                  authorizer: {},
                },
                resource: "/delivery/requests",
                path: "/delivery/requests",
                httpMethod: 'POST',
                body: JSON.stringify(badNewNotificationRequest)
            };

            const res = await handleEvent(event)
            expect(res).to.not.be.null;
            expect(res).to.not.be.undefined;
            expect(res.statusCode).to.equal(400);
        });

        it('should return 400 when invalid pagoPaIntMode', async () => {
            const notificationJSON = fs.readFileSync("./src/test/newNotificationRequestV1.json");
            let badNewNotificationRequest = JSON.parse(notificationJSON);
            badNewNotificationRequest.pagoPaIntMode = 'ASYNC'

            const event = {
                headers: {},
                requestContext: {
                  authorizer: {},
                },
                resource: "/delivery/requests",
                path: "/delivery/requests",
                httpMethod: 'POST',
                body: JSON.stringify(badNewNotificationRequest)
            };

            const res = await handleEvent(event)
            expect(res).to.not.be.null;
            expect(res).to.not.be.undefined;
            expect(res.statusCode).to.equal(400);
        });
        
        it("newNotificationRequestV21 accepted", async () => {
            
            beforeEach(() => {
                fetchMock.reset();
            });

            process.env = Object.assign(process.env, {
                PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it/delivery",
            });

            const eventHandler = proxyquire
                .noCallThru()
                .load("../app/eventHandler.js", {});

            let url = `${process.env.PN_DELIVERY_URL}/requests`;

            fetchMock.post(url, {
                status: 202,
                body: { message: 'Accepted' },
            });

            const event = {
                headers: {},
                requestContext: {
                authorizer: {},
                },
                resource: "/delivery/requests",
                path: "/delivery/requests",
                httpMethod: 'POST',
                body: JSON.stringify(newNotificationRequesV1)
            };

            //const event = createEvent('/delivery/requests', 'POST', newNotificationRequesV1)

            const response = await handleEvent(event);
            expect(response.statusCode).to.equal(202);
            
        });

        it("newNotificationRequestV21 with NO pagoPaIntMode, accepted", async () => {
            
            beforeEach(() => {
                fetchMock.reset();
            });

            process.env = Object.assign(process.env, {
                PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it/delivery",
            });

            const eventHandler = proxyquire
                .noCallThru()
                .load("../app/eventHandler.js", {});

            let url = `${process.env.PN_DELIVERY_URL}/requests`;

            fetchMock.post(url, {
                status: 202,
                body: { message: 'Accepted' },
            });

            let notificationWithNOpagoPaIntMode = newNotificationRequesV1;
            delete notificationWithNOpagoPaIntMode.pagoPaIntMode;

            const event = {
                headers: {},
                requestContext: {
                authorizer: {},
                },
                resource: "/delivery/requests",
                path: "/delivery/requests",
                httpMethod: 'POST',
                body: JSON.stringify(notificationWithNOpagoPaIntMode)
            };

            const response = await handleEvent(event);
            expect(response.statusCode).to.equal(202);
            
        });
    });
});