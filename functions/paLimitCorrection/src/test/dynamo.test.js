const { expect } = require('chai');
const sinon = require('sinon');
const { DynamoDBDocumentClient, QueryCommand, UpdateCommand } = require('@aws-sdk/lib-dynamodb');
const { getNotificationMetadata, getNotificationLimit, updateNotificationLimit } = require('../app/lib/dynamo');

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

  describe('getNotificationLimit', () => {
    it('should retrieve an item successfully', async () => {
      const yearMonth = '2025##01';
      const mockItem = [{ "pk": "testPk", "paId": "testPaId", "yearMonth": "2025##01", "residualLimit": 10 }];
      docClientStub.resolves({ Items: mockItem });

      const result = await getNotificationLimit(yearMonth);

      expect(docClientStub.firstCall.args[0]).to.be.an.instanceof(QueryCommand);
      expect(result).to.deep.equal(mockItem);
    });
  });

  describe('getNotificationMetadata', () => {
    it('should retrieve an item successfully', async () => {
      const key = 'testKey';
      const sortKey = 'testSantAt';
      const mockItem = [{ "iun_recipientId": "testIun", "sentAt": "testSantAt", "senderId_creationMonth": "testKey" }];
      docClientStub.resolves({ Items: mockItem });

      const result = await getNotificationMetadata(key, sortKey);

      expect(docClientStub.firstCall.args[0]).to.be.an.instanceof(QueryCommand);
      expect(result).to.deep.equal(mockItem);
    });
    it('should retrive items successfully with exclusiveStartKey', async () => {
      const key = 'testKey';
      const sortKey = 'testSantAt';
      const mockItem = [{ "iun_recipientId": "testIun", "sentAt": "testSantAt", "senderId_creationMonth": "testKey" }];
      docClientStub.onFirstCall().resolves({ Items: mockItem, LastEvaluatedKey: 'testLastEvaluatedKey' });
      docClientStub.onSecondCall().resolves({ Items: mockItem });

      const result = await getNotificationMetadata(key, sortKey);

      expect(docClientStub.firstCall.args[0]).to.be.an.instanceof(QueryCommand);
      expect(docClientStub.secondCall.args[0]).to.be.an.instanceof(QueryCommand);
      expect(result).to.deep.equal(mockItem.concat(mockItem));
    });
  });

  describe('updateNotificationLimit', () => {
    it('should update an item successfully', async () => {
      const key = 'testKey';
      const dailyCounterX = 'testDailyCounterX';
      const deltaDailyCounter = 10;
      docClientStub.resolves({ Items: [] });

      await updateNotificationLimit(key, dailyCounterX, deltaDailyCounter);

      expect(docClientStub.firstCall.args[0]).to.be.an.instanceof(UpdateCommand);
    });
  });

})