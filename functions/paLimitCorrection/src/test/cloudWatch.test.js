const { expect } = require('chai');
const sinon = require('sinon');
const { CloudWatchClient, GetMetricStatisticsCommand } = require("@aws-sdk/client-cloudwatch");
const cloudWatch = require('../app/lib/cloudWatch');

describe('cloudWatch.js tests', () => {
    let getMetricDataStub;

    beforeEach(() => {
        getMetricDataStub = sinon.stub(CloudWatchClient.prototype, 'send');
    });

    afterEach(() => {
        sinon.restore();
    });

    it('returns maximum iterator age for a given lambda function', async () => {
        const lambdaName = 'myLambdaFunction';

        const expectedResponse = {
            Label: 'IteratorAge',
            Datapoints: [
                {
                    Maximum: 1000
                },
                {
                    Maximum: 1500
                },
                {
                    Maximum: 1200
                }
            ]
        };
        getMetricDataStub.resolves(expectedResponse);

        const result = await cloudWatch.getIteratorAgeMetrics(lambdaName);

        expect(getMetricDataStub.calledOnce).to.be.true;
        expect(result).to.deep.equal(expectedResponse.Datapoints);
    });

    it('handles errors when retrieving metric data', async () => {
        const lambdaName = 'myLambdaFunction';
        const expectedError = new Error('Failed to retrieve metric data');
        getMetricDataStub.rejects(expectedError);

        try {
            await cloudWatch.getIteratorAgeMetrics(lambdaName);
        } catch (error) {
            expect(getMetricDataStub.calledOnce).to.be.true;
            expect(error).to.equal(expectedError);
        }
    });
});