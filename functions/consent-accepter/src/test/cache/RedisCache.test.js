const { expect } = require('chai');
const sinon = require('sinon');
const RedisCache = require('../../app/cache/RedisCache');
const RedisClientProvider = require('../../app/cache/RedisClientProvider');

const namespace = 'test_namespace:';
const KEY_PREFIX = "consent::";

const composeRedisKey = (key) => {
  return `${namespace}${KEY_PREFIX}${key}`;
};

describe('RedisCache', () => {
  let redisCache;
  let mockClient;
  let providerGetClientStub;
  let providerInvalidateStub;

  beforeEach(() => {
    process.env.REDIS_NAMESPACE = namespace;

    // Create mock Redis client
    mockClient = {
      connect: sinon.stub().resolves(),
      quit: sinon.stub().resolves(),
      set: sinon.stub().resolves(),
      get: sinon.stub().resolves()
    };

    // Stub the RedisClientProvider constructor
    providerGetClientStub = sinon.stub(RedisClientProvider.prototype, 'getClient').returns(Promise.resolve(mockClient));
    providerInvalidateStub = sinon.stub(RedisClientProvider.prototype, 'invalidateClient').returns();
    redisCache = new RedisCache();
  });

  afterEach(() => {
    sinon.restore();
  });

  describe('connect', () => {
    it('should connect to Redis successfully', async () => {
      await redisCache.connect();

      expect(providerGetClientStub.calledOnce).to.be.true;
      expect(mockClient.connect.calledOnce).to.be.true;
      expect(redisCache.redisClient).to.equal(mockClient);
    });

    it('should skip connection if client is already ready', async () => {
      mockClient.isReady = true;
      redisCache.redisClient = mockClient;

      await redisCache.connect();

      expect(providerGetClientStub.called).to.be.false;
      expect(mockClient.connect.called).to.be.false;
    });

    it('should handle provider getClient errors', async () => {
      const error = new Error('Provider failed');
      providerGetClientStub.rejects(error);

      await redisCache.connect();

      expect(providerGetClientStub.calledOnce).to.be.true;
      expect(mockClient.connect.called).to.be.false;
      expect(providerInvalidateStub.calledOnce).to.be.true;
      expect(redisCache.redisClient).to.be.null;
    });

    it('should handle client connection errors', async () => {
      const error = new Error('Connection failed');
      mockClient.connect.rejects(error);

      await redisCache.connect();

      expect(providerGetClientStub.calledOnce).to.be.true;
      expect(mockClient.connect.calledOnce).to.be.true;
      expect(providerInvalidateStub.calledOnce).to.be.true;
      expect(redisCache.redisClient).to.be.null;
    });

    it('should handle case where client exists but isReady is false', async () => {
      const oldClient = { isReady: false };
      redisCache.redisClient = oldClient;

      await redisCache.connect();

      expect(providerGetClientStub.calledOnce).to.be.true;
      expect(mockClient.connect.calledOnce).to.be.true;
      expect(redisCache.redisClient).to.equal(mockClient);
      expect(redisCache.redisClient).to.not.equal(oldClient);
    });


    it('should not call connect twice on already ready client', async () => {
      mockClient.isReady = true;
      redisCache.redisClient = mockClient;

      await redisCache.connect();
      await redisCache.connect(); // Second call

      expect(providerGetClientStub.called).to.be.false;
      expect(mockClient.connect.called).to.be.false;
    });
  });

  describe('disconnect', () => {
    it('should disconnect successfully when client exists', async () => {
      redisCache.redisClient = mockClient;

      await redisCache.disconnect();

      expect(mockClient.quit.calledOnce).to.be.true;
      expect(redisCache.redisClient).to.be.null;
    });

    it('should handle null client gracefully', async () => {
      redisCache.redisClient = null;

      await redisCache.disconnect();

      expect(mockClient.quit.called).to.be.false;
      expect(redisCache.redisClient).to.be.null;
    });

    it('should handle quit errors and still reset client', async () => {
      const error = new Error('Quit failed');
      mockClient.quit.rejects(error);
      redisCache.redisClient = mockClient;

      await redisCache.disconnect();

      expect(mockClient.quit.calledOnce).to.be.true;
      expect(redisCache.redisClient).to.be.null; // Should be reset even on error
    });

    it('should handle multiple disconnect calls safely', async () => {
      redisCache.redisClient = mockClient;

      await redisCache.disconnect();
      await redisCache.disconnect(); // Second call with null client

      expect(mockClient.quit.calledOnce).to.be.true; // Only called once
      expect(redisCache.redisClient).to.be.null;
    });

  });

  describe('set', () => {
    it('should set value with expiration timestamp', async () => {
      const key = 'testKey';
      const value = { data: 'test' };
      const timestamp = Date.now() + 10000;

      await redisCache.connect();
      const result = await redisCache.set(key, value, timestamp);

      expect(result).to.be.true;
      expect(mockClient.set.calledOnce).to.be.true;
      expect(mockClient.set.calledWith(composeRedisKey(key), JSON.stringify(value), { PXAT: timestamp })).to.be.true;
    });

    it('should set value without expiration timestamp', async () => {
      const key = 'testKey';
      const value = { data: 'test' };

      await redisCache.connect();
      const result = await redisCache.set(key, value);

      expect(result).to.be.true;
      expect(mockClient.set.calledOnce).to.be.true;
      expect(mockClient.set.calledWith(composeRedisKey(key), JSON.stringify(value), {})).to.be.true;
    });

    it('should handle set errors and return false', async () => {
      const error = new Error('Set failed');
      mockClient.set.rejects(error);
      const key = 'testKey';
      const value = { data: 'test' };

      const result = await redisCache.set(key, value);

      expect(result).to.be.false;
    });
  });

  describe('get', () => {
    it('should get and parse existing value', async () => {
      const key = 'testKey';
      const value = { data: 'test' };
      const serializedValue = JSON.stringify(value);
      mockClient.get.resolves(serializedValue);

      await redisCache.connect();
      const result = await redisCache.get(key);

      expect(result).to.deep.equal(value);
      expect(mockClient.get.calledWith(composeRedisKey(key))).to.be.true;
    });

    it('should return null for non-existent key', async () => {
      const key = 'nonExistentKey';
      mockClient.get.resolves(null);

      await redisCache.connect();
      const result = await redisCache.get(key);

      expect(result).to.be.null;
    });

    it('should handle get errors and return null', async () => {
      const error = new Error('Get failed');
      mockClient.get.rejects(error);
      const key = 'testKey';

      const result = await redisCache.get(key);

      expect(result).to.be.null;
    });

    it('should handle JSON parse errors and return null', async () => {
      const key = 'testKey';
      mockClient.get.resolves('invalid json');

      const result = await redisCache.get(key);

      expect(result).to.be.null;
    });

    it('should handle primitive values', async () => {
      const key = 'stringKey';
      const value = 'simple string';
      mockClient.get.resolves(JSON.stringify(value));

      await redisCache.connect();
      const result = await redisCache.get(key);

      expect(result).to.equal(value);
    });

    it('should handle null values correctly', async () => {
      const key = 'nullKey';
      const value = null;
      mockClient.get.resolves(JSON.stringify(value));

      const result = await redisCache.get(key);

      expect(result).to.be.null;
    });
  });
});