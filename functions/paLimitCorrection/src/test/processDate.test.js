const { expect } = require('chai');
const sinon = require('sinon');
const dynamo = require('../app/lib/dynamo');
const slaLambda = require('../app/lib/slaLambda');
const { processDate } = require('../app/lib/processDate');

describe('processDate.js tests', () => {
  let getNotificationLimitStub, searchSLAViolationsStub, getNotificationMetadataStub, updateNotificationLimitStub;

  beforeEach(() => {
    getNotificationLimitStub = sinon.stub(dynamo, 'getNotificationLimit');
    searchSLAViolationsStub = sinon.stub(slaLambda, 'searchSLAViolations');
    getNotificationMetadataStub = sinon.stub(dynamo, 'getNotificationMetadata');
    updateNotificationLimitStub = sinon.stub(dynamo, 'updateNotificationLimit');
  });

  afterEach(() => {
    sinon.restore();
  });

  it('should process date and update notification limit when deltaDailyCounter is positive', async () => {
    const dateToVerifyLimit = new Date('2023-10-10');
    const yearMonth = '2023##10';
    const notificationLimit = [{ paId: 'pa1', dailyCounter10: 5 }];
    const results = [{}, {}];
    const notificationMetadata = [{}, {}];

    getNotificationLimitStub.resolves(notificationLimit);
    searchSLAViolationsStub.resolves(results);
    getNotificationMetadataStub.resolves(notificationMetadata);
    updateNotificationLimitStub.resolves();

    await processDate(dateToVerifyLimit);

    expect(getNotificationLimitStub.firstCall.args[0]).to.equal(yearMonth);
    expect(searchSLAViolationsStub.firstCall.args[0]).to.equal(dateToVerifyLimit);
    expect(getNotificationMetadataStub.firstCall.args[0]).to.equal('pa1##202310');
    expect(updateNotificationLimitStub.firstCall.args[0]).to.equal('pa1##2023##10');
    expect(updateNotificationLimitStub.firstCall.args[1]).to.equal('dailyCounter10');
    expect(updateNotificationLimitStub.firstCall.args[2]).to.equal(5);
    expect(updateNotificationLimitStub.firstCall.args[3]).to.equal(1);
  });

  it('should not update notification limit when deltaDailyCounter is zero or negative', async () => {
    const dateToVerifyLimit = new Date('2023-10-10');
    const yearMonth = '2023##10';
    const notificationLimit = [{ paId: 'pa1', dailyCounter10: 2 }];
    const results = [{}];
    const notificationMetadata = [{}];

    getNotificationLimitStub.resolves(notificationLimit);
    searchSLAViolationsStub.resolves(results);
    getNotificationMetadataStub.resolves(notificationMetadata);

    await processDate(dateToVerifyLimit);

    expect(getNotificationLimitStub.firstCall.args[0]).to.equal(yearMonth);
    expect(searchSLAViolationsStub.firstCall.args[0]).to.equal(dateToVerifyLimit);
    expect(getNotificationMetadataStub.firstCall.args[1]).to.equal('2023-10-10');
    expect(updateNotificationLimitStub.callCount).to.equal(0);
  });

  it('should handle case when notification limit is not found', async () => {
    const dateToVerifyLimit = new Date('2023-10-10');
    const yearMonth = '2023##10';

    getNotificationLimitStub.resolves([]);

    await processDate(dateToVerifyLimit);

    expect(getNotificationLimitStub.firstCall.args[0]).to.equal(yearMonth);
    expect(searchSLAViolationsStub.callCount).to.equal(0);
    expect(getNotificationMetadataStub.callCount).to.equal(0);
    expect(updateNotificationLimitStub.callCount).to.equal(0);
  });

});