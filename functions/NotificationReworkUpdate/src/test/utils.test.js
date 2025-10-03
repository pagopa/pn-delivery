const { expect } = require('chai');
const { decodePayload } = require('../app/lib/utils');
const { Buffer } = require('node:buffer');

describe('utils tests', () => {
  describe('decodePayload', () => {
    it('should decode base64 JSON string', () => {
      const json = { key: 'value' };
      const b64Str = Buffer.from(JSON.stringify(json)).toString('base64');

      const result = decodePayload(b64Str);

      expect(result).to.deep.equal(json);
    });

    it('should throw an error for invalid base64 string', () => {
      const invalidB64Str = 'invalid_base64';

      expect(() => decodePayload(invalidB64Str)).to.throw();
    });
  });
});