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
  let mockProvider;
  let providerStub;

  beforeEach(() => {
    process.env.REDIS_NAMESPACE = namespace;

    // Create mock Redis client
    mockClient = {
      connect: sinon.stub().resolves(),
      disconnect: sinon.stub().resolves(),
      set: sinon.stub().resolves(),
      get: sinon.stub().resolves()
    };

    // Create mock provider
    mockProvider = {
      getClient: sinon.stub().resolves(mockClient)
    };

    // Stub the RedisClientProvider constructor
    providerStub = sinon.stub(RedisClientProvider.prototype, 'getClient').returns(Promise.resolve(mockClient));

    redisCache = new RedisCache();
  });

  afterEach(() => {
    sinon.restore();
  });

  describe('constructor', () => {
    it('should initialize with RedisClientProvider and null redisClient', () => {
      expect(redisCache.provider).to.be.instanceOf(RedisClientProvider);
      expect(redisCache.redisClient).to.be.null;
    });
  });

  describe('connectClient', () => {
    it('should connect to Redis successfully', async () => {
      await redisCache.connectClient();

      expect(providerStub.calledOnce).to.be.true;
      expect(mockClient.connect.calledOnce).to.be.true;
      expect(redisCache.redisClient).to.equal(mockClient);
    });

    it('should handle connection errors', async () => {
      const error = new Error('Connection failed');
      mockClient.connect.rejects(error);

      try {
        await redisCache.connectClient();
        expect.fail('Should have thrown an error');
      } catch (err) {
        expect(err).to.equal(error);
      }
    });
  });

  describe('disconnectClient', () => {
    it('should disconnect when client exists', async () => {
      redisCache.redisClient = mockClient;

      await redisCache.disconnectClient();

      expect(mockClient.disconnect.calledOnce).to.be.true;
      expect(redisCache.redisClient).to.be.null;
    });

    it('should handle null client gracefully', async () => {
      redisCache.redisClient = null;

      await redisCache.disconnectClient();

      expect(mockClient.disconnect.called).to.be.false;
    });

    it('should handle disconnect errors', async () => {
      const error = new Error('Disconnect failed');
      mockClient.disconnect.rejects(error);
      redisCache.redisClient = mockClient;

      try {
        await redisCache.disconnectClient();
        expect.fail('Should have thrown an error');
      } catch (err) {
        expect(err).to.equal(error);
      }
    });
  });

  describe('set', () => {
    it('should set value with expiration timestamp', async () => {
      const key = 'testKey';
      const value = { data: 'test' };
      const timestamp = Date.now() + 10000;

      const result = await redisCache.set(key, value, timestamp);

      expect(result).to.be.true;
      expect(mockClient.set.calledOnce).to.be.true;
      expect(mockClient.set.calledWith(composeRedisKey(key), JSON.stringify(value), { PXAT: timestamp })).to.be.true;
    });

    it('should set value without expiration timestamp', async () => {
      const key = 'testKey';
      const value = { data: 'test' };

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

    it('should always disconnect after set operation', async () => {
      const disconnectSpy = sinon.spy(redisCache, 'disconnectClient');
      
      await redisCache.set('key', 'value');

      expect(disconnectSpy.calledOnce).to.be.true;
    });

    it('should disconnect even when set fails', async () => {
      mockClient.set.rejects(new Error('Set failed'));
      const disconnectSpy = sinon.spy(redisCache, 'disconnectClient');
      
      await redisCache.set('key', 'value');

      expect(disconnectSpy.calledOnce).to.be.true;
    });

    it('should handle complex objects', async () => {
      const key = 'complexKey';
      const value = { nested: { data: 'test' }, array: [1, 2, 3] };

      const result = await redisCache.set(key, value);

      expect(result).to.be.true;
      expect(mockClient.set.calledWith(composeRedisKey(key), JSON.stringify(value), {})).to.be.true;
    });
  });

  describe('get', () => {
    it('should get and parse existing value', async () => {
      const key = 'testKey';
      const value = { data: 'test' };
      const serializedValue = JSON.stringify(value);
      mockClient.get.resolves(serializedValue);

      const result = await redisCache.get(key);

      expect(result).to.deep.equal(value);
      expect(mockClient.get.calledWith(composeRedisKey(key))).to.be.true;
    });

    it('should return null for non-existent key', async () => {
      const key = 'nonExistentKey';
      mockClient.get.resolves(null);

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

    it('should always disconnect after get operation', async () => {
      mockClient.get.resolves(JSON.stringify({ data: 'test' }));
      const disconnectSpy = sinon.spy(redisCache, 'disconnectClient');
      
      await redisCache.get('key');

      expect(disconnectSpy.calledOnce).to.be.true;
    });

    it('should disconnect even when get fails', async () => {
      mockClient.get.rejects(new Error('Get failed'));
      const disconnectSpy = sinon.spy(redisCache, 'disconnectClient');
      
      await redisCache.get('key');

      expect(disconnectSpy.calledOnce).to.be.true;
    });

    it('should handle primitive values', async () => {
      const key = 'stringKey';
      const value = 'simple string';
      mockClient.get.resolves(JSON.stringify(value));

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

  describe('integration scenarios', () => {
    it('should handle connection failure during set', async () => {
      const connectError = new Error('Connection failed');
      mockClient.connect.rejects(connectError);

      const result = await redisCache.set('key', 'value');

      expect(result).to.be.false;
    });

    it('should handle connection failure during get', async () => {
      const connectError = new Error('Connection failed');
      mockClient.connect.rejects(connectError);

      const result = await redisCache.get('key');

      expect(result).to.be.null;
    });

    it('should handle provider getClient failure', async () => {
      const providerError = new Error('Provider failed');
      providerStub.rejects(providerError);

      const result = await redisCache.set('key', 'value');

      expect(result).to.be.false;
    });
  });
});