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
});
