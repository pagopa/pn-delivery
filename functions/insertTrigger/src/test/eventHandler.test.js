/* eslint-disable no-unused-vars */
"use strict";

const { expect } = require("chai");
const proxyquire = require("proxyquire").noCallThru();
const { describe, it } = require("mocha");

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/**
 * Builds a minimal DynamoDB Stream record.
 * @param {string} iun
 * @param {string} senderPaId
 * @param {string} eventName         default "INSERT"
 * @param {string} communicationType optional — when provided simulates an informal notification
 */
function makeDynamoRecord(iun, senderPaId = "pa-001", eventName = "INSERT", communicationType = undefined) {
  const newImage = { senderPaId: { S: senderPaId } };
  if (communicationType !== undefined) {
    newImage.communicationType = { S: communicationType };
  }

  return {
    eventName,
    dynamodb: {
      Keys: { iun: { S: iun } },
      NewImage: newImage,
    },
  };
}

// Valid IUN samples
const FORMAL_IUN   = "ABCD-EFGH-IJKL-202301-A-1";   // last char digit  → formal/legal
const INFORMAL_IUN = "ABCD-EFGH-IJKL-202301-A-X";   // last char letter → informal
const INVALID_IUN  = "invalidIun";

// ---------------------------------------------------------------------------
// messageUtils
// ---------------------------------------------------------------------------

describe("messageUtils", () => {
  const { isRecordToSend, mapMessage } = require("../app/messageUtils");

  // ── isRecordToSend ────────────────────────────────────────────────────────

  describe("isRecordToSend()", () => {
    it("returns false when eventName is not INSERT", () => {
      expect(isRecordToSend(makeDynamoRecord(FORMAL_IUN, "pa", "MODIFY"))).to.be.false;
      expect(isRecordToSend(makeDynamoRecord(FORMAL_IUN, "pa", "REMOVE"))).to.be.false;
    });

    it("returns true for a formally valid IUN (digit suffix)", () => {
      expect(isRecordToSend(makeDynamoRecord(FORMAL_IUN))).to.be.true;
    });

    it("returns true for an informally valid IUN (letter suffix)", () => {
      expect(isRecordToSend(makeDynamoRecord(INFORMAL_IUN))).to.be.true;
    });

    it("returns false when IUN does not match either pattern", () => {
      expect(isRecordToSend(makeDynamoRecord(INVALID_IUN))).to.be.false;
    });
  });

  // ── mapMessage ────────────────────────────────────────────────────────────

  describe("mapMessage()", () => {
    describe("without communicationType", () => {
      let message;

      before(() => {
        message = mapMessage(makeDynamoRecord(FORMAL_IUN, "pa-test"));
      });

      it("sets Id to iun + '_start'", () => {
        expect(message.Id).to.equal(`${FORMAL_IUN}_start`);
      });

      it("sets DelaySeconds to 0", () => {
        expect(message.DelaySeconds).to.equal(0);
      });

      it("sets MessageGroupId with 'DELIVERY-' prefix", () => {
        expect(message.MessageGroupId).to.equal(`DELIVERY-${FORMAL_IUN}`);
      });

      it("sets MessageDeduplicationId to iun + '_start'", () => {
        expect(message.MessageDeduplicationId).to.equal(`${FORMAL_IUN}_start`);
      });

      it("sets iun attribute correctly", () => {
        expect(message.MessageAttributes.iun.StringValue).to.equal(FORMAL_IUN);
      });

      it("sets eventType attribute to NEW_NOTIFICATION", () => {
        expect(message.MessageAttributes.eventType.StringValue).to.equal("NEW_NOTIFICATION");
      });

      it("sets publisher attribute to DELIVERY", () => {
        expect(message.MessageAttributes.publisher.StringValue).to.equal("DELIVERY");
      });

      it("encodes paId in MessageBody JSON", () => {
        const body = JSON.parse(message.MessageBody);
        expect(body.paId).to.equal("pa-test");
      });

      it("sets communicationType to undefined in MessageBody when not present in record", () => {
        const body = JSON.parse(message.MessageBody);
        expect(body.communicationType).to.be.undefined;
      });

      it("sets createdAt to a valid ISO date string", () => {
        const val = message.MessageAttributes.createdAt.StringValue;
        expect(new Date(val).toISOString()).to.equal(val);
      });
    });

    describe("with communicationType", () => {
      let message;

      before(() => {
        message = mapMessage(makeDynamoRecord(INFORMAL_IUN, "pa-test", "INSERT", "INFORMAL"));
      });

      it("encodes communicationType in MessageBody JSON", () => {
        const body = JSON.parse(message.MessageBody);
        expect(body.communicationType).to.equal("INFORMAL");
      });

      it("still encodes paId correctly", () => {
        const body = JSON.parse(message.MessageBody);
        expect(body.paId).to.equal("pa-test");
      });

      it("still sets iun attribute correctly", () => {
        expect(message.MessageAttributes.iun.StringValue).to.equal(INFORMAL_IUN);
      });
    });
  });
});

