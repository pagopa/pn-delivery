const { expect } = require('chai');
const sinon = require('sinon');
const proxyquire = require('proxyquire').noCallThru();

describe('eventHandler tests', () => {
  let eventHandler;
  let processRecordStub;

  beforeEach(() => {
    processRecordStub = sinon.stub(); // Creiamo uno stub per processRecord
    eventHandler = proxyquire('../app/handler', {
      '../app/lib/processRecord': { processRecord: processRecordStub }, // Mock della dipendenza
    }).eventHandler;
  });

  afterEach(() => {
    sinon.restore();
  });

  it('should process all records successfully', async () => {
    const event = {
      Records: [
        { kinesis: { sequenceNumber: '1' } },
        { kinesis: { sequenceNumber: '2' } },
      ],
    };

    processRecordStub.resolves();

    const res = await eventHandler(event);

    expect(processRecordStub.callCount).to.equal(2);
    expect(processRecordStub.firstCall.args[0]).to.deep.equal(event.Records[0]);
    expect(processRecordStub.secondCall.args[0]).to.deep.equal(event.Records[1]);
    expect(res).to.deep.equal({ batchItemFailures: [] });
  });

  it('should handle errors and return batchItemFailures', async () => {
    const event = {
      Records: [
        { kinesis: { sequenceNumber: '1' } },
        { kinesis: { sequenceNumber: '2' } },
      ],
    };

    processRecordStub.callsFake(async (record) => {
      if (record.kinesis.sequenceNumber === '1') {
        throw new Error('Error processing record 1');
      }
    });

    const res = await eventHandler(event);

    expect(processRecordStub.callCount).to.equal(2);
    expect(processRecordStub.firstCall.args[0]).to.deep.equal(event.Records[0]);
    expect(processRecordStub.secondCall.args[0]).to.deep.equal(event.Records[1]);
    expect(res).to.deep.equal({ batchItemFailures: [{ itemIdentifier: '1' }] });
  });

  it('should handle multiple errors and return batchItemFailures', async () => {
    const event = {
      Records: [
        { kinesis: { sequenceNumber: '1' } },
        { kinesis: { sequenceNumber: '2' } },
      ],
    };

    processRecordStub.callsFake(async (record) => {
      if (record.kinesis.sequenceNumber === '1') {
        throw new Error('Error processing record 1');
      }
      if (record.kinesis.sequenceNumber === '2') {
        throw new Error('Error processing record 2');
      }
    });

    const res = await eventHandler(event);

    expect(processRecordStub.callCount).to.equal(2);
    expect(processRecordStub.firstCall.args[0]).to.deep.equal(event.Records[0]);
    expect(processRecordStub.secondCall.args[0]).to.deep.equal(event.Records[1]);
    expect(res).to.deep.equal({ batchItemFailures: [{ itemIdentifier: '1' }, { itemIdentifier: '2' }] });
  });

  it('should return empty batchItemFailures if no records', async () => {
    const event = {
      Records: [],
    };

    const res = await eventHandler(event);

    expect(processRecordStub.callCount).to.equal(0);
    expect(res).to.deep.equal({ batchItemFailures: [] });
  });
});