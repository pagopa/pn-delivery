const sinon = require('sinon');
const { expect } = require('chai');
const utils = require('../app/lib/utils');
const dynamo = require('../app/lib/dynamo');
const putNotificationMetadata = require('../app/lib/putNotificationMetadata');
const { processRecord } = require('../app/lib/processRecord');
const { ItemNotFoundException } = require('../app/lib/exceptions');

describe('processRecord tests', () => {
  let record = {
    kinesis: {
      data: 'mockedEncodedData',
    },
  };
  let shouldSkipEvaluationStub;
  let decodePayloadStub;
  let getItemStub;
  let deleteItemStub;
  let putNotificationMetadataStub;
  let warnLogStub;

  const timelineElementMock = {
    iun: 'mockedIun',
    statusInfo: {
      actual: 'ACCEPTED',
      statusChangeTimestamp: '2025-01-01T00:00:00Z',
    },
    timelineElementId: 'mockedTimelineId'
  }

  const notificationMock = {
    iun: 'mockedIun',
    recipients: [
      {
        payments: [
          { creditorTaxId: 'mockedTaxId', noticeCode: 'mockedNoticeCode' },
        ],
        recipientId: 'mockedRecipientId',
      },
    ],
  }

  beforeEach(() => {
    decodePayloadStub = sinon.stub(utils, 'decodePayload').returns({
      dynamodb: {
        NewImage: {
          iun: { S: 'mockedIun' },
          statusInfo: {
            M: {
              actual: { S: 'ACCEPTED' },
              statusChangeTimestamp: { S: '2025-01-01T00:00:00Z' }
            }
          },
          timelineElementId: { S: 'mockedTimelineId' }
        },
      },
    });
    shouldSkipEvaluationStub = sinon.stub(utils, 'shouldSkipEvaluation');
    getItemStub = sinon.stub(dynamo, 'getItem').resolves(notificationMock);
    deleteItemStub  = sinon.stub(dynamo, 'deleteItem').resolves();
    putNotificationMetadataStub = sinon.stub(putNotificationMetadata, 'putNotificationMetadata');
    warnLogStub = sinon.stub(console, 'warn');
  });

  afterEach(() => {
    sinon.restore();
  });

  it('should skip processing if shouldSkipEvaluation returns true', async () => {
    shouldSkipEvaluationStub.returns(true);

    await processRecord(record);
    expect(decodePayloadStub.firstCall.args[0]).to.be.equal('mockedEncodedData');
    expect(getItemStub.notCalled).to.be.true;
    expect(putNotificationMetadataStub.notCalled).to.be.true;
  });

  it('should process an ACCEPTED record', async () => {
    await processRecord(record);
    expect(decodePayloadStub.firstCall.args[0]).to.be.equal('mockedEncodedData');
    expect(getItemStub.firstCall.args[0]).to.be.equal('pn-Notifications');
    expect(getItemStub.firstCall.args[1]).to.deep.equal({ iun: 'mockedIun' });

    expect(putNotificationMetadataStub.firstCall.args[0]).to.be.deep.equal(timelineElementMock.statusInfo);
    expect(putNotificationMetadataStub.firstCall.args[1]).to.be.deep.equal(notificationMock);
    expect(putNotificationMetadataStub.firstCall.args[2]).to.be.equal('2025-01-01T00:00:00Z');
  });

  it('should process a REFUSED record and delete payments', async () => {
    decodePayloadStub.returns({
      dynamodb: {
        NewImage: {
          iun: { S: 'mockedIun' },
          statusInfo: { M: { actual: { S: 'REFUSED' } } },
          timelineElementId: { S: 'mockedTimelineId' }
        },
      },
    });

    await processRecord(record);
    expect(deleteItemStub.firstCall.args[0]).to.be.equal('pn-NotificationsCost');
    expect(deleteItemStub.firstCall.args[1]).to.be.deep.equal({creditorTaxId_noticeCode: 'mockedTaxId##mockedNoticeCode'});
  });

  it('should handle missing metadata for non-ACCEPTED statuses', async () => {
    decodePayloadStub.returns({
      dynamodb: {
        NewImage: {
          iun: { S: 'mockedIun' },
          statusInfo: { M: { actual: { S: 'DELIVERED' } } },
          timelineElementId: { S: 'mockedTimelineId' }
        },
      },
    });

    const itemNotFoundException = new ItemNotFoundException("mockedIun", "pn-NotificationsMetadata");
    getItemStub.callsFake((param1, param2) => {
      if (param1 === 'pn-Notifications') {
        return Promise.resolve(notificationMock);
      }
      if (param1 === 'pn-NotificationsMetadata') {
        return Promise.reject(itemNotFoundException);
      }
      return Promise.resolve(null);
    });

    try {
      await processRecord(record);
    } catch (error) {
      expect(error).to.be.equal(itemNotFoundException);
    }

    expect(warnLogStub.firstCall.args[0]).to.be.equal('Unable to retrieve accepted date - iun=mockedIun recipientId=mockedRecipientId');
    expect(putNotificationMetadataStub.notCalled).to.be.true;
  });

  it('should process metadata for non-ACCEPTED statuses', async () => {
    decodePayloadStub.returns({
      dynamodb: {
        NewImage: {
          iun: { S: 'mockedIun' },
          statusInfo: { M: { actual: { S: 'DELIVERED' } } },
          timelineElementId: { S: 'mockedTimelineId' }
        },
      },
    });

    getItemStub.callsFake((param1, param2) => {
      if (param1 === 'pn-Notifications') {
        return Promise.resolve(notificationMock);
      }
      if (param1 === 'pn-NotificationsMetadata') {
        return Promise.resolve({ tableRow: { acceptedAt: '2025-01-02T00:00:00Z' } });
      }
      return Promise.resolve(null);
    });

    await processRecord(record);
    expect(putNotificationMetadataStub.firstCall.args[0]).to.be.deep.equal({ actual: 'DELIVERED' });
    expect(putNotificationMetadataStub.firstCall.args[1]).to.be.deep.equal(notificationMock);
    expect(putNotificationMetadataStub.firstCall.args[2]).to.be.equal('2025-01-02T00:00:00Z');
  });

  it('should throw an error for unexpected exceptions', async () => {
    const unexpectedError = new Error('Unexpected error');
    getItemStub.rejects(unexpectedError);

    try {
      await processRecord(record);
    } catch (error) {
      expect(error).to.be.equal(unexpectedError);
    }
  });
});