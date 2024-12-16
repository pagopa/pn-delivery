const { headerMapper } = require('../app/utils');
const { expect } = require("chai");

describe('headerMapper', () => {
  it('should correctly create headers with all authorizer fields', () => {
    const event = {
      headers: {
        "existing-header": "value1",
      },
      requestContext: {
        authorizer: {
          cx_groups: "group1",
          cx_id: "id123",
          cx_role: "roleABC",
          cx_type: "typeX",
          cx_jti: "jtiToken",
          sourceChannelDetails: "channelDetails",
          uid: "user123",
        },
      },
    };

    const expectedHeaders = {
      "existing-header": "value1",
      "x-pagopa-pn-src-ch": "B2B",
      "x-pagopa-pn-cx-groups": "group1",
      "x-pagopa-pn-cx-id": "id123",
      "x-pagopa-pn-cx-role": "roleABC",
      "x-pagopa-pn-cx-type": "typeX",
      "x-pagopa-pn-jti": "jtiToken",
      "x-pagopa-pn-src-ch-details": "channelDetails",
      "x-pagopa-pn-uid": "user123",
    };

    expect(headerMapper(event)).to.deep.equal(expectedHeaders);
  });

  it('should handle missing authorizer fields gracefully', () => {
    const event = {
      headers: {
        "existing-header": "value1",
      },
      requestContext: {
        authorizer: {
          cx_groups: "group1",
          cx_id: "id123",
        },
      },
    };

    const expectedHeaders = {
      "existing-header": "value1",
      "x-pagopa-pn-src-ch": "B2B",
      "x-pagopa-pn-cx-groups": "group1",
      "x-pagopa-pn-cx-id": "id123",
    };

    expect(headerMapper(event)).to.deep.equal(expectedHeaders);
  });

  it('should correctly handle an empty authorizer object', () => {
    const event = {
      headers: {
        "existing-header": "value1",
      },
      requestContext: {
        authorizer: {},
      },
    };

    const expectedHeaders = {
      "existing-header": "value1",
      "x-pagopa-pn-src-ch": "B2B",
    };

    expect(headerMapper(event)).to.deep.equal(expectedHeaders);
  });
});