// ---------------------------------------------------------------------------
// eventBridgeFunctions
// ---------------------------------------------------------------------------

describe("eventBridgeFunctions", () => {
  let sendMessages;
  let capturedEntries;
  let stubbedResponse;

  beforeEach(() => {
    capturedEntries = null;
    stubbedResponse = { FailedEntryCount: 0, Entries: [] };

    const FakeClient = class {
      async send(command) {
        capturedEntries = command.input.Entries;
        return stubbedResponse;
      }
    };

    const FakePutEventsCommand = class {
      constructor(input) { this.input = input; }
    };

    ({ sendMessages } = proxyquire("../app/eventBridgeFunctions", {
      "@aws-sdk/client-eventbridge": {
        EventBridgeClient: FakeClient,
        PutEventsCommand: FakePutEventsCommand,
      },
    }));
  });

  /**
   * Build a minimal message as produced by mapMessage().
   * @param {string}  iun
   * @param {string}  paId
   * @param {string|undefined} communicationType  when set → informal notification
   */
  function makeMessage(iun = FORMAL_IUN, paId = "pa-001", communicationType = undefined) {
    const body = { paId };
    if (communicationType !== undefined) body.communicationType = communicationType;

    return {
      Id: `${iun}_start`,
      DelaySeconds: 0,
      MessageGroupId: `DELIVERY-${iun}`,
      MessageDeduplicationId: `${iun}_start`,
      MessageAttributes: {
        iun: { DataType: "String", StringValue: iun },
        eventType: { DataType: "String", StringValue: "NEW_NOTIFICATION" },
      },
      MessageBody: JSON.stringify(body),
    };
  }

  it("calls PutEvents with one entry per message", async () => {
    await sendMessages([makeMessage(FORMAL_IUN), makeMessage(INFORMAL_IUN)]);
    expect(capturedEntries).to.have.lengthOf(2);
  });

  it("returns the response from EventBridge", async () => {
    const result = await sendMessages([makeMessage()]);
    expect(result).to.deep.equal(stubbedResponse);
  });

  it("sets Source correctly on each entry", async () => {
    await sendMessages([makeMessage()]);
    expect(capturedEntries[0].Source).to.equal("eventbridge.pn-delivery.insertTrigger");
  });

  it("includes iun in the serialised body", async () => {
    await sendMessages([makeMessage(FORMAL_IUN)]);
    const detail = JSON.parse(capturedEntries[0].Detail);
    expect(detail.body.iun).to.equal(FORMAL_IUN);
  });

  it("includes paId in the serialised body", async () => {
    await sendMessages([makeMessage(FORMAL_IUN, "pa-xyz")]);
    const detail = JSON.parse(capturedEntries[0].Detail);
    expect(detail.body.paId).to.equal("pa-xyz");
  });

  // ── getDetailTypeByNotification routing ───────────────────────────────────

  describe("DetailType routing via getDetailTypeByNotification()", () => {
    it("uses PnDeliveryValidationOutcomeEvent when communicationType is absent", async () => {
      await sendMessages([makeMessage(FORMAL_IUN, "pa-001")]);
      expect(capturedEntries[0].DetailType).to.equal("PnDeliveryValidationOutcomeEvent");
    });

    it("uses PnDeliveryValidationOutcomeEvent when communicationType is undefined", async () => {
      await sendMessages([makeMessage(FORMAL_IUN, "pa-001", undefined)]);
      expect(capturedEntries[0].DetailType).to.equal("PnDeliveryValidationOutcomeEvent");
    });

    it("uses PnDeliveryInformalValidationOutcomeEvent when communicationType is set", async () => {
      await sendMessages([makeMessage(INFORMAL_IUN, "pa-001", "INFORMAL")]);
      expect(capturedEntries[0].DetailType).to.equal("PnDeliveryInformalValidationOutcomeEvent");
    });

    it("uses PnDeliveryInformalValidationOutcomeEvent regardless of the communicationType value", async () => {
      await sendMessages([makeMessage(INFORMAL_IUN, "pa-001", "SOME_OTHER_TYPE")]);
      expect(capturedEntries[0].DetailType).to.equal("PnDeliveryInformalValidationOutcomeEvent");
    });
  });
});

