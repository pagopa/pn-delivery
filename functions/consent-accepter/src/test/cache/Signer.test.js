const { expect } = require('chai');
const sinon = require('sinon');
const proxyquire = require('proxyquire');

describe('Signer', () => {
  let Signer;
  let mockSignatureV4;
  let mockFormatUrl;
  let mockSignatureV4Class;
  let mockHash;
  let signer;
  let mockCredentials;
  let mockSha256;

  beforeEach(() => {
    // Mock environment variables first
    process.env.REDIS_SERVER_NAME = 'test-redis-server.cache.amazonaws.com';
    
    // Create mocks
    mockSignatureV4 = {
      presign: sinon.stub()
    };
    mockSignatureV4Class = sinon.stub().returns(mockSignatureV4);
    mockFormatUrl = sinon.stub();
    mockCredentials = { accessKeyId: 'test', secretAccessKey: 'test' };
    mockSha256 = sinon.stub();
    
    // Create Hash mock
    mockHash = sinon.stub();
    mockHash.bind = sinon.stub().returns(mockSha256);
    
    // Use proxyquire to inject mocks
    const SignerModule = proxyquire('../../app/cache/Signer', {
      '@aws-sdk/signature-v4': {
        SignatureV4: mockSignatureV4Class
      },
      '@aws-sdk/util-format-url': {
        formatUrl: mockFormatUrl
      },
      '@aws-sdk/hash-node': {
        Hash: mockHash
      }
    });
    
    Signer = SignerModule.Signer;
  });

  afterEach(() => {
    sinon.restore();
    delete process.env.REDIS_SERVER_NAME;
  });

  describe('constructor', () => {
    it('should initialize with default protocol and service', () => {
      const config = {
        credentials: mockCredentials,
        region: 'us-east-1',
        username: 'test-user'
      };
      
      signer = new Signer(config);
      
      expect(signer.protocol).to.equal('https:');
      expect(signer.service).to.equal('elasticache');
      expect(signer.configuration).to.exist;
    });

    it('should call _getRuntimeConfig with provided configuration', () => {
      const config = {
        credentials: mockCredentials,
        region: 'us-east-1',
        username: 'test-user'
      };
      
      signer = new Signer(config);
      
      expect(signer.configuration.credentials).to.equal(mockCredentials);
      expect(signer.configuration.region).to.equal('us-east-1');
      expect(signer.configuration.username).to.equal('test-user');
    });
  });

  describe('_getRuntimeConfig', () => {
    beforeEach(() => {
      signer = new Signer({ credentials: mockCredentials, region: 'us-east-1' });
    });

    it('should return default configuration with runtime and expiresIn', () => {
      const config = {
        credentials: mockCredentials,
        region: 'us-east-1'
      };
      
      const result = signer._getRuntimeConfig(config);
      
      expect(result.runtime).to.equal('node');
      expect(result.expiresIn).to.equal(900);
      expect(result.credentials).to.equal(mockCredentials);
      expect(result.region).to.equal('us-east-1');
    });

    it('should use provided sha256 function when available', () => {
      const customSha256 = sinon.stub();
      const config = {
        credentials: mockCredentials,
        region: 'us-east-1',
        sha256: customSha256
      };
      
      const result = signer._getRuntimeConfig(config);
      
      expect(result.sha256).to.equal(customSha256);
    });

    it('should use Hash.bind when sha256 is not provided', () => {
      const config = {
        credentials: mockCredentials,
        region: 'us-east-1'
      };
      
      const result = signer._getRuntimeConfig(config);
      
      // Check if Hash.bind was called with correct arguments
      sinon.assert.calledWith(mockHash.bind, null, 'sha256');
      expect(result.sha256).to.equal(mockSha256);
    });

    it('should override default expiresIn when provided', () => {
      const config = {
        credentials: mockCredentials,
        region: 'us-east-1',
        expiresIn: 1800
      };
      
      const result = signer._getRuntimeConfig(config);
      
      expect(result.expiresIn).to.equal(1800);
    });

    it('should merge all provided config properties', () => {
      const config = {
        credentials: mockCredentials,
        region: 'us-east-1',
        username: 'test-user',
        hostname: 'test-hostname',
        customProperty: 'custom-value'
      };
      
      const result = signer._getRuntimeConfig(config);
      
      expect(result.username).to.equal('test-user');
      expect(result.hostname).to.equal('test-hostname');
      expect(result.customProperty).to.equal('custom-value');
    });
  });

  describe('getAuthToken', () => {
    beforeEach(() => {
      const config = {
        credentials: mockCredentials,
        region: 'us-east-1',
        username: 'test-user',
        expiresIn: 900
      };
      signer = new Signer(config);
    });

    it('should create SignatureV4 with correct configuration', async () => {
      const mockPresignedUrl = {
        protocol: 'https:',
        hostname: 'test-redis-server.cache.amazonaws.com',
        path: '/',
        query: { signature: 'test-signature' }
      };
      mockSignatureV4.presign.resolves(mockPresignedUrl);
      mockFormatUrl.returns('https://formatted-url');

      await signer.getAuthToken();

      sinon.assert.calledWith(mockSignatureV4Class, {
        service: 'elasticache',
        region: 'us-east-1',
        credentials: mockCredentials,
        sha256: signer.configuration.sha256
      });
    });

    it('should create correct request object', async () => {
      const mockPresignedUrl = {
        protocol: 'https:',
        hostname: 'test-redis-server.cache.amazonaws.com',
        path: '/',
        query: { signature: 'test-signature' }
      };
      mockSignatureV4.presign.resolves(mockPresignedUrl);
      mockFormatUrl.returns('https://formatted-url');

      await signer.getAuthToken();

      sinon.assert.calledWith(mockSignatureV4.presign, {
        method: 'GET',
        protocol: 'https:',
        hostname: 'test-redis-server.cache.amazonaws.com',
        path: '/',
        query: {
          Action: 'connect',
          User: 'test-user',
          ResourceType: 'ServerlessCache'
        },
        headers: {
          host: 'test-redis-server.cache.amazonaws.com'
        }
      }, {
        expiresIn: 900
      });
    });

    it('should format the presigned URL correctly', async () => {
      const mockPresignedUrl = {
        protocol: 'https:',
        hostname: 'test-redis-server.cache.amazonaws.com',
        path: '/',
        query: { signature: 'test-signature' }
      };
      mockSignatureV4.presign.resolves(mockPresignedUrl);
      mockFormatUrl.returns('https://test-redis-server.cache.amazonaws.com/?signature=test-signature');

      const result = await signer.getAuthToken();

      sinon.assert.calledWith(mockFormatUrl, mockPresignedUrl);
      expect(result).to.equal('test-redis-server.cache.amazonaws.com/?signature=test-signature');
    });

    it('should remove protocol from formatted URL', async () => {
      const mockPresignedUrl = {
        protocol: 'https:',
        hostname: 'test-redis-server.cache.amazonaws.com'
      };
      mockSignatureV4.presign.resolves(mockPresignedUrl);
      mockFormatUrl.returns('https://test-redis-server.cache.amazonaws.com/path?query=value');

      const result = await signer.getAuthToken();

      expect(result).to.equal('test-redis-server.cache.amazonaws.com/path?query=value');
    });

    it('should use custom expiresIn from configuration', async () => {
      const config = {
        credentials: mockCredentials,
        region: 'us-east-1',
        username: 'test-user',
        expiresIn: 1800
      };
      signer = new Signer(config);
      
      const mockPresignedUrl = { protocol: 'https:', hostname: 'test' };
      mockSignatureV4.presign.resolves(mockPresignedUrl);
      mockFormatUrl.returns('https://formatted-url');

      await signer.getAuthToken();

      sinon.assert.calledWith(mockSignatureV4.presign,
        sinon.match.any,
        { expiresIn: 1800 }
      );
    });

    it('should handle errors from SignatureV4.presign', async () => {
      const error = new Error('Presign error');
      mockSignatureV4.presign.rejects(error);

      try {
        await signer.getAuthToken();
        expect.fail('Should have thrown an error');
      } catch (err) {
        expect(err).to.equal(error);
      }
    });

    it('should handle errors from formatUrl', async () => {
      const mockPresignedUrl = { protocol: 'https:', hostname: 'test' };
      mockSignatureV4.presign.resolves(mockPresignedUrl);
      
      const error = new Error('Format URL error');
      mockFormatUrl.throws(error);

      try {
        await signer.getAuthToken();
        expect.fail('Should have thrown an error');
      } catch (err) {
        expect(err).to.equal(error);
      }
    });

    it('should use environment variable for hostname in request and headers', async () => {
      process.env.REDIS_SERVER_NAME = 'custom-redis-server.amazonaws.com';
      
      const mockPresignedUrl = { protocol: 'https:', hostname: 'test' };
      mockSignatureV4.presign.resolves(mockPresignedUrl);
      mockFormatUrl.returns('https://formatted-url');

      await signer.getAuthToken();

      sinon.assert.calledWith(mockSignatureV4.presign,
        sinon.match({
          hostname: 'custom-redis-server.amazonaws.com',
          headers: {
            host: 'custom-redis-server.amazonaws.com'
          }
        }),
        sinon.match.any
      );
    });
  });
});