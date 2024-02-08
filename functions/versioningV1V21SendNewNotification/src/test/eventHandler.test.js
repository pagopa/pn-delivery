const { expect } = require("chai");
const { createEvent } = require('./utils.js');
const { handleEvent } = require("../app/eventHandler.js");
const proxyquire = require("proxyquire").noPreserveCache();
const fs = require("fs");
const axios = require('axios');
var MockAdapter = require("axios-mock-adapter");
var mock = new MockAdapter(axios);
const newNotificationRequesV1 = require("./newNotificationRequestV1.json");
const newNotificationRequestV21 = require("./newNotificationRequestV21.json");

describe('EventHandler Testing', () => {
    describe('handleEvent Testing', () => {
        it('should return 404 when httpMethod/path isn\'t right', async () => {
            const event = createEvent('/delivery/new-notification', 'GET', newNotificationRequesV1)
            const res = await handleEvent(event)
            expect(res).to.not.be.null;
            expect(res).to.not.be.undefined;
            expect(res.statusCode).to.equal(404);
        });

        it('should return 404 when httpMethod/path isn\'t right', async () => {
            const event = createEvent('/delivery/v2.1/new-notification', 'GET', newNotificationRequesV1)
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

        it('should return 400 when no vat or paFee for ASYNC', async () => {
            const notificationJSON = fs.readFileSync("./src/test/newNotificationRequestV21.json");
            let badNewNotificationRequest = JSON.parse(notificationJSON);
            badNewNotificationRequest.pagoPaIntMode = 'ASYNC';

            const event = {
                headers: {},
                requestContext: {
                  authorizer: {},
                },
                resource: "/delivery/requests",
                path: "/delivery/v2.1/requests",
                httpMethod: 'POST',
                body: JSON.stringify(badNewNotificationRequest)
            };

            const res = await handleEvent(event)
            expect(res).to.not.be.null;
            expect(res).to.not.be.undefined;
            expect(res.statusCode).to.equal(400);
        });

        it('should return 400 when no vat or paFee for F24', async () => {
            const notificationJSON = fs.readFileSync("./src/test/newNotificationRequestV21.json");
            let badNewNotificationRequest = JSON.parse(notificationJSON);
            const f24Payment = {
                f24: {
                    title: "f24Test",
                    applyCost: true,
                    metadataAttachment: {}
                } 
            }
            badNewNotificationRequest.recipients[0].payments.push(f24Payment)

            const event = {
                headers: {},
                requestContext: {
                  authorizer: {},
                },
                resource: "/delivery/requests",
                path: "/delivery/v2.1/requests",
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

            process.env = Object.assign(process.env, {
                PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it/delivery",
            });

            const eventHandler = proxyquire
                .noCallThru()
                .load("../app/eventHandler.js", {});

            let url = `${process.env.PN_DELIVERY_URL}/requests`;

            mock.onPost(url).reply(202, {message: 'Accepted'});

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

        it("newNotificationRequestV23 accepted", async () => {

            process.env = Object.assign(process.env, {
                PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it/delivery",
            });

            const eventHandler = proxyquire
                .noCallThru()
                .load("../app/eventHandler.js", {});

            let url = `${process.env.PN_DELIVERY_URL}/v2.1/requests`;

            mock.onPost(url).reply(202, {message: 'Accepted'});

            const event = {
                headers: {},
                requestContext: {
                authorizer: {},
                },
                resource: "/delivery/requests",
                path: "/delivery/v2.1/requests",
                httpMethod: 'POST',
                body: JSON.stringify(newNotificationRequestV21)
            };

            const response = await handleEvent(event);
            expect(response.statusCode).to.equal(202);
            
        });

        it("newNotificationRequestV21 with NO pagoPaIntMode, accepted", async () => {

            process.env = Object.assign(process.env, {
                PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it/delivery",
            });

            const eventHandler = proxyquire
                .noCallThru()
                .load("../app/eventHandler.js", {});

            let url = `${process.env.PN_DELIVERY_URL}/requests`;

            mock.onPost(url).reply(202, {message: 'Accepted'});

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

        it("newNotificationRequestV21 bad request", async () => {

            process.env = Object.assign(process.env, {
                PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it/delivery",
            });

            const eventHandler = proxyquire
                .noCallThru()
                .load("../app/eventHandler.js", {});

            let url = `${process.env.PN_DELIVERY_URL}/requests`;

            mock.onPost(url).reply(400, {
                problem: {
                    type: 'GENERIC_ERROR',
                    status: 400,
                    title: 'Bad Request',
                    errors: [
                        {
                            code: 'PN_GENERIC_INVALIDPARAMETER',
                            detail: 'SEND accepts only numerical taxId for PG recipient 0'
                        }
                    ]
                }
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
            expect(response.statusCode).to.equal(400);
            
        });

        it("newNotificationRequestV21 conflict", async () => {

            process.env = Object.assign(process.env, {
                PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it/delivery",
            });

            const eventHandler = proxyquire
                .noCallThru()
                .load("../app/eventHandler.js", {});

            let url = `${process.env.PN_DELIVERY_URL}/requests`;

            mock.onPost(url).reply(409, {
                problem: {
                    type: 'GENERIC_ERROR',
                    status: 409,
                    title: 'Conflict',
                    detail: 'Some resources are in conflict',
                    errors: [
                        {
                            code: 'PN_GENERIC_INVALIDPARAMETER_DUPLICATED',
                            element: 'Duplicated notification for creditorTaxId##noticeCode',
                            detail: 'Duplicated notification for creditorTaxId##noticeCode=80007530738##302025180271322709'
                        }
                    ]
                }
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
            expect(response.statusCode).to.equal(409);
            
        });
        
        it("newNotificationRequestV21 abort request", async () => {

            process.env = Object.assign(process.env, {
                PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it/delivery",
            });

            const eventHandler = proxyquire
                .noCallThru()
                .load("../app/eventHandler.js", {});

            let url = `${process.env.PN_DELIVERY_URL}/requests`;

            mock.onPost(url).abortRequest();

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
            expect(response.statusCode).to.equal(500);
            
        });
    });
});