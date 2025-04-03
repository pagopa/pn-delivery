const { expect } = require('chai');
const sinon = require('sinon');
const dynamo = require('../app/lib/dynamo');
const slaLambda = require('../app/lib/slaLambda');
const cloudWatch = require('../app/lib/cloudWatch');
const { processDate } = require('../app/lib/processDate');

describe('processDate.js tests', () => {
  let getNotificationLimitStub, searchSLAViolationsStub, getNotificationMetadataStub, updateNotificationLimitStub, getIteratorAgeMetricsStub;

  beforeEach(() => {
    getNotificationLimitStub = sinon.stub(dynamo, 'getNotificationLimit');
    searchSLAViolationsStub = sinon.stub(slaLambda, 'searchSLAViolations');
    getNotificationMetadataStub = sinon.stub(dynamo, 'getNotificationMetadata');
    updateNotificationLimitStub = sinon.stub(dynamo, 'updateNotificationLimit');
    getIteratorAgeMetricsStub = sinon.stub(cloudWatch, 'getIteratorAgeMetrics');
  });

  afterEach(() => {
    sinon.restore();
  });

  it('should process date and update notification limit when deltaDailyCounter is positive', async () => {
    process.env.DEBUG_MODE = 'false'
    process.env.MAXIMUM_ITERATOR_AGE = 43200000;
    const dateToVerifyLimit = new Date('2023-10-10');
    const yearMonth = '2023##10';
    const notificationLimit = [{ paId: 'pa1', dailyCounter10: 5 }];
    const results = [{startTimestamp: '2023-10-10T18:45:15.000Z'}];
    const notificationMetadata = [{}, {}];
    const datapoints = [{Maximum: 1000}, {Maximum: 1500}, {Maximum: 1200}];

    getNotificationLimitStub.resolves(notificationLimit);
    searchSLAViolationsStub.resolves(results);
    getNotificationMetadataStub.resolves(notificationMetadata);
    updateNotificationLimitStub.resolves();
    getIteratorAgeMetricsStub.resolves(datapoints);

    await processDate(dateToVerifyLimit);

    expect(getNotificationLimitStub.firstCall.args[0]).to.equal(yearMonth);
    expect(getNotificationMetadataStub.firstCall.args[0]).to.equal('pa1##202310');
    expect(updateNotificationLimitStub.firstCall.args[0]).to.equal('pa1##2023##10');
    expect(updateNotificationLimitStub.firstCall.args[1]).to.equal('dailyCounter10');
    expect(updateNotificationLimitStub.firstCall.args[2]).to.equal(5);
    expect(updateNotificationLimitStub.firstCall.args[3]).to.equal(2);
    expect(getIteratorAgeMetricsStub.callCount).to.equal(2);
  });

  it('should not update notification limit when deltaDailyCounter is zero or negative', async () => {
    process.env.MAXIMUM_ITERATOR_AGE = 43200000;
    const dateToVerifyLimit = new Date('2023-10-10');
    const yearMonth = '2023##10';
    const notificationLimit = [{ paId: 'pa1', dailyCounter10: 2 }];
    const results = [{startTimestamp: '2023-10-10T18:45:15.000Z'}];
    const notificationMetadata = [{}];
    const datapoints = [{Maximum: 1000}, {Maximum: 1500}, {Maximum: 1200}];

    getNotificationLimitStub.resolves(notificationLimit);
    searchSLAViolationsStub.resolves(results);
    getNotificationMetadataStub.resolves(notificationMetadata);
    getIteratorAgeMetricsStub.resolves(datapoints);

    await processDate(dateToVerifyLimit);

    expect(getNotificationLimitStub.firstCall.args[0]).to.equal(yearMonth);
    expect(getNotificationMetadataStub.firstCall.args[1]).to.equal('2023-10-10');
    expect(updateNotificationLimitStub.callCount).to.equal(0);
    expect(getIteratorAgeMetricsStub.callCount).to.equal(2);
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
    expect(getIteratorAgeMetricsStub.callCount).to.equal(0);
  });

  it('should not update notification limit when DEBUG_MODE is true', async () => {
    process.env.DEBUG_MODE = 'true'
    process.env.MAXIMUM_ITERATOR_AGE = 43200000;
    const dateToVerifyLimit = new Date('2023-10-10');
    const yearMonth = '2023##10';
    const notificationLimit = [{ paId: 'pa1', dailyCounter10: 5 }];
    const results = [{startTimestamp: '2023-10-10T18:45:15.000Z'}, {startTimestamp: '2025-12-05T10:30:00.000Z'}];
    const notificationMetadata = [{}, {}];
    const datapoints = [{Maximum: 1000}, {Maximum: 1500}, {Maximum: 1200}];

    getNotificationLimitStub.resolves(notificationLimit);
    searchSLAViolationsStub.resolves(results);
    getNotificationMetadataStub.resolves(notificationMetadata);
    getIteratorAgeMetricsStub.resolves(datapoints);

    await processDate(dateToVerifyLimit);

    expect(getNotificationLimitStub.firstCall.args[0]).to.equal(yearMonth);
    expect(getNotificationMetadataStub.firstCall.args[0]).to.equal('pa1##202310');
    expect(updateNotificationLimitStub.callCount).to.equal(0);
    expect(getIteratorAgeMetricsStub.callCount).to.equal(2);
  });

  it('should not update notification limit when iteratorAge exceeds 12 hours', async () => {
    process.env.MAXIMUM_ITERATOR_AGE = 43200000;
    const dateToVerifyLimit = new Date('2023-10-10');
    const yearMonth = '2023##10';
    const notificationLimit = [{ paId: 'pa1', dailyCounter10: 5 }];
    const datapoints = [{Maximum: 1000}, {Maximum: 1500}, {Maximum: 21700000}];

    getNotificationLimitStub.resolves(notificationLimit);
    getIteratorAgeMetricsStub.resolves(datapoints);

    try {
      await processDate(dateToVerifyLimit);
    } catch (error) {
      expect(getNotificationLimitStub.firstCall.args[0]).to.equal(yearMonth);
      expect(getNotificationMetadataStub.callCount).to.equal(0);
      expect(updateNotificationLimitStub.callCount).to.equal(0);
      expect(getIteratorAgeMetricsStub.callCount).to.equal(2);
      expect(error.message).to.equal('Total maxIteratorAge exceeds MAXIMUM_ITERATOR_AGE defined.');
    }
  });

  it('should update notification limit when no datapoints are returned', async () => {
    process.env.DEBUG_MODE = 'false'
    process.env.MAXIMUM_ITERATOR_AGE = 43200000;
    const dateToVerifyLimit = new Date('2023-10-10');
    const yearMonth = '2023##10';
    const notificationLimit = [{ paId: 'pa1', dailyCounter10: 5 }];
    const results = [{startTimestamp: '2023-10-10T18:45:15.000Z'}];
    const notificationMetadata = [{}, {}];
    const datapoints = [];

    getNotificationLimitStub.resolves(notificationLimit);
    searchSLAViolationsStub.resolves(results);
    getNotificationMetadataStub.resolves(notificationMetadata);
    updateNotificationLimitStub.resolves();
    getIteratorAgeMetricsStub.resolves(datapoints);

    await processDate(dateToVerifyLimit);

    expect(getNotificationLimitStub.firstCall.args[0]).to.equal(yearMonth);
    expect(getNotificationMetadataStub.firstCall.args[0]).to.equal('pa1##202310');
    expect(updateNotificationLimitStub.firstCall.args[0]).to.equal('pa1##2023##10');
    expect(updateNotificationLimitStub.firstCall.args[1]).to.equal('dailyCounter10');
    expect(updateNotificationLimitStub.firstCall.args[2]).to.equal(5);
    expect(updateNotificationLimitStub.firstCall.args[3]).to.equal(2);
    expect(getIteratorAgeMetricsStub.callCount).to.equal(2);
  });

});