// ---------------------------------------------------------------------------
// eventHandler
// ---------------------------------------------------------------------------

describe("eventHandler", () => {
  let handler;
  let sendMessagesCalled;
  let sendMessagesArg;

  const { mapMessage, isRecordToSend } = require("../app/messageUtils");

  beforeEach(() => {
    sendMessagesCalled = false;
    sendMessagesArg    = null;

    handler = proxyquire("../app/eventHandler", {
      "./eventBridgeFunctions": {
        sendMessages: async (msgs) => {
          sendMessagesCalled = true;
          sendMessagesArg    = msgs;
        },
      },
      "./messageUtils": { mapMessage, isRecordToSend },
    }).handler;
  });

  it("returns StatusCode 200", async () => {
    const event = { Records: [makeDynamoRecord(FORMAL_IUN)] };
    const result = await handler(event);
    expect(result.StatusCode).to.equal(200);
  });

  it("calls sendMessages with mapped messages for valid INSERT records", async () => {
    const event = { Records: [makeDynamoRecord(FORMAL_IUN, "pa-001")] };
    await handler(event);

    expect(sendMessagesCalled).to.be.true;
    expect(sendMessagesArg).to.have.lengthOf(1);
    expect(sendMessagesArg[0].MessageAttributes.iun.StringValue).to.equal(FORMAL_IUN);
  });

  it("does not call sendMessages when all records are filtered out", async () => {
    const event = { Records: [makeDynamoRecord(INVALID_IUN)] };
    await handler(event);
    expect(sendMessagesCalled).to.be.false;
  });

  it("skips non-INSERT records", async () => {
    const event = {
      Records: [
        makeDynamoRecord(FORMAL_IUN, "pa", "MODIFY"),
        makeDynamoRecord(FORMAL_IUN, "pa", "REMOVE"),
      ],
    };
    await handler(event);
    expect(sendMessagesCalled).to.be.false;
  });

  it("sends only valid records when the batch is mixed", async () => {
    const event = {
      Records: [
        makeDynamoRecord(FORMAL_IUN,   "pa-good"),
        makeDynamoRecord(INVALID_IUN,  "pa-bad"),
        makeDynamoRecord(INFORMAL_IUN, "pa-also-good"),
      ],
    };
    await handler(event);

    expect(sendMessagesCalled).to.be.true;
    expect(sendMessagesArg).to.have.lengthOf(2);
  });

  it("handles an empty Records array without throwing", async () => {
    const event = { Records: [] };
    const result = await handler(event);
    expect(result.StatusCode).to.equal(200);
    expect(sendMessagesCalled).to.be.false;
  });

  it("forwards communicationType through to the mapped message body", async () => {
    const event = {
      Records: [makeDynamoRecord(INFORMAL_IUN, "pa-001", "INSERT", "INFORMAL")],
    };
    await handler(event);

    expect(sendMessagesCalled).to.be.true;
    const body = JSON.parse(sendMessagesArg[0].MessageBody);
    expect(body.communicationType).to.equal("INFORMAL");
  });

  it("does not set communicationType in the message body when absent in the record", async () => {
    const event = {
      Records: [makeDynamoRecord(FORMAL_IUN, "pa-001")],
    };
    await handler(event);

    expect(sendMessagesCalled).to.be.true;
    const body = JSON.parse(sendMessagesArg[0].MessageBody);
    expect(body.communicationType).to.be.undefined;
  });
});