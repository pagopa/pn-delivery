const { expect } = require('chai');
const sinon = require('sinon');
const proxyquire = require('proxyquire');

describe('RedisClientProvider', () => {
  let RedisClientProvider;
  let redisClientProvider;
  let mockCreateClient;
  let mockFromNodeProviderChain;
  let mockSigner;
  let mockSignerClass;
  let clock;

  beforeEach(() => {
    // Mock environment variables first
    process.env.REDIS_ENDPOINT = 'test-redis-endpoint';
    process.env.AWS_REGION = 'us-east-1';
    process.env.REDIS_SERVER_NAME = 'test-server';
    process.env.USER_ID_REDIS = 'test-user';
    
    // Create mocks
    mockCreateClient = sinon.stub();
    mockFromNodeProviderChain = sinon.stub().resolves({ accessKeyId: 'test', secretAccessKey: 'test' });
    mockSigner = {
      getAuthToken: sinon.stub().resolves('mock-auth-token')
    };
    mockSignerClass = sinon.stub().returns(mockSigner);
    
    // Use proxyquire to inject mocks
    RedisClientProvider = proxyquire('../../app/cache/RedisClientProvider', {
      'redis': {
        createClient: mockCreateClient
      },
      '@aws-sdk/credential-providers': {
        fromNodeProviderChain: () => mockFromNodeProviderChain
      },
      './Signer': {
        Signer: mockSignerClass
      }
    });
    
    redisClientProvider = new RedisClientProvider();
    
    // Mock Date.now for consistent timing tests
    clock = sinon.useFakeTimers();
  });

  afterEach(() => {
    sinon.restore();
    clock.restore();
    delete process.env.REDIS_ENDPOINT;
    delete process.env.AWS_REGION;
    delete process.env.REDIS_SERVER_NAME;
    delete process.env.USER_ID_REDIS;
  });

  describe('constructor', () => {
    it('should initialize with null client and expiration', () => {
      expect(redisClientProvider.client).to.be.null;
      expect(redisClientProvider.expiration).to.be.null;
    });
  });

  describe('getClient', () => {
    it('should create and return a new client when no client exists', async () => {
      const mockClient = { connect: sinon.stub() };
      mockCreateClient.returns(mockClient);

      const result = await redisClientProvider.getClient();

      sinon.assert.calledOnce(mockFromNodeProviderChain);
      sinon.assert.calledOnce(mockSigner.getAuthToken);
      sinon.assert.calledWith(mockCreateClient, {
        url: `rediss://${process.env.REDIS_ENDPOINT}:6379`,
        password: 'mock-auth-token',
        username: process.env.USER_ID_REDIS,
        socket: {
          tls: true,
          rejectUnauthorized: false
        }
      });
      expect(result).to.equal(mockClient);
      expect(redisClientProvider.client).to.equal(mockClient);
    });

    it('should return existing client when not expired and forceRefresh is false', async () => {
      const mockClient = { connect: sinon.stub() };
      redisClientProvider.client = mockClient;
      redisClientProvider.expiration = Date.now() + 10000;

      const result = await redisClientProvider.getClient();

      sinon.assert.notCalled(mockFromNodeProviderChain);
      sinon.assert.notCalled(mockSigner.getAuthToken);
      sinon.assert.notCalled(mockCreateClient);
      expect(result).to.equal(mockClient);
    });

    it('should create new client when existing client is expired', async () => {
      const oldMockClient = { connect: sinon.stub() };
      const newMockClient = { connect: sinon.stub() };
      
      redisClientProvider.client = oldMockClient;
      redisClientProvider.expiration = Date.now() - 1000; // expired
      mockCreateClient.returns(newMockClient);

      const result = await redisClientProvider.getClient();

      sinon.assert.calledOnce(mockFromNodeProviderChain);
      sinon.assert.calledOnce(mockSigner.getAuthToken);
      sinon.assert.calledOnce(mockCreateClient);
      expect(result).to.equal(newMockClient);
      expect(redisClientProvider.client).to.equal(newMockClient);
    });

    it('should create new client when forceRefresh is true', async () => {
      const oldMockClient = { connect: sinon.stub() };
      const newMockClient = { connect: sinon.stub() };
      
      redisClientProvider.client = oldMockClient;
      redisClientProvider.expiration = Date.now() + 10000; // not expired
      mockCreateClient.returns(newMockClient);

      const result = await redisClientProvider.getClient(true);

      sinon.assert.calledOnce(mockFromNodeProviderChain);
      sinon.assert.calledOnce(mockSigner.getAuthToken);
      sinon.assert.calledOnce(mockCreateClient);
      expect(result).to.equal(newMockClient);
    });

    it('should set expiration to AUTHTOKEN_DURATION minus 100 seconds', async () => {
      const mockClient = { connect: sinon.stub() };
      mockCreateClient.returns(mockClient);
      
      const currentTime = 1000000;
      clock.tick(currentTime);

      await redisClientProvider.getClient();

      const expectedExpiration = currentTime + (900 - 100) * 1000;
      expect(redisClientProvider.expiration).to.equal(expectedExpiration);
    });

    it('should create Signer with correct configuration', async () => {
      const mockClient = { connect: sinon.stub() };
      mockCreateClient.returns(mockClient);
      const mockCredentials = { accessKeyId: 'test', secretAccessKey: 'test' };
      mockFromNodeProviderChain.resolves(mockCredentials);

      await redisClientProvider.getClient();

      sinon.assert.calledWith(mockSignerClass, {
        region: process.env.AWS_REGION,
        hostname: process.env.REDIS_SERVER_NAME,
        username: process.env.USER_ID_REDIS,
        credentials: mockCredentials,
        expiresIn: 900
      });
    });

    it('should handle errors when getting credentials', async () => {
      const error = new Error('Credential error');
      mockFromNodeProviderChain.rejects(error);

      try {
        await redisClientProvider.getClient();
        expect.fail('Should have thrown an error');
      } catch (err) {
        expect(err).to.equal(error);
      }
    });

    it('should handle errors when getting auth token', async () => {
      const error = new Error('Auth token error');
      mockSigner.getAuthToken.rejects(error);

      try {
        await redisClientProvider.getClient();
        expect.fail('Should have thrown an error');
      } catch (err) {
        expect(err).to.equal(error);
      }
    });
  });

  describe('invalidateClient', () => {
    it('should set client and expiration to null', () => {
      const mockClient = { connect: sinon.stub() };
      redisClientProvider.client = mockClient;
      redisClientProvider.expiration = Date.now() + 10000;

      redisClientProvider.invalidateClient();

      expect(redisClientProvider.client).to.be.null;
      expect(redisClientProvider.expiration).to.be.null;
    });
  });
});