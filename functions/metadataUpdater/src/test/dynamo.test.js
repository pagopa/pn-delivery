const { expect } = require('chai');
const sinon = require('sinon');
const { DynamoDBDocumentClient, GetCommand, UpdateCommand } = require('@aws-sdk/lib-dynamodb');
const { getItem, updateMetadata } = require('../app/lib/dynamo');
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

  describe('updateMetadata', () => {
    it('should execute UpdateCommand with correct parameters', async () => {
      docClientStub.resolves({});

      await updateMetadata('testTable', { iun_recipientId: 'iun1##rec1' }, { viewed: true });

      expect(docClientStub.firstCall.args[0]).to.be.an.instanceof(UpdateCommand);
      const cmd = docClientStub.firstCall.args[0];
      expect(cmd.input.TableName).to.equal('testTable');
      expect(cmd.input.Key).to.deep.equal({ iun_recipientId: 'iun1##rec1' });
      expect(cmd.input.UpdateExpression).to.equal('SET #viewed = :viewed');
      expect(cmd.input.ExpressionAttributeValues).to.deep.equal({ ':viewed': true });
      expect(cmd.input.ExpressionAttributeNames).to.deep.equal({ '#viewed': 'viewed' });
    });

    it('should handle multiple fields in UpdateExpression', async () => {
      docClientStub.resolves({});

      await updateMetadata('testTable', { iun_recipientId: 'iun1##rec1' }, { viewed: true, delivered: true });

      const cmd = docClientStub.firstCall.args[0];
      expect(cmd.input.UpdateExpression).to.include('SET');
      expect(cmd.input.UpdateExpression).to.include('#viewed = :viewed');
      expect(cmd.input.UpdateExpression).to.include('#delivered = :delivered');
    });

    it('should log and return early if no fields provided', async () => {
      await updateMetadata('testTable', { iun_recipientId: 'iun1##rec1' }, {});

      expect(docClientStub.called).to.be.false;
      expect(logStub.calledWith('[metadataUpdater] DynamoDB UpdateItem skipped: no fields to update, table=testTable, key={"iun_recipientId":"iun1##rec1"}')).to.be.true;
    });

    it('should skip null and undefined values in fieldsToUpdate', async () => {
      docClientStub.resolves({});

      await updateMetadata('testTable', { iun_recipientId: 'iun1##rec1' }, { viewed: null, delivered: undefined });

      expect(docClientStub.called).to.be.false;
    });

    it('should throw error if UpdateCommand fails', async () => {
      const error = new Error('DynamoDB error');
      docClientStub.rejects(error);

      try {
        await updateMetadata('testTable', { iun_recipientId: 'iun1##rec1' }, { viewed: true });
      } catch (err) {
        expect(err.message).to.equal('DynamoDB error');
      }
    });
  });
});
