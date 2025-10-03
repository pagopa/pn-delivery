const sinon = require('sinon');
const { expect } = require('chai');
const utils = require('../app/lib/utils');
const putNotificationRework = require('../app/lib/putNotificationRework');
const { processRecord } = require('../app/lib/processRecord');

describe('processRecord tests', () => {
  let record = {
    kinesis: {
      data: 'mockedEncodedData',
    },
  };
  let decodePayloadStub;
  let putNotificationReworkStub;

  const timelineElementMock_REWORK_CREATED = {
    iun: 'mockedIun',
    category: 'REWORK_CREATED',
    statusInfo: {
      actual: 'ACCEPTED',
      statusChangeTimestamp: '2025-01-01T00:00:00Z',
    },
    timelineElementId: 'mockedTimelineId'
  }

  const timelineElementMock_SEND_ANALOG_PROGRESS = {
    iun: 'mockedIun',
    category: 'SEND_ANALOG_PROGRESS',
    statusInfo: {
      actual: 'ACCEPTED',
      statusChangeTimestamp: '2025-01-01T00:00:00Z',
    },
    timelineElementId: 'mockedTimelineId'
  }

  const timelineElementMock_SEND_ANALOG_FEEDBACK = {
    iun: 'mockedIun',
    category: 'SEND_ANALOG_FEEDBACK',
    statusInfo: {
      actual: 'ACCEPTED',
      statusChangeTimestamp: '2025-01-01T00:00:00Z',
    },
    timelineElementId: 'mockedTimelineId'
  }

  beforeEach(() => {
    putNotificationReworkStub = sinon.stub(putNotificationRework, 'updateRework');
    warnLogStub = sinon.stub(console, 'warn');
  });

  afterEach(() => {
    sinon.restore();
  });

  it('should process REWORK_CREATED category', async () => {
    decodePayloadStub = sinon.stub(utils, 'decodePayload').returns({
      dynamodb: {
        NewImage: {
          iun: { S: 'mockedIun' },
          category: { S: 'REWORK_CREATED' },
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

    await processRecord(record);
    expect(decodePayloadStub.firstCall.args[0]).to.be.equal('mockedEncodedData');

    expect(putNotificationReworkStub.firstCall.args[0]).to.be.deep.equal(timelineElementMock_REWORK_CREATED);
  });

  it('should process SEND_ANALOG_PROGRESS category', async () => {
    decodePayloadStub = sinon.stub(utils, 'decodePayload').returns({
      dynamodb: {
        NewImage: {
          iun: { S: 'mockedIun' },
          category: { S: 'SEND_ANALOG_PROGRESS' },
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

    await processRecord(record);
    expect(decodePayloadStub.firstCall.args[0]).to.be.equal('mockedEncodedData');

    expect(putNotificationReworkStub.firstCall.args[0]).to.be.deep.equal(timelineElementMock_SEND_ANALOG_PROGRESS);
  });

  it('should process SEND_ANALOG_FEEDBACK category', async () => {
    decodePayloadStub = sinon.stub(utils, 'decodePayload').returns({
      dynamodb: {
        NewImage: {
          iun: { S: 'mockedIun' },
          category: { S: 'SEND_ANALOG_FEEDBACK' },
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

    await processRecord(record);
    expect(decodePayloadStub.firstCall.args[0]).to.be.equal('mockedEncodedData');

    expect(putNotificationReworkStub.firstCall.args[0]).to.be.deep.equal(timelineElementMock_SEND_ANALOG_FEEDBACK);
  });

  it('should process unrecognized category', async () => {
    decodePayloadStub = sinon.stub(utils, 'decodePayload').returns({
      dynamodb: {
        NewImage: {
          iun: { S: 'mockedIun' },
          category: { S: 'FAKE_CATEGORY' },
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

    const unexpectedError = new Error("Category FAKE_CATEGORY not managed");
    
    try {
      await processRecord(record);
    } catch (error) {
      expect(error.message).to.be.equal(unexpectedError.message);
    }
  });
});