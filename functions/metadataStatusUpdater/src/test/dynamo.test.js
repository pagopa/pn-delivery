const { expect } = require('chai');
const sinon = require('sinon');
const { DynamoDBDocumentClient, GetCommand, DeleteCommand, PutCommand } = require('@aws-sdk/lib-dynamodb');
const { getItem, deleteItem, putMetadata } = require('../app/lib/dynamo');
const { ItemNotFoundException } = require('../app/lib/exceptions');

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

  describe('getItem', () => {
    it('should retrieve an item successfully', async () => {
      const mockItem = { key: 'value' };
      docClientStub.resolves({ Item: mockItem });

      const result = await getItem('testTable', { id: 'testId' });

      expect(docClientStub.firstCall.args[0]).to.be.an.instanceof(GetCommand);
      expect(result).to.deep.equal(mockItem);
    });

    it('should throw ItemNotFoundException if item is not found', async () => {
      docClientStub.resolves({ Item: undefined });

      try {
        await getItem('testTable', { id: 'testId' });
      } catch (error) {
        expect(error).to.be.an.instanceof(ItemNotFoundException);
      }
    });
  });

  describe('deleteItem', () => {
    it('should delete an item successfully', async () => {
      docClientStub.resolves({});

      await deleteItem('testTable', { id: 'testId' }, 'iun');

      expect(docClientStub.firstCall.args[0]).to.be.an.instanceof(DeleteCommand);
    });

    it('should log and throw error if ConditionalCheckFailedException occurs', async () => {
      const error = new Error('Test error');
      error.name = 'ConditionalCheckFailedException';
      docClientStub.rejects(error);

      try {
        await deleteItem('testTable', { id: 'testId' }, 'iun');
      } catch (error) {
        expect(error.message).to.equal('Test error');
      }
    });
  });

  describe('putMetadata', () => {
    it('should put metadata successfully', async () => {
      docClientStub.resolves({});

      const item = { notificationStatusTimestamp: '2025-01-01T00:00:00Z' };
      await putMetadata('testTable', item, 'testKey');

      expect(docClientStub.firstCall.args[0]).to.be.an.instanceof(PutCommand);
    });

    it('should log and not throw error if ConditionalCheckFailedException occurs', async () => {
      const error = new Error();
      error.name = 'ConditionalCheckFailedException';
      docClientStub.rejects(error);

      const item = { testKey: 'key', notificationStatus: "DELIVERING" };
      await putMetadata('testTable', item, 'testKey');

      expect(logStub.firstCall.args[0]).to.be.equal('update not necessary for item with pk: key and status: DELIVERING on table: testTable');
    });

    it('should throw error if other exception occurs', async () => {
      const error = new Error('Test error');
      docClientStub.rejects(error);

      const item = { notificationStatusTimestamp: '2025-01-01T00:00:00Z' };
      try {
        await putMetadata('testTable', item, 'testKey')
      } catch (error) {
        expect(error.message).to.equal('Test error');
      }
    });
  });
})