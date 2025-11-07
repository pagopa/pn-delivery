const { expect } = require('chai');
const proxyquire = require('proxyquire').noCallThru();

describe('eventHandler tests', () => {
  const event = require("./kinesis.event.example.json");

  it('should process all records successfully', async () => {
    const mockSQSClient = {
      send: async () => ({}) // Mock per un successo
    };

    const lambda = proxyquire.noCallThru().load("../app/handler.js", {
      "@aws-sdk/client-sqs": {
        SQSClient: class {
          constructor() {
            return mockSQSClient;
          }
        },
        SendMessageCommand: class {},
      },
      "./lib/utils.js": {
        extractKinesisData: () => {
          return [{...event.Records[0].kinesis.data, kinesisSeqNumber: "test",}];
        },
      },
    });

    const res = await lambda.eventHandler(event);

    expect(res).deep.equals({
      batchItemFailures: [],
    });
  });

  it('should handle sqs error and immediately return batchItemFailures', async () => {

    const mockSQSClient = {
      send: async () => {throw new Error("Error sending message to SQS");} // Mock per un errore,
    };

    const lambda = proxyquire.noCallThru().load("../app/handler.js", {
      "@aws-sdk/client-sqs": {
        SQSClient: class {
          constructor() {
            return mockSQSClient;
          }
        },
        SendMessageCommand: class {},
      },
      "./lib/utils.js": {
        extractKinesisData: () => {
          return [{...event.Records[0].kinesis.data, kinesisSeqNumber: "message-1",}];
        },
      },
    });

    const res = await lambda.eventHandler(event);
    expect(res).deep.equals({
      batchItemFailures: [{itemIdentifier: 'message-1'}],
    });
  });

  it("test no data to persist", async () => {
    const event = {};

    const lambda = proxyquire.noCallThru().load("../app/handler.js", {
      "./lib/utils.js": {
        extractKinesisData: () => {
          return [];
        },
      },
    });

    const res = await lambda.eventHandler(event);
    expect(res).deep.equals({
      batchItemFailures: [],
    });
  });
});