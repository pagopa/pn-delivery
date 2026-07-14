const { expect } = require('chai');
const sinon = require('sinon');
const { DynamoDBDocumentClient, GetCommand, DeleteCommand, UpdateCommand } = require('@aws-sdk/lib-dynamodb');
const { getItem, deleteItem, updateNotificationMetadataRecord, buildNotificationMetadataUpdateParams } = require('../app/lib/dynamo');
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

  describe('updateNotificationMetadataRecord', () => {
    it('should execute UpdateCommand successfully', async () => {
      docClientStub.resolves({});

      const item = { iun_recipientId: 'iun1##rec1', notificationStatusTimestamp: '2025-01-01T00:00:00Z', notificationStatus: 'ACCEPTED' };
      await updateNotificationMetadataRecord('testTable', item);

      expect(docClientStub.firstCall.args[0]).to.be.an.instanceof(UpdateCommand);
    });

    it('should log and not throw on ConditionalCheckFailedException', async () => {
      const error = new Error();
      error.name = 'ConditionalCheckFailedException';
      docClientStub.rejects(error);

      const item = { iun_recipientId: 'iun1##rec1', notificationStatus: 'DELIVERING', notificationStatusTimestamp: '2025-01-01T00:00:00Z' };
      await updateNotificationMetadataRecord('testTable', item);

      expect(logStub.firstCall.args[0]).to.equal('update not necessary for item with pk: iun1##rec1 and status: DELIVERING on table: testTable');
    });

    it('should throw on other errors', async () => {
      const error = new Error('Test error');
      docClientStub.rejects(error);

      const item = { iun_recipientId: 'iun1##rec1', notificationStatus: 'ACCEPTED', notificationStatusTimestamp: '2025-01-01T00:00:00Z' };
      try {
        await updateNotificationMetadataRecord('testTable', item);
        expect.fail('should have thrown');
      } catch (err) {
        expect(err.message).to.equal('Test error');
      }
    });
  });

  describe('buildNotificationMetadataUpdateParams', () => {
    const fullItem = {
      iun_recipientId: 'iun1##rec1',
      notificationStatus: 'ACCEPTED',
      notificationStatusTimestamp: '2025-01-01T00:00:00Z',
      senderId: 'sender1',
      recipientId: 'rec1',
      sentAt: '2025-01-01T00:00:00Z',
      notificationGroup: 'group1',
      communicationType: 'INFORMAL',
      campaignId: 'campaign1',
      campaignId_creationMonth: 'campaign1##202501',
      campaignId_recipientId: 'campaign1##rec1',
      recipientIds: ['rec1', 'rec2'],
      tableRow: {
        iun: 'iun1',
        recipientsIds: '[rec1,rec2]',
        paProtocolNumber: 'proto1',
        subject: 'subject1',
        senderDenomination: 'sender name',
      },
      senderId_recipientId: 'sender1##rec1',
      senderId_creationMonth: 'sender1##202501',
      recipientId_creationMonth: 'rec1##202501',
      recipientOne: true,
    };

    it('should set Key to iun_recipientId and sentAt (table composite key)', () => {
      const params = buildNotificationMetadataUpdateParams('testTable', fullItem);
      expect(params.Key).to.deep.equal({ iun_recipientId: 'iun1##rec1', sentAt: '2025-01-01T00:00:00Z' });
    });

    it('should include all notification metadata fields in UpdateExpression', () => {
      const params = buildNotificationMetadataUpdateParams('testTable', fullItem);
      const expr = params.UpdateExpression;

      expect(expr).to.include('#notificationStatus = :notificationStatus');
      expect(expr).to.include('#notificationStatusTimestamp = :notificationStatusTimestamp');
      expect(expr).to.include('#senderId = :senderId');
      expect(expr).to.include('#recipientId = :recipientId');
      expect(expr).to.include('#notificationGroup = :notificationGroup');
      expect(expr).to.include('#communicationType = :communicationType');
      expect(expr).to.include('#campaignId = :campaignId');
      expect(expr).to.include('#campaignId_creationMonth = :campaignId_creationMonth');
      expect(expr).to.include('#campaignId_recipientId = :campaignId_recipientId');
      expect(expr).to.include('#recipientIds = :recipientIds');
      expect(expr).to.include('#tableRow = :tableRow');
      expect(expr).to.include('#senderId_recipientId = :senderId_recipientId');
      expect(expr).to.include('#senderId_creationMonth = :senderId_creationMonth');
      expect(expr).to.include('#recipientId_creationMonth = :recipientId_creationMonth');
      expect(expr).to.include('#recipientOne = :recipientOne');
      expect(expr).to.include('#viewed = if_not_exists(#viewed, :false)');
      expect(expr).to.include('#delivered = if_not_exists(#delivered, :false)');
      expect(expr).to.include('#desiredFeedback = if_not_exists(#desiredFeedback, :false)');
    });

    it('should map every field value into ExpressionAttributeValues', () => {
      const params = buildNotificationMetadataUpdateParams('testTable', fullItem);
      const vals = params.ExpressionAttributeValues;

      expect(vals[':notificationStatus']).to.equal('ACCEPTED');
      expect(vals[':notificationStatusTimestamp']).to.equal('2025-01-01T00:00:00Z');
      expect(vals[':senderId']).to.equal('sender1');
      expect(vals[':recipientId']).to.equal('rec1');
      expect(vals[':notificationGroup']).to.equal('group1');
      expect(vals[':communicationType']).to.equal('INFORMAL');
      expect(vals[':campaignId']).to.equal('campaign1');
      expect(vals[':campaignId_creationMonth']).to.equal('campaign1##202501');
      expect(vals[':campaignId_recipientId']).to.equal('campaign1##rec1');
      expect(vals[':recipientIds']).to.deep.equal(['rec1', 'rec2']);
      expect(vals[':tableRow']).to.deep.equal(fullItem.tableRow);
      expect(vals[':senderId_recipientId']).to.equal('sender1##rec1');
      expect(vals[':senderId_creationMonth']).to.equal('sender1##202501');
      expect(vals[':recipientId_creationMonth']).to.equal('rec1##202501');
      expect(vals[':recipientOne']).to.equal(true);
      expect(vals[':statusChangeTimestamp']).to.equal('2025-01-01T00:00:00Z');
      expect(vals[':false']).to.equal(false);
    });

    it('should preserve nested tableRow object as-is', () => {
      const params = buildNotificationMetadataUpdateParams('testTable', fullItem);
      expect(params.ExpressionAttributeValues[':tableRow']).to.deep.equal(fullItem.tableRow);
    });

    it('should not include iun_recipientId in UpdateExpression or ExpressionAttributeValues', () => {
      const params = buildNotificationMetadataUpdateParams('testTable', fullItem);
      expect(params.UpdateExpression).to.not.include('#iun_recipientId');
      expect(params.ExpressionAttributeValues).to.not.have.property(':iun_recipientId');
    });

    it('should not include sentAt in UpdateExpression or ExpressionAttributeValues (it is a key attribute)', () => {
      const params = buildNotificationMetadataUpdateParams('testTable', fullItem);
      expect(params.UpdateExpression).to.not.include('#sentAt');
      expect(params.ExpressionAttributeValues).to.not.have.property(':sentAt');
    });

    it('should set correct ConditionExpression for timestamp ordering', () => {
      const params = buildNotificationMetadataUpdateParams('testTable', fullItem);
      expect(params.ConditionExpression).to.equal(
        'attribute_not_exists(#notificationStatusTimestamp) OR #notificationStatusTimestamp < :statusChangeTimestamp'
      );
      expect(params.ExpressionAttributeValues[':statusChangeTimestamp']).to.equal(fullItem.notificationStatusTimestamp);
    });

    it('should set null for optional field campaignId when it is null', () => {
      const params = buildNotificationMetadataUpdateParams('testTable', { ...fullItem, campaignId: null });
      expect(params.ExpressionAttributeValues[':campaignId']).to.be.null;
    });

    it('should set null for optional field campaignId when it is undefined', () => {
      const params = buildNotificationMetadataUpdateParams('testTable', { ...fullItem, campaignId: undefined });
      expect(params.ExpressionAttributeValues[':campaignId']).to.be.null;
    });
  });
})
