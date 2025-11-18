const proxyquire = require('proxyquire');
const sinon = require('sinon');
const { expect } = require('chai');

process.env.REDIS_NAMESPACE = "testNamespace::";

describe('Consent Handler Tests', () => {
  let RestClientStub;
  let utilsStub;
  let CacheManagerStub;
  let mockCacheManagerInstance;

  beforeEach(() => {
    // Setup stubs
    RestClientStub = {
      putConsents: sinon.stub(),
      checkQrCode: sinon.stub(),
      getLastVersion: sinon.stub()
    };

    utilsStub = {
      getUserInfoFromEvent: sinon.stub(),
      retrieveHeadersToForward: sinon.stub()
    };

    mockCacheManagerInstance = {
      get: sinon.stub()
    };

    CacheManagerStub = sinon.stub().returns(mockCacheManagerInstance);


    // Setup delle variabili d'ambiente
    process.env.CONSENTS_TO_ACCEPT = JSON.stringify([
      { consentType: 'TOS', version: 'v1' },
      { consentType: 'PRIVACY' }
    ]);
    process.env.CACHE_ITEM_TTL_SECONDS = '60';

    // Carica il modulo con proxyquire
    handler = proxyquire('../app/eventHandler', {
      './services': RestClientStub,
      './utils': utilsStub,
      './cache/CacheManager': CacheManagerStub
    });
  });

  afterEach(() => {
    sinon.restore();
    delete process.env.CONSENTS_TO_ACCEPT;
    delete process.env.CACHE_ITEM_TTL_SECONDS;
  });

  describe('handle function', () => {
    it('should successfully process consents and return delivery response', async () => {
      // Arrange
      const mockEvent = {
        body: 'qrCodeData',
        headers: { 'x-custom-header': 'value' }
      };
      
      const mockUserInfo = {
        uid: 'user123',
        cxType: 'PF'
      };

      const mockDeliveryResponse = {
        statusCode: 200,
        body: { success: true }
      };

      utilsStub.getUserInfoFromEvent.returns(mockUserInfo);
      utilsStub.retrieveHeadersToForward.returns({ 'x-custom-header': 'value' });
      RestClientStub.putConsents.resolves({});
      RestClientStub.checkQrCode.resolves(mockDeliveryResponse);
      mockCacheManagerInstance.get.returns('v2');

      // Act
      const result = await handler.handle(mockEvent);

      // Assert
      expect(utilsStub.getUserInfoFromEvent.calledOnce).to.be.true;
      expect(utilsStub.getUserInfoFromEvent.calledWith(mockEvent)).to.be.true;
      expect(RestClientStub.putConsents.callCount).to.equal(2);
      expect(RestClientStub.putConsents.firstCall.args).to.deep.equal(['TOS', 'v1', 'user123', 'PF']);
      expect(RestClientStub.putConsents.secondCall.args).to.deep.equal(['PRIVACY', 'v2', 'user123', 'PF']);
      expect(RestClientStub.checkQrCode.calledOnce).to.be.true;
      expect(RestClientStub.checkQrCode.calledWith(
        'qrCodeData',
        { 'x-custom-header': 'value' },
        mockUserInfo
      )).to.be.true;
      expect(result).to.deep.equal(mockDeliveryResponse);
    });

    it('should return 500 error when an exception occurs', async () => {
      // Arrange
      const mockEvent = {
        body: 'qrCodeData',
        headers: {}
      };

      utilsStub.getUserInfoFromEvent.throws(new Error('User info extraction failed'));

      // Act
      const result = await handler.handle(mockEvent);

      // Assert
      expect(result.statusCode).to.equal(500);
      const body = JSON.parse(result.body);
      expect(body).to.have.property('type', 'GENERIC_ERROR');
      expect(body).to.have.property('status', 500);
      expect(body).to.have.property('title', 'Handled error');
      expect(body.errors).to.be.an('array').with.lengthOf(1);
      expect(body.errors[0]).to.deep.include({
        code: 'INTERNAL_ERROR',
        element: null,
        detail: 'Error executing request'
      });
    });

    it('should handle empty headers gracefully', async () => {
      // Arrange
      const mockEvent = {
        body: 'qrCodeData'
        // headers non definito
      };

      const mockUserInfo = { uid: 'user123', cxType: 'PF' };
      utilsStub.getUserInfoFromEvent.returns(mockUserInfo);
      utilsStub.retrieveHeadersToForward.returns({});
      RestClientStub.putConsents.resolves({});
      RestClientStub.checkQrCode.resolves({ statusCode: 200 });
      mockCacheManagerInstance.get.returns('v1');

      // Act
      const result = await handler.handle(mockEvent);

      // Assert
      expect(utilsStub.retrieveHeadersToForward.calledWith({})).to.be.true;
      expect(result.statusCode).to.equal(200);
    });

    it('should handle putConsents failures gracefully', async () => {
      // Arrange
      const mockEvent = { body: 'data', headers: {} };
      const mockUserInfo = { uid: 'user123', cxType: 'PF' };
      
      utilsStub.getUserInfoFromEvent.returns(mockUserInfo);
      RestClientStub.putConsents.rejects(new Error('Network error'));
      mockCacheManagerInstance.get.returns('v1');

      // Act
      const result = await handler.handle(mockEvent);

      // Assert
      expect(result.statusCode).to.equal(500);
      const body = JSON.parse(result.body);
      expect(body.errors[0].detail).to.equal('Error executing request');
    });
  });

  describe('validateConsentsToAccept function', () => {
    it('should throw error when CONSENTS_TO_ACCEPT is not set', async () => {
      // Arrange
      delete process.env.CONSENTS_TO_ACCEPT;
      const mockEvent = { body: 'data', headers: {} };
      utilsStub.getUserInfoFromEvent.returns({ uid: 'user123', cxType: 'PF' });

      // Act
      const result = await handler.handle(mockEvent);

      // Assert
      expect(result.statusCode).to.equal(500);
    });

    it('should throw error when CONSENTS_TO_ACCEPT is not a valid JSON', async () => {
      // Arrange
      process.env.CONSENTS_TO_ACCEPT = 'not-a-json';
      const mockEvent = { body: 'data', headers: {} };
      utilsStub.getUserInfoFromEvent.returns({ uid: 'user123', cxType: 'PF' });

      // Act
      const result = await handler.handle(mockEvent);

      // Assert
      expect(result.statusCode).to.equal(500);
    });

    it('should throw error when CONSENTS_TO_ACCEPT is not an array', async () => {
      // Arrange
      process.env.CONSENTS_TO_ACCEPT = JSON.stringify({ consentType: 'TOS' });
      const mockEvent = { body: 'data', headers: {} };
      utilsStub.getUserInfoFromEvent.returns({ uid: 'user123', cxType: 'PF' });

      // Act
      const result = await handler.handle(mockEvent);

      // Assert
      expect(result.statusCode).to.equal(500);
    });

    it('should throw error when consent object lacks consentType', async () => {
      // Arrange
      process.env.CONSENTS_TO_ACCEPT = JSON.stringify([{ version: 'v1' }]);
      const mockEvent = { body: 'data', headers: {} };
      utilsStub.getUserInfoFromEvent.returns({ uid: 'user123', cxType: 'PF' });

      // Act
      const result = await handler.handle(mockEvent);

      // Assert
      expect(result.statusCode).to.equal(500);
    });

    it('should accept valid consents configuration', async () => {
      // Arrange
      process.env.CONSENTS_TO_ACCEPT = JSON.stringify([
        { consentType: 'TOS', version: 'v1' },
        { consentType: 'PRIVACY' }
      ]);
      const mockEvent = { body: 'data', headers: {} };
      const mockUserInfo = { uid: 'user123', cxType: 'PF' };
      
      utilsStub.getUserInfoFromEvent.returns(mockUserInfo);
      utilsStub.retrieveHeadersToForward.returns({});
      RestClientStub.putConsents.resolves({});
      RestClientStub.checkQrCode.resolves({ statusCode: 200 });
      mockCacheManagerInstance.get.returns('v2');

      // Act
      const result = await handler.handle(mockEvent);

      // Assert
      expect(result.statusCode).to.equal(200);
    });
  });

  describe('acceptConsent scenarios', () => {
    it('should use provided version when available', async () => {
      // Arrange
      const mockEvent = {
        body: 'qrCodeData',
        headers: {}
      };

      process.env.CONSENTS_TO_ACCEPT = JSON.stringify([
        { consentType: 'TOS', version: 'v3' }
      ]);

      const mockUserInfo = { uid: 'user123', cxType: 'PF' };
      utilsStub.getUserInfoFromEvent.returns(mockUserInfo);
      utilsStub.retrieveHeadersToForward.returns({});
      RestClientStub.putConsents.resolves({});
      RestClientStub.checkQrCode.resolves({ statusCode: 200 });

      // Act
      await handler.handle(mockEvent);

      // Assert
      expect(RestClientStub.putConsents.calledOnce).to.be.true;
      expect(RestClientStub.putConsents.calledWith('TOS', 'v3', 'user123', 'PF')).to.be.true;
      expect(mockCacheManagerInstance.get.called).to.be.false;
    });

    it('should fetch version from cache when not provided', async () => {
      // Arrange
      const mockEvent = {
        body: 'qrCodeData',
        headers: {}
      };

      process.env.CONSENTS_TO_ACCEPT = JSON.stringify([
        { consentType: 'PRIVACY' }
      ]);

      const mockUserInfo = { uid: 'user123', cxType: 'PG' };
      utilsStub.getUserInfoFromEvent.returns(mockUserInfo);
      utilsStub.retrieveHeadersToForward.returns({});
      RestClientStub.putConsents.resolves({});
      RestClientStub.checkQrCode.resolves({ statusCode: 200 });
      mockCacheManagerInstance.get.returns('v5');

      // Act
      await handler.handle(mockEvent);

      // Assert
      expect(mockCacheManagerInstance.get.calledOnce).to.be.true;
      expect(mockCacheManagerInstance.get.calledWith('PG', 'PRIVACY')).to.be.true;
      expect(RestClientStub.putConsents.calledWith('PRIVACY', 'v5', 'user123', 'PG')).to.be.true;
    });
  });

  describe('Integration scenarios', () => {
    it('should handle multiple consents with mixed version sources', async () => {
      // Arrange
      const mockEvent = {
        body: 'qrCodeData',
        headers: { 'x-api-key': 'test-key' }
      };

      process.env.CONSENTS_TO_ACCEPT = JSON.stringify([
        { consentType: 'TOS', version: 'v1' },
        { consentType: 'PRIVACY' },
        { consentType: 'MARKETING', version: 'v2' }
      ]);

      const mockUserInfo = { uid: 'user456', cxType: 'PG' };
      utilsStub.getUserInfoFromEvent.returns(mockUserInfo);
      utilsStub.retrieveHeadersToForward.returns({ 'x-api-key': 'test-key' });
      RestClientStub.putConsents.resolves({});
      RestClientStub.checkQrCode.resolves({ statusCode: 200, body: 'ok' });
      mockCacheManagerInstance.get.returns('v3');

      // Act
      const result = await handler.handle(mockEvent);

      // Assert
      expect(RestClientStub.putConsents.callCount).to.equal(3);
      expect(RestClientStub.putConsents.getCall(0).args).to.deep.equal(['TOS', 'v1', 'user456', 'PG']);
      expect(RestClientStub.putConsents.getCall(1).args).to.deep.equal(['PRIVACY', 'v3', 'user456', 'PG']);
      expect(RestClientStub.putConsents.getCall(2).args).to.deep.equal(['MARKETING', 'v2', 'user456', 'PG']);
      expect(mockCacheManagerInstance.get.calledOnce).to.be.true;
      expect(result.statusCode).to.equal(200);
    });

  });

  describe('Error response format', () => {
    it('should generate proper error structure', async () => {
      // Arrange
      const mockEvent = { body: 'data', headers: {} };
      utilsStub.getUserInfoFromEvent.throws(new Error('Test error'));

      // Act
      const result = await handler.handle(mockEvent);
      const body = JSON.parse(result.body);

      // Assert
      expect(body).to.have.property('type', 'GENERIC_ERROR');
      expect(body).to.have.property('status', 500);
      expect(body).to.have.property('title', 'Handled error');
      expect(body).to.have.property('timestamp');
      expect(body.errors).to.be.an('array').with.lengthOf(1);
      expect(body.errors[0]).to.deep.equal({
        code: 'INTERNAL_ERROR',
        element: null,
        detail: 'Error executing request'
      });
    });
  });
});