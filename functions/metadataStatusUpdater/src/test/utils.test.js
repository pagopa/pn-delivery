const { expect } = require('chai');
const { decodePayload, parseKinesisObjToJsonObj, shouldSkipEvaluation, extractYearMonth, arrayToString } = require('../app/lib/utils');
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

  describe('shouldSkipEvaluation', () => {
    it('should return true for non-allowed table', () => {
      const rec = { tableName: 'non-allowed-table', eventSource: 'aws:dynamodb', eventName: 'INSERT' };

      const result = shouldSkipEvaluation(rec);

      expect(result).to.be.true;
    });

    it('should return true for non-allowed event source', () => {
      const rec = { tableName: 'pn-Timelines', eventSource: 'non-allowed-source', eventName: 'INSERT' };

      const result = shouldSkipEvaluation(rec);

      expect(result).to.be.true;
    });

    it('should return true for non-INSERT event', () => {
      const rec = { tableName: 'pn-Timelines', eventSource: 'aws:dynamodb', eventName: 'MODIFY' };

      const result = shouldSkipEvaluation(rec);

      expect(result).to.be.true;
    });

    it('should return false for allowed table, event source, and INSERT event', () => {
      const rec = { tableName: 'pn-Timelines', eventSource: 'aws:dynamodb', eventName: 'INSERT' };

      const result = shouldSkipEvaluation(rec);

      expect(result).to.be.false;
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

  describe("parseKinesisObjToJsonObj", () => {

    it("should parse kinesis obj", () => {
      const kinesisObj = {
        iun: {
          S: "abcd",
        },
        timelineElementId: {
          S: "notification_viewed_creation_request;IUN_XLDW-MQYJ-WUKA-202302-A-1;RECINDEX_1",
        },
        notificationSentAt: {
          S: "2023-01-20T14:48:00.000Z",
        },
        timestamp: {
          S: "2023-01-20T14:48:00.000Z",
        },
        paId: {
          S: "026e8c72-7944-4dcd-8668-f596447fec6d",
        },
        details: {
          M: {
            notificationCost: {
              N: 100,
            },
            recIndex: {
              N: 0,
            },
            aarKey: {
              S: "safestorage://PN_AAR-0002-YCUO-BZCH-9MKQ-EGKG",
            },
          },
        },
      };
      const parsedObj = parseKinesisObjToJsonObj(kinesisObj);
      expect(parsedObj).to.eql({
        iun: "abcd",
        timelineElementId:
          "notification_viewed_creation_request;IUN_XLDW-MQYJ-WUKA-202302-A-1;RECINDEX_1",
        notificationSentAt: "2023-01-20T14:48:00.000Z",
        timestamp: "2023-01-20T14:48:00.000Z",
        paId: "026e8c72-7944-4dcd-8668-f596447fec6d",
        details: {
          notificationCost: 100,
          recIndex: 0,
          aarKey: "safestorage://PN_AAR-0002-YCUO-BZCH-9MKQ-EGKG",
        },
      });
    });
  
    it("no kinesis obj", () => {
      const parsedObj = parseKinesisObjToJsonObj(null);
      expect(parsedObj).equal(null);
    });
  });
});