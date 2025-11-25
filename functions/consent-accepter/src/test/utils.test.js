const { expect } = require('chai');
const sinon = require('sinon');
const { getUserInfoFromEvent, retrieveHeadersToForward, removeCxPrefix } = require('../app/utils');

describe('utils', () => {

  beforeEach(() => {
  });

  afterEach(() => {
    sinon.restore();
  });

  describe('retrieveHeadersToForward', () => {
    it('should return only headers that are in HEADERS_TO_RETRIEVE list', () => {
      const inputHeaders = {
        'x-pagopa-lollipop-assertion-ref': 'ref123',
        'x-pagopa-lollipop-auth-jwt': 'jwt-token',
        'authorization': 'Bearer token',
        'content-type': 'application/json',
        'signature': 'sig123',
        'x-custom-header': 'custom-value'
      };

      const result = retrieveHeadersToForward(inputHeaders);

      expect(result).to.deep.equal({
        'x-pagopa-lollipop-assertion-ref': 'ref123',
        'x-pagopa-lollipop-auth-jwt': 'jwt-token',
        'signature': 'sig123'
      });
    });

    it('should return all allowed headers when all are present', () => {
      const inputHeaders = {
        'x-pagopa-lollipop-assertion-ref': 'ref123',
        'x-pagopa-lollipop-assertion-type': 'type123',
        'x-pagopa-lollipop-auth-jwt': 'jwt-token',
        'x-pagopa-lollipop-original-method': 'POST',
        'x-pagopa-lollipop-original-url': 'https://example.com',
        'x-pagopa-lollipop-public-key': 'pubkey123',
        'x-pagopa-lollipop-user-id': 'user123',
        'x-pagopa-pn-src-ch': 'IO',
        'signature': 'sig123',
        'signature-input': 'input123',
        'extra-header': 'should-not-appear'
      };

      const result = retrieveHeadersToForward(inputHeaders);

      expect(result).to.deep.equal({
        'x-pagopa-lollipop-assertion-ref': 'ref123',
        'x-pagopa-lollipop-assertion-type': 'type123',
        'x-pagopa-lollipop-auth-jwt': 'jwt-token',
        'x-pagopa-lollipop-original-method': 'POST',
        'x-pagopa-lollipop-original-url': 'https://example.com',
        'x-pagopa-lollipop-public-key': 'pubkey123',
        'x-pagopa-lollipop-user-id': 'user123',
        'x-pagopa-pn-src-ch': 'IO',
        'signature': 'sig123',
        'signature-input': 'input123'
      });
    });

    it('should return empty object when no allowed headers are present', () => {
      const inputHeaders = {
        'authorization': 'Bearer token',
        'content-type': 'application/json',
        'user-agent': 'test-agent',
        'accept': 'application/json'
      };

      const result = retrieveHeadersToForward(inputHeaders);

      expect(result).to.deep.equal({});
    });

    it('should handle empty headers object', () => {
      const result = retrieveHeadersToForward({});

      expect(result).to.deep.equal({});
    });

    it('should handle null or undefined headers gracefully', () => {
      expect(() => retrieveHeadersToForward(null)).to.throw();
      expect(() => retrieveHeadersToForward(undefined)).to.throw();
    });

  });

  describe('removeCxPrefix', () => {
    it('should remove PF- prefix', () => {
      const result = removeCxPrefix('PF-user123');
      expect(result).to.equal('user123');
    });

    it('should remove PG- prefix', () => {
      const result = removeCxPrefix('PG-company456');
      expect(result).to.equal('company456');
    });

    it('should remove PA- prefix', () => {
      const result = removeCxPrefix('PA-admin789');
      expect(result).to.equal('admin789');
    });

    it('should return original string if no prefix matches', () => {
      const result = removeCxPrefix('user123');
      expect(result).to.equal('user123');
    });

    it('should handle empty string', () => {
      const result = removeCxPrefix('');
      expect(result).to.equal('');
    });

    it('should handle null or undefined gracefully', () => {
      expect(() => removeCxPrefix(null)).to.throw();
      expect(() => removeCxPrefix(undefined)).to.throw();
    });
  });

  describe('getUserInfoFromEvent', () => {
    it('should extract user info from valid event', () => {
      const event = {
        requestContext: {
          authorizer: {
            cx_id: 'PF-user123',
            cx_type: 'PF'
          }
        },
        headers: {
          'x-pagopa-cx-taxid': 'RSSMRA80A01H501U'
        }
      };

      const result = getUserInfoFromEvent(event);

      expect(result).to.deep.equal({
        uid: 'user123',
        cxType: 'PF',
        cxId: 'PF-user123',
        taxId: 'RSSMRA80A01H501U'
      });
    });

    it('should throw error when cxId is missing', () => {
      const event = {
        requestContext: {
          authorizer: {
            cx_type: 'PF'
          }
        },
        headers: {
          'x-pagopa-cx-taxid': 'RSSMRA80A01H501U'
        }
      };

      expect(() => getUserInfoFromEvent(event)).to.throw('Missing required info: cxId ');
    });

    it('should throw error when cxType is missing', () => {
      const event = {
        requestContext: {
          authorizer: {
            cx_id: 'PF-user123'
          }
        },
        headers: {
          'x-pagopa-cx-taxid': 'RSSMRA80A01H501U'
        }
      };

      expect(() => getUserInfoFromEvent(event)).to.throw('Missing required info: cxType ');
    });

    it('should throw error when taxId is missing', () => {
      const event = {
        requestContext: {
          authorizer: {
            cx_id: 'PF-user123',
            cx_type: 'PF'
          }
        },
        headers: {}
      };

      expect(() => getUserInfoFromEvent(event)).to.throw('Missing required info: taxId ');
    });

    it('should throw error when multiple fields are missing', () => {
      const event = {
        requestContext: {
          authorizer: {}
        },
        headers: {}
      };

      expect(() => getUserInfoFromEvent(event)).to.throw('Missing required info: cxId cxType taxId ');
    });

    it('should handle event with missing requestContext', () => {
      const event = {
        headers: {
          'x-pagopa-cx-taxid': 'RSSMRA80A01H501U'
        }
      };

      expect(() => getUserInfoFromEvent(event)).to.throw('Missing required info: cxId cxType ');
    });

    it('should handle event with missing authorizer', () => {
      const event = {
        requestContext: {},
        headers: {
          'x-pagopa-cx-taxid': 'RSSMRA80A01H501U'
        }
      };

      expect(() => getUserInfoFromEvent(event)).to.throw('Missing required info: cxId cxType ');
    });

    it('should handle event with missing headers', () => {
      const event = {
        requestContext: {
          authorizer: {
            cx_id: 'PF-user123',
            cx_type: 'PF'
          }
        }
      };

      expect(() => getUserInfoFromEvent(event)).to.throw('Missing required info: taxId ');
    });

    

    it('should handle additional headers without affecting result', () => {
      const event = {
        requestContext: {
          authorizer: {
            cx_id: 'PF-user123',
            cx_type: 'PF',
            extra_field: 'should_be_ignored'
          }
        },
        headers: {
          'x-pagopa-cx-taxid': 'RSSMRA80A01H501U',
          'authorization': 'Bearer token',
          'content-type': 'application/json'
        }
      };

      const result = getUserInfoFromEvent(event);

      expect(result).to.deep.equal({
        uid: 'user123',
        cxType: 'PF',
        cxId: 'PF-user123',
        taxId: 'RSSMRA80A01H501U'
      });
    });
  });

  describe('checkRequiredUserInfo (internal function behavior)', () => {
    it('should handle mixed missing and present fields', () => {
      const event = {
        requestContext: {
          authorizer: {
            cx_id: 'PF-user123'
            // missing cx_type
          }
        },
        headers: {
          // missing taxId
        }
      };

      expect(() => getUserInfoFromEvent(event)).to.throw('Missing required info: cxType taxId ');
    });

    it('should handle fields with falsy values', () => {
      const event = {
        requestContext: {
          authorizer: {
            cx_id: 'PF-user123',
            cx_type: 0 // falsy but not undefined
          }
        },
        headers: {
          'x-pagopa-cx-taxid': false // falsy but not undefined
        }
      };

      expect(() => getUserInfoFromEvent(event)).to.throw('Missing required info: cxType taxId ');
    });

  });

  describe('integration scenarios', () => {
    it('should work with complete real-world event structure', () => {
      const event = {
        resource: '/consent-accepter',
        path: '/consent-accepter',
        httpMethod: 'POST',
        headers: {
          'accept': 'application/json',
          'content-type': 'application/json',
          'x-pagopa-cx-taxid': 'RSSMRA80A01H501U',
          'x-pagopa-lollipop-assertion-ref': 'ref123',
          'authorization': 'Bearer jwt-token'
        },
        requestContext: {
          resourceId: 'abc123',
          resourcePath: '/consent-accepter',
          httpMethod: 'POST',
          authorizer: {
            cx_id: 'PF-user123',
            cx_type: 'PF',
            source_channel: 'IO'
          }
        },
        body: 'QR_CODE_CONTENT'
      };

      const userInfo = getUserInfoFromEvent(event);
      const headersToForward = retrieveHeadersToForward(event.headers);

      expect(userInfo).to.deep.equal({
        uid: 'user123',
        cxType: 'PF',
        cxId: 'PF-user123',
        taxId: 'RSSMRA80A01H501U'
      });

      expect(headersToForward).to.deep.equal({
        'x-pagopa-lollipop-assertion-ref': 'ref123'
      });
    });

  });

  describe('error message formatting', () => {
    it('should create properly formatted error messages', () => {
      const event = {
        requestContext: {
          authorizer: {}
        },
        headers: {}
      };

      try {
        getUserInfoFromEvent(event);
        expect.fail('Should have thrown an error');
      } catch (error) {
        expect(error.message).to.equal('Missing required info: cxId cxType taxId ');
        expect(error.message.endsWith(' ')).to.be.true; // trailing space
      }
    });

    it('should handle single missing field error message', () => {
      const event = {
        requestContext: {
          authorizer: {
            cx_id: 'PF-user123',
            cx_type: 'PF'
          }
        },
        headers: {}
      };

      try {
        getUserInfoFromEvent(event);
        expect.fail('Should have thrown an error');
      } catch (error) {
        expect(error.message).to.equal('Missing required info: taxId ');
      }
    });
  });
});