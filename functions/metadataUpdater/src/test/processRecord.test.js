const sinon = require('sinon');
const { expect } = require('chai');
const utils = require('../app/lib/utils');
const dynamo = require('../app/lib/dynamo');
const { processRecord } = require('../app/lib/processRecord');

describe('processRecord tests', () => {
  const record = {
    kinesis: { data: 'mockedEncodedData' },
  };

  const notificationMock = {
    iun: 'mockedIun',
    recipients: [
      { recipientId: 'recipientId1' },
      { recipientId: 'recipientId2' },
    ],
  };

  let decodePayloadStub;
  let getItemStub;
  let updateMetadataStub;

  const makeKinesisPayload = (category, details) => ({
    dynamodb: {
      NewImage: {
        iun: { S: 'mockedIun' },
        category: { S: category },
        ...(details ? { details: { M: Object.fromEntries(Object.entries(details).map(([k, v]) => [k, { S: v }])) } } : {}),
      },
    },
  });

  beforeEach(() => {
    decodePayloadStub = sinon.stub(utils, 'decodePayload');
    getItemStub = sinon.stub(dynamo, 'getItem').resolves(notificationMock);
    updateMetadataStub = sinon.stub(dynamo, 'updateMetadata').resolves();
    sinon.stub(console, 'log');
  });

  afterEach(() => {
    sinon.restore();
  });

  it('should set viewed=true for NOTIFICATION_VIEWED', async () => {
    decodePayloadStub.returns(makeKinesisPayload('NOTIFICATION_VIEWED'));

    await processRecord(record);

    expect(updateMetadataStub.callCount).to.equal(2);
    expect(updateMetadataStub.firstCall.args[2]).to.deep.equal({ viewed: true });
  });

  it('should set viewed=true for NOTIFICATION_VIEWED_INFORMAL', async () => {
    decodePayloadStub.returns(makeKinesisPayload('NOTIFICATION_VIEWED_INFORMAL'));

    await processRecord(record);

    expect(getItemStub.firstCall.args).to.deep.equal(['pn-Notifications', { iun: 'mockedIun' }]);
    expect(updateMetadataStub.callCount).to.equal(2);
    expect(updateMetadataStub.firstCall.args).to.deep.equal([
      'pn-NotificationsMetadata',
      { iun_recipientId: 'mockedIun##recipientId1' },
      { viewed: true },
    ]);
    expect(updateMetadataStub.secondCall.args).to.deep.equal([
      'pn-NotificationsMetadata',
      { iun_recipientId: 'mockedIun##recipientId2' },
      { viewed: true },
    ]);
  });

  it('should set delivered=true for SEND_DIGITAL_FEEDBACK with responseStatus OK', async () => {
    decodePayloadStub.returns(makeKinesisPayload('SEND_DIGITAL_FEEDBACK', { responseStatus: 'OK' }));

    await processRecord(record);

    expect(updateMetadataStub.callCount).to.equal(2);
    expect(updateMetadataStub.firstCall.args[2]).to.deep.equal({ delivered: true });
  });

  it('should set delivered=true for SEND_ANALOG_FEEDBACK with responseStatus OK', async () => {
    decodePayloadStub.returns(makeKinesisPayload('SEND_ANALOG_FEEDBACK', { responseStatus: 'OK' }));

    await processRecord(record);

    expect(updateMetadataStub.callCount).to.equal(2);
    expect(updateMetadataStub.firstCall.args[2]).to.deep.equal({ delivered: true });
  });

  it('should skip update for SEND_DIGITAL_FEEDBACK with responseStatus KO', async () => {
    decodePayloadStub.returns(makeKinesisPayload('SEND_DIGITAL_FEEDBACK', { responseStatus: 'KO' }));

    await processRecord(record);

    expect(getItemStub.called).to.be.false;
    expect(updateMetadataStub.called).to.be.false;
  });

  it('should skip update for SEND_ANALOG_FEEDBACK without details', async () => {
    decodePayloadStub.returns(makeKinesisPayload('SEND_ANALOG_FEEDBACK'));

    await processRecord(record);

    expect(getItemStub.called).to.be.false;
    expect(updateMetadataStub.called).to.be.false;
  });

  it('should set desiredFeedback=true for WORKFLOW_DONE', async () => {
    decodePayloadStub.returns(makeKinesisPayload('WORKFLOW_DONE'));

    await processRecord(record);

    expect(updateMetadataStub.callCount).to.equal(2);
    expect(updateMetadataStub.firstCall.args[2]).to.deep.equal({ desiredFeedback: true });
  });

  it('should skip unknown category', async () => {
    decodePayloadStub.returns(makeKinesisPayload('UNKNOWN_CATEGORY'));

    await processRecord(record);

    expect(getItemStub.called).to.be.false;
    expect(updateMetadataStub.called).to.be.false;
  });

  it('should throw error when getItem fails', async () => {
    decodePayloadStub.returns(makeKinesisPayload('WORKFLOW_DONE'));
    getItemStub.rejects(new Error('DynamoDB error'));

    try {
      await processRecord(record);
    } catch (err) {
      expect(err.message).to.equal('DynamoDB error');
    }
  });

  it('should throw error when updateMetadata fails', async () => {
    decodePayloadStub.returns(makeKinesisPayload('NOTIFICATION_VIEWED_INFORMAL'));
    updateMetadataStub.rejects(new Error('Update error'));

    try {
      await processRecord(record);
    } catch (err) {
      expect(err.message).to.equal('Update error');
    }
  });
});
