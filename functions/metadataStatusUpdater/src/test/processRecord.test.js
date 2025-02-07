const sinon = require('sinon');
const { expect } = require('chai');
const utils = require('../app/lib/utils');
const dynamo = require('../app/lib/dynamo');
const putNotificationMetadata = require('../app/lib/putNotificationMetadata');
const { processRecord } = require('../app/lib/processRecord');

describe('processRecord tests', () => {
  let record = {
    kinesis: {
      data: 'mockedEncodedData',
    },
  };
  let decodePayloadStub;
  let getItemStub;
  let deleteItemStub;
  let putNotificationMetadataStub;

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
    sentAt: '2025-01-01T00:00:00Z',
  }

  const notificationMockNoPayments = {
    iun: 'mockedIun',
    recipients: [
      {
        payments: [{f24: {}}],
        recipientId: 'mockedRecipientId',
      },
    ],
    sentAt: '2025-01-01T00:00:00Z',
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
    getItemStub = sinon.stub(dynamo, 'getItem').resolves(notificationMock);
    deleteItemStub  = sinon.stub(dynamo, 'deleteItem').resolves();
    putNotificationMetadataStub = sinon.stub(putNotificationMetadata, 'putNotificationMetadata');
    warnLogStub = sinon.stub(console, 'warn');
  });

  afterEach(() => {
    sinon.restore();
  });

  it('should process an ACCEPTED record', async () => {
    await processRecord(record);
    expect(decodePayloadStub.firstCall.args[0]).to.be.equal('mockedEncodedData');
    expect(getItemStub.firstCall.args[0]).to.be.equal('pn-Notifications');
    expect(getItemStub.firstCall.args[1]).to.deep.equal({ iun: 'mockedIun' });

    expect(putNotificationMetadataStub.firstCall.args[0]).to.be.deep.equal(timelineElementMock.statusInfo);
    expect(putNotificationMetadataStub.firstCall.args[1]).to.be.deep.equal(notificationMock);
  });

  it('should process a REFUSED record and delete payments when present', async () => {
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

  it('should process a REFUSED record and do nothing if notification has not payments', async () => {
    decodePayloadStub.returns({
      dynamodb: {
        NewImage: {
          iun: { S: 'mockedIun' },
          statusInfo: { M: { actual: { S: 'REFUSED' } } },
          timelineElementId: { S: 'mockedTimelineId' }
        },
      },
    });

    getItemStub.resolves(notificationMockNoPayments);

    await processRecord(record);
    expect(deleteItemStub.notCalled).to.be.true;
    expect(putNotificationMetadataStub.notCalled).to.be.true;
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