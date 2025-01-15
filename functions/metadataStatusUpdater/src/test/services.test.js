const chai = require('chai');
const sinon = require('sinon');
const axios = require('axios');
const RestClient = require('../app/lib/services');
const expect = chai.expect;

describe('RestClient', () => {
  let axiosGetStub;

  beforeEach(() => {
    axiosGetStub = sinon.stub(axios, 'get');
  });

  afterEach(() => {
    sinon.restore();
  });

  describe('getRootSenderId', () => {
    it('should return root sender ID', async () => {
      const senderPaId = 'testSenderPaId';
      const expectedRootId = 'rootId123';
      axiosGetStub.resolves({ data: { rootId: expectedRootId } });

      const rootId = await RestClient.getRootSenderId(senderPaId);

      sinon.assert.calledOnce(axiosGetStub);
      expect(axiosGetStub.firstCall.args[0]).to.equal(`${process.env.PN_EXTERNAL_REGISTRIES_BASE_URL}/ext-registry-private/pa/v1/${senderPaId}/root-id`);
      expect(rootId).to.equal(expectedRootId);
    });

    it('should throw an error if the request fails', async () => {
      const senderPaId = 'testSenderPaId';
      const errorMessage = 'Request failed';
      axiosGetStub.rejects(new Error(errorMessage));

      try {
        await RestClient.getRootSenderId(senderPaId);
      } catch (err) {
        expect(err.message).to.equal(errorMessage);
      }
    });
  });

  describe('getMandates', () => {
    it('should return mandates', async () => {
      const recipientId = 'testRecipientId';
      const expectedMandates = [{ mandateId: 'mandate1' }, { mandateId: 'mandate2' }];
      axiosGetStub.resolves({ data: expectedMandates });

      const mandates = await RestClient.getMandates(recipientId);

      sinon.assert.calledOnce(axiosGetStub);
      expect(axiosGetStub.firstCall.args[0]).to.equal(`${process.env.PN_MANDATE_BASE_URL}/mandate-private/api/v1/mandates-by-internaldelegator/${recipientId}`);
      expect(axiosGetStub.firstCall.args[1]).to.deep.equal({ params: { delegateType: 'PG' } });
      expect(mandates).to.deep.equal(expectedMandates);
    });

    it('should throw an error if the request fails', async () => {
      const recipientId = 'testRecipientId';
      const errorMessage = 'Request failed';
      axiosGetStub.rejects(new Error(errorMessage));

      try {
        await RestClient.getMandates(recipientId);
      } catch (err) {
        expect(err.message).to.equal(errorMessage);
      }
    });
  });
});