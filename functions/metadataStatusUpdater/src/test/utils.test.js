const { expect } = require('chai');
const { decodePayload, shouldSkipEvaluation, extractYearMonth, arrayToString } = require('../app/lib/utils');
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

  describe('extractYearMonth', () => {
    it('should extract year and month from date string', () => {
      const date = '2023-10-05T12:34:56Z';

      const result = extractYearMonth(date);

      expect(result).to.equal('202310');
    });
  });

  describe('arrayToString', () => {
    it('should convert array to string', () => {
      const array = [1, 2, 3];

      const result = arrayToString(array);

      expect(result).to.equal('[1,2,3]');
    });
  });
});