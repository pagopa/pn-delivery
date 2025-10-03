const { expect } = require('chai');
const sinon = require('sinon');
const { DynamoDBDocumentClient, UpdateCommand } = require('@aws-sdk/lib-dynamodb');
const { updateWithConditionalStatus } = require('../app/lib/dynamo');

describe('dynamo.js tests', () => {

  let docClientStub;
  let logStub;

  beforeEach(() => {
    docClientStub = sinon.stub(DynamoDBDocumentClient.prototype, 'send');
    logStub = sinon.stub(console, 'log');
  });

  afterEach(() => {
    sinon.restore();
  });

  describe('updateWithConditionalStatus', () => {
    it('should put rework successfully with state REWORK_CREATED', async () => {
      docClientStub.resolves({});
      const item = {
        iun:   "iun",
        reworkId:   "reworkId",
        flagInvalidatedIds: ["id1","id2","id3"],
        status: "READY",
        updatedAt: new Date()
      };
      await updateWithConditionalStatus("pn-NotificationReworks", item, 'testKey', "CREATED");

      expect(docClientStub.firstCall.args[0]).to.be.an.instanceof(UpdateCommand);
    });

    it('should log and not throw error if ConditionalCheckFailedException occurs', async () => {
      const error = new Error();
      error.name = 'ConditionalCheckFailedException';
      docClientStub.rejects(error);

      const item = {
        iun:   "iun",
        reworkId:   "reworkId",
        flagInvalidatedIds: ["id1","id2","id3"],
        status: "READY",
        updatedAt: new Date()
      };
      await updateWithConditionalStatus("pn-NotificationReworks", item, 'testKey', "CREATED");

      expect(logStub.firstCall.args[0]).to.be.equal('update not necessary for item with pk: iun and reworkId: reworkId on table: pn-NotificationReworks');
    });

    it('should throw error if other exception occurs', async () => {
      const error = new Error('Test error');
      docClientStub.rejects(error);

      const item = {
        iun:   "iun",
        reworkId:   "reworkId",
        flagInvalidatedIds: ["id1","id2","id3"],
        status: "READY",
        updatedAt: new Date()
      };
      
      try {
        await updateWithConditionalStatus("pn-NotificationReworks", item, 'testKey', "CREATED");
      } catch (error) {
        expect(error.message).to.equal('Test error');
      }
    });
  });
})