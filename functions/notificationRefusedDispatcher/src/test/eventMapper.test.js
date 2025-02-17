const { expect } = require("chai");
const fs = require("fs");
const { mapEvent } = require("../app/lib/eventMapper");

describe("event mapper tests", function () {
    it("test mapping", async () => {
        const eventJSON = fs.readFileSync("./src/test/eventMapper.timeline.json");
        let event = JSON.parse(eventJSON);
  
        const res = await mapEvent(event);
        let body = JSON.parse(res.MessageBody);
        expect(body.iun).equal("abcd");
        expect(body.paId).equal("026e8c72-7944-4dcd-8668-f596447fec6d");
        expect(body.sentAt).equal("2023-01-20T14:48:00.000Z");
        expect(body.timelineId).equal("notification_viewed_creation_request;IUN_XLDW-MQYJ-WUKA-202302-A-1;RECINDEX_1");

        expect(res.MessageAttributes.publisher.StringValue).equal("delivery");
        expect(res.MessageAttributes.iun.StringValue).equal("abcd");
        expect(res.MessageAttributes.eventType.StringValue).equal("NOTIFICATION_REFUSED");
    });
});
  