const { expect } = require("chai");
const { createEvent } = require('./utils.js');
const { handleEvent } = require("../app/eventHandler.js");
const fetchMock = require("fetch-mock");
const proxyquire = require("proxyquire").noPreserveCache();
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
        
        it("newNotificationRequestV21 accepted", async () => {
            
        beforeEach(() => {
            fetchMock.reset();
        });

        const eventHandler = proxyquire
            .noCallThru()
            .load("../app/eventHandler.js", {});

        let url = 'delivery/requests/';

        fetchMock.mock(url, {
            status: 202,
            body: newNotificationRequesV1,
            headers: { "Content-Type": "application/json" },
        });

        const event = createEvent('/delivery/requests', 'POST', newNotificationRequesV1)

        const response = await eventHandler.handleEvent(event);
        expect(response.statusCode).to.equal(202);
            
        });
    });
});