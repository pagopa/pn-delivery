const { expect } = require('chai');
const { LambdaClient, InvokeCommand } = require('@aws-sdk/client-lambda');
const sinon = require('sinon');
const { Buffer } = require('node:buffer');
const { searchSLAViolations } = require('../app/lib/slaLambda');

describe('slaLambda.js tests', () => {
    let sendStub;

    beforeEach(() => {
        sendStub = sinon.stub(LambdaClient.prototype, 'send');
    });

    afterEach(() => {
        sinon.restore();
    });

    it('returns results when there are no SLA violations', async () => {
        const mockResponse = {
            Payload: Buffer.from(JSON.stringify({ results: [], lastScannedKey: null }))
        };
        sendStub.resolves(mockResponse);

        const dateToVerifyLimit = new Date('2023-10-10T00:00:00.000Z')
        const results = await searchSLAViolations(dateToVerifyLimit);
        expect(results).to.deep.equal([]);
    });

    it('returns results when there are SLA violations', async () => {
        const mockResponse = {
            Payload: Buffer.from(JSON.stringify({ results: [{ id: 1, startTimestamp: '2023-10-10T18:45:15.000Z' }, { id: 2, startTimestamp: '2025-11-12T19:45:15.000Z' }], lastScannedKey: null }))
        };
        sendStub.resolves(mockResponse);

        const dateToVerifyLimit = new Date('2023-10-10T00:00:00.000Z')
        const results = await searchSLAViolations(dateToVerifyLimit);
        expect(results).to.deep.equal([{ id: 1, startTimestamp: '2023-10-10T18:45:15.000Z' }]);
    });

    it('handles multiple pages of results', async () => {
        const mockResponse1 = {
            Payload: Buffer.from(JSON.stringify({ results: [{ id: 1, startTimestamp: '2023-10-10T18:45:15.000Z' }], lastScannedKey: 'key1' }))
        };
        const mockResponse2 = {
            Payload: Buffer.from(JSON.stringify({ results: [{ id: 2, startTimestamp: '2023-10-10T20:00:15.000Z' }], lastScannedKey: null }))
        };
        sendStub.onFirstCall().resolves(mockResponse1);
        sendStub.onSecondCall().resolves(mockResponse2);

        const dateToVerifyLimit = new Date('2023-10-10T00:00:00.000Z')
        const results = await searchSLAViolations(dateToVerifyLimit);
        expect(results).to.deep.equal([{ id: 1, startTimestamp: '2023-10-10T18:45:15.000Z' }, { id: 2, startTimestamp: '2023-10-10T20:00:15.000Z' }]);
    });

    it('handles errors', async () => {
        sendStub.rejects(new Error('Lambda error'));
		const dateToVerifyLimit = new Date('2023-10-10T00:00:00.000Z')

        try {
            await searchSLAViolations(dateToVerifyLimit);
        } catch (error) {
            expect(error.message).to.equal('Lambda error');
        }
    });
});