const { expect } = require('chai');
const sinon = require('sinon');
const { DynamoDBDocumentClient, GetCommand, DeleteCommand, UpdateCommand } = require('@aws-sdk/lib-dynamodb');
const { getItem, deleteItem, updateMetadata, buildMetadataUpdateParams } = require('../app/lib/dynamo');
const { ItemNotFoundException } = require('../app/lib/exceptions');

describe('dynamo.js tests', () => {

  let docClientStub;
  let logStub;
  let warnStub;

  beforeEach(() => {
    docClientStub = sinon.stub(DynamoDBDocumentClient.prototype, 'send');
    logStub = sinon.stub(console, 'log');
    warnStub = sinon.stub(console, 'warn');
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

  describe('updateMetadata', () => {
    it('should update metadata successfully without replacing key attributes', async () => {
      docClientStub.resolves({});

      const item = {
        testKey: 'key',
        sentAt: '2025-01-01T00:00:00Z',
        notificationStatus: 'DELIVERING',
        notificationStatusTimestamp: '2025-01-01T00:00:00Z',
      };
      await updateMetadata('testTable', item, 'testKey');

      const command = docClientStub.firstCall.args[0];
      expect(command).to.be.an.instanceof(UpdateCommand);
      expect(command.input.Key).to.deep.equal({
        testKey: 'key',
        sentAt: '2025-01-01T00:00:00Z',
      });
      expect(command.input.UpdateExpression).to.include('#notificationStatus = :notificationStatus');
      expect(command.input.UpdateExpression).to.not.include('#testKey');
      expect(command.input.UpdateExpression).to.not.include('#sentAt');
    });

    it('should log and not throw error if ConditionalCheckFailedException occurs', async () => {
      const error = new Error();
      error.name = 'ConditionalCheckFailedException';
      docClientStub.rejects(error);

      const item = { testKey: 'key', notificationStatus: "DELIVERING" };
      await updateMetadata('testTable', item, 'testKey');

      expect(warnStub.firstCall.args[0]).to.be.equal('[metadataStatusUpdater] DynamoDB UpdateItem skipped by timestamp condition: table=testTable, key={"testKey":"key"}, status=DELIVERING');
    });

    it('should throw error if other exception occurs', async () => {
      const error = new Error('Test error');
      docClientStub.rejects(error);

      const item = { notificationStatusTimestamp: '2025-01-01T00:00:00Z' };
      try {
        await updateMetadata('testTable', item, 'testKey')
      } catch (error) {
        expect(error.message).to.equal('Test error');
      }
    });
  });

  describe('buildMetadataUpdateParams', () => {
    it('should omit undefined optional fields from the update expression', () => {
      const params = buildMetadataUpdateParams('testTable', {
        testKey: 'key',
        sentAt: '2025-01-01T00:00:00Z',
        notificationStatus: 'DELIVERING',
        notificationStatusTimestamp: '2025-01-01T00:00:00Z',
        notificationGroup: undefined,
      }, 'testKey');

      expect(params.UpdateExpression).to.not.include('#notificationGroup');
      expect(params.ExpressionAttributeValues).to.not.have.property(':notificationGroup');
    });

    it('should ignore fields that are not explicitly mapped', () => {
      const params = buildMetadataUpdateParams('testTable', {
        testKey: 'key',
        sentAt: '2025-01-01T00:00:00Z',
        notificationStatus: 'DELIVERING',
        notificationStatusTimestamp: '2025-01-01T00:00:00Z',
        unexpectedField: 'must not be written',
      }, 'testKey');

      expect(params.UpdateExpression).to.not.include('#unexpectedField');
      expect(params.ExpressionAttributeValues).to.not.have.property(':unexpectedField');
    });

    it('should update delegation records using their partition key', () => {
      const params = buildMetadataUpdateParams('delegationTable', {
        iun_recipientId_delegateId_groupId: 'iun##recipient##delegate',
        sentAt: '2025-01-01T00:00:00Z',
        notificationStatus: 'DELIVERING',
        notificationStatusTimestamp: '2025-01-01T00:00:00Z',
      }, 'iun_recipientId_delegateId_groupId');

      expect(params.Key).to.deep.equal({
        iun_recipientId_delegateId_groupId: 'iun##recipient##delegate',
        sentAt: '2025-01-01T00:00:00Z',
      });
      expect(params.UpdateExpression).to.not.include('#iun_recipientId_delegateId_groupId');
      expect(params.UpdateExpression).to.not.include('#sentAt');
    });
  });
})