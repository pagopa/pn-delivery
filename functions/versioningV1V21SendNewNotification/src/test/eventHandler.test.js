const { expect } = require("chai");
const { createEvent } = require('./utils.js');
const { handleEvent } = require("../app/eventHandler.js");
const newNotificationEvent = require("./newNotificationEvent.json");

describe('EventHandler Testing', () => {
    describe('handleEvent Testing', () => {
        it('should return 404 when httpMethod/path isn\'t right', async () => {
            const event = createEvent('/delivery/new-notification', 'GET', JSON.stringify({"events": newNotificationEvent}))
            const res = await handleEvent(event)
            expect(res).to.not.be.null;
            expect(res).to.not.be.undefined;
            expect(res.statusCode).to.equal(404);
        });
    });
});