const { expect } = require('chai');
const { extractKinesisData } = require('../app/lib/utils');
const { Buffer } = require('node:buffer');

describe('utils tests', () => {
  describe('extractKinesisData', () => {
    it('should extract kinesis data from base64 JSON string', () => {
      
      const json = { key: 'value' };
      const b64Str = Buffer.from(JSON.stringify(json)).toString('base64');
      const cdc = buildKinesisEvent(b64Str);

      const records = extractKinesisData(cdc);

      const result = records[0];

      expect(result.kinesisSeqNumber).to.deep.equal(cdc.Records[0].kinesis.sequenceNumber);
      expect(result.key).to.deep.equal(json.key);
    });

    it('should throw an error for invalid base64 string', () => {
      const invalidB64Str = 'invalid_base64';
      const cdc = buildKinesisEvent(invalidB64Str);

      expect(() => extractKinesisData(invalidB64Str)).to.throw();
    });
  });


  function buildKinesisEvent(data) {
    return {
      "Records": [
        {
          "kinesis": {
            "partitionKey": "partitionKey-03",
            "kinesisSchemaVersion": "1.0",
            "data": data,
            "sequenceNumber": "49735115243490431018280067714973111582180062593243322962",
            "approximateArrivalTimestamp": 1428537600
          },
          "eventSource": "aws:kinesis",
          "eventID": "shardId-000000000000:49545115243490985018280067714973144582180062593244200961",
          "invokeIdentityArn": "arn:aws:iam::EXAMPLE",
          "eventVersion": "1.0",
          "eventName": "aws:kinesis:record",
          "eventSourceARN": "arn:aws:kinesis:EXAMPLE",
          "awsRegion": "us-east-1"
        }
      ]
    }
  }
});