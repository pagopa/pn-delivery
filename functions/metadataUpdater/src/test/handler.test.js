const { expect } = require('chai');
const sinon = require('sinon');
const proxyquire = require('proxyquire').noCallThru();

describe('eventHandler tests', () => {
  let eventHandler;
  let processRecordStub;

  beforeEach(() => {
    processRecordStub = sinon.stub();
    eventHandler = proxyquire('../app/handler', {
      './lib/processRecord': { processRecord: processRecordStub },
    }).eventHandler;
    sinon.stub(console, 'log');
    sinon.stub(console, 'error');
  });

  afterEach(() => {
    sinon.restore();
  });

  it('should process all records successfully', async () => {
    processRecordStub.resolves();

    const event = {
      Records: [
        { kinesis: { sequenceNumber: '1' } },
        { kinesis: { sequenceNumber: '2' } },
      ],
    };
    const result = await eventHandler(event);

    expect(processRecordStub.callCount).to.equal(2);
    expect(result).to.deep.equal({ batchItemFailures: [] });
  });

  it('should handle errors and return batchItemFailures', async () => {
    processRecordStub.callsFake(async (record) => {
      if (record.kinesis.sequenceNumber === '1') {
        throw new Error('Error processing record 1');
      }
    });

    const event = {
      Records: [
        { kinesis: { sequenceNumber: '1' } },
        { kinesis: { sequenceNumber: '2' } },
      ],
    };
    const result = await eventHandler(event);

    expect(processRecordStub.callCount).to.equal(2);
    expect(result).to.deep.equal({ batchItemFailures: [{ itemIdentifier: '1' }] });
  });

  it('should handle multiple errors and return batchItemFailures', async () => {
    processRecordStub.callsFake(async (record) => {
      throw new Error(`Error processing record ${record.kinesis.sequenceNumber}`);
    });

    const event = {
      Records: [
        { kinesis: { sequenceNumber: '1' } },
        { kinesis: { sequenceNumber: '2' } },
      ],
    };
    const result = await eventHandler(event);

    expect(result.batchItemFailures).to.have.length(2);
    expect(result.batchItemFailures).to.deep.equal([
      { itemIdentifier: '1' },
      { itemIdentifier: '2' },
    ]);
  });

  it('should return empty batchItemFailures if no records', async () => {
    const event = { Records: [] };
    const result = await eventHandler(event);
    expect(result.batchItemFailures).to.deep.equal([]);
  });
});

