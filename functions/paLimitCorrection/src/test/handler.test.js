const { expect } = require('chai');
const sinon = require('sinon');
const proxyquire = require('proxyquire').noCallThru();

describe('handler.js tests', () => {
  let eventHandler;
  let processDateStub;

  beforeEach(() => {
    processDateStub = sinon.stub(); // Create a stub for processDate
    eventHandler = proxyquire('../app/handler', {
      '../app/lib/processDate': { processDate: processDateStub }, // Mock the dependency
    }).eventHandler;
  });

  afterEach(() => {
    sinon.restore();
  });

  it('should process date with provided dateToVerifyLimit', async () => {
    const event = {
      dateToVerifyLimit: '2025-02-11',
    };

    processDateStub.resolves();

    await eventHandler(event);

    expect(processDateStub.callCount).to.equal(1);
    expect(processDateStub.firstCall.args[0].toISOString()).to.equal(new Date(event.dateToVerifyLimit).toISOString());
  });

  it('when dateToVerifyLimit is less than four days ago the process return', async () => {
    const currentDate = new Date();
    currentDate.setDate(currentDate.getDate() - 2);
    const event = {
      dateToVerifyLimit: currentDate,
    };

    processDateStub.resolves();

    await eventHandler(event);

    expect(processDateStub.callCount).to.equal(0);
  });

  it('should set four days ago date when dateToVerifyLimit is not provided', async () => {
    const event = {};
    const now = new Date();
    const fourDaysAgo = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate() - 4, 0, 0, 0));

    processDateStub.resolves();

    await eventHandler(event);

    expect(processDateStub.callCount).to.equal(1);
    expect(processDateStub.firstCall.args[0].toISOString()).to.equal(fourDaysAgo.toISOString());
  });
});