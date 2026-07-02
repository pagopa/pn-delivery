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
    communicationType: 'INFORMAL',
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
        ...(details ? {
          details: {
            M: Object.fromEntries(
              Object.entries(details).map(([k, v]) => [
                k,
                typeof v === 'number' ? { N: v.toString() } : { S: v },
              ])
            ),
          },
        } : {}),
      },
    },
  });

  beforeEach(() => {
    decodePayloadStub = sinon.stub(utils, 'decodePayload');
    getItemStub = sinon.stub(dynamo, 'getItem').resolves(notificationMock);
    updateMetadataStub = sinon.stub(dynamo, 'updateMetadata').resolves();
    sinon.stub(console, 'log');
    sinon.stub(console, 'error');
  });

  afterEach(() => {
    sinon.restore();
  });

  // INFORMAL notification tests
  it('should set viewed=true for INFORMAL_NOTIFICATION_VIEWED on the correct recipient', async () => {
    decodePayloadStub.returns(makeKinesisPayload('INFORMAL_NOTIFICATION_VIEWED', { recIndex: 0 }));

    await processRecord(record);

    expect(getItemStub.firstCall.args).to.deep.equal(['pn-Notifications', { iun: 'mockedIun' }]);
    expect(updateMetadataStub.callCount).to.equal(1);
    expect(updateMetadataStub.firstCall.args).to.deep.equal([
      'pn-NotificationsMetadata',
      { iun_recipientId: 'mockedIun##recipientId1' },
      { viewed: true },
    ]);
  });

  it('should use recIndex=1 to target the second recipient for INFORMAL_NOTIFICATION_VIEWED', async () => {
    decodePayloadStub.returns(makeKinesisPayload('INFORMAL_NOTIFICATION_VIEWED', { recIndex: 1 }));

    await processRecord(record);

    expect(updateMetadataStub.callCount).to.equal(1);
    expect(updateMetadataStub.firstCall.args[1]).to.deep.equal({ iun_recipientId: 'mockedIun##recipientId2' });
  });

  it('should set delivered=true for REACHED on the correct recipient', async () => {
    decodePayloadStub.returns(makeKinesisPayload('REACHED', { recIndex: 1 }));

    await processRecord(record);

    expect(updateMetadataStub.callCount).to.equal(1);
    expect(updateMetadataStub.firstCall.args).to.deep.equal([
      'pn-NotificationsMetadata',
      { iun_recipientId: 'mockedIun##recipientId2' },
      { delivered: true },
    ]);
  });

  it('should set desiredFeedback=true for WORKFLOW_DONE on the correct recipient', async () => {
    decodePayloadStub.returns(makeKinesisPayload('WORKFLOW_DONE', { recIndex: 1 }));

    await processRecord(record);

    expect(updateMetadataStub.callCount).to.equal(1);
    expect(updateMetadataStub.firstCall.args[1]).to.deep.equal({ iun_recipientId: 'mockedIun##recipientId2' });
    expect(updateMetadataStub.firstCall.args[2]).to.deep.equal({ desiredFeedback: true });
  });

  // Common tests
  it('should skip unknown category', async () => {
    decodePayloadStub.returns(makeKinesisPayload('UNKNOWN_CATEGORY'));

    await processRecord(record);

    expect(getItemStub.called).to.be.true;
    expect(updateMetadataStub.called).to.be.false;
  });

  it('should throw error when recIndex is missing from details', async () => {
    decodePayloadStub.returns(makeKinesisPayload('INFORMAL_NOTIFICATION_VIEWED', {}));

    try {
      await processRecord(record);
      expect.fail('should have thrown');
    } catch (err) {
      expect(err.message).to.include('Missing recIndex');
      expect(getItemStub.called).to.be.true;
    }
  });

  it('should throw error when recIndex is out of bounds', async () => {
    decodePayloadStub.returns(makeKinesisPayload('INFORMAL_NOTIFICATION_VIEWED', { recIndex: 99 }));

    try {
      await processRecord(record);
      expect.fail('should have thrown');
    } catch (err) {
      expect(err.message).to.include('Recipient at index 99 not found');
    }
  });

  it('should throw error when getItem fails', async () => {
    decodePayloadStub.returns(makeKinesisPayload('INFORMAL_NOTIFICATION_VIEWED', { recIndex: 0 }));
    getItemStub.rejects(new Error('DynamoDB error'));

    try {
      await processRecord(record);
      expect.fail('should have thrown');
    } catch (err) {
      expect(err.message).to.equal('DynamoDB error');
    }
  });

  it('should throw error when updateMetadata fails', async () => {
    decodePayloadStub.returns(makeKinesisPayload('INFORMAL_NOTIFICATION_VIEWED', { recIndex: 0 }));
    updateMetadataStub.rejects(new Error('Update error'));

    try {
      await processRecord(record);
      expect.fail('should have thrown');
    } catch (err) {
      expect(err.message).to.equal('Update error');
    }
  });
});
