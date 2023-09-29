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
                body: badNewNotificationRequest
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
            PN_DELIVERY_URL: "https://api.dev.notifichedigitali.it",
          });

        const eventHandler = proxyquire
            .noCallThru()
            .load("../app/eventHandler.js", {});

        let url = `${process.env.PN_DELIVERY_URL}/delivery/requests`;

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
            body: newNotificationRequesV1
          };

        //const event = createEvent('/delivery/requests', 'POST', newNotificationRequesV1)

        const response = await handleEvent(event);
        expect(response.status).to.equal(202);
            
        });
    });
});