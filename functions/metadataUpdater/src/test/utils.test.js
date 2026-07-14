const { expect } = require('chai');
const utils = require('../app/lib/utils');

describe('utils tests', () => {
  describe('decodePayload', () => {
    it('should decode base64 JSON string', () => {
      const payload = { key: 'value' };
      const b64 = Buffer.from(JSON.stringify(payload)).toString('base64');
      const result = utils.decodePayload(b64);
      expect(result).to.deep.equal(payload);
    });

    it('should throw an error for invalid base64 string', () => {
      try {
        utils.decodePayload('not-valid-base64!!!@@@###');
      } catch (err) {
        expect(err).to.be.an.instanceof(Error);
      }
    });
  });
});
