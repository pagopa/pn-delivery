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

        const results = await searchSLAViolations('2023-10-10T00:00:00.000Z');
        expect(results).to.deep.equal([]);
    });

    it('returns results when there are SLA violations', async () => {
        const mockResponse = {
            Payload: Buffer.from(JSON.stringify({ results: [{ id: 1 }], lastScannedKey: null }))
        };
        sendStub.resolves(mockResponse);

        const results = await searchSLAViolations('2023-10-10T00:00:00.000Z');
        expect(results).to.deep.equal([{ id: 1 }]);
    });

    it('handles multiple pages of results', async () => {
        const mockResponse1 = {
            Payload: Buffer.from(JSON.stringify({ results: [{ id: 1 }], lastScannedKey: 'key1' }))
        };
        const mockResponse2 = {
            Payload: Buffer.from(JSON.stringify({ results: [{ id: 2 }], lastScannedKey: null }))
        };
        sendStub.onFirstCall().resolves(mockResponse1);
        sendStub.onSecondCall().resolves(mockResponse2);

        const results = await searchSLAViolations('2023-10-10T00:00:00.000Z');
        expect(results).to.deep.equal([{ id: 1 }, { id: 2 }]);
    });

    it('handles errors', async () => {
        sendStub.rejects(new Error('Lambda error'));

        try {
            await searchSLAViolations('2023-10-10T00:00:00.000Z');
        } catch (error) {
            expect(error.message).to.equal('Lambda error');
        }
    });
});