const { expect } = require('chai');
const sinon = require('sinon');
const CacheManager = require('../../app/cache/CacheManager');
const LocalCache = require('../../app/cache/LocalCache');
const RedisCache = require('../../app/cache/RedisCache');

describe('CacheManager', () => {
  let cacheManager;
  let mockExternalFetcher;
  let localCacheStub;
  let redisCacheStub;

  beforeEach(() => {
    process.env.REDIS_NAMESPACE = "testNamespace::";
    mockExternalFetcher = sinon.stub();
    
    localCacheStub = {
      get: sinon.stub(),
      set: sinon.stub()
    };
    
    redisCacheStub = {
      get: sinon.stub(),
      set: sinon.stub()
    };

    sinon.stub(LocalCache.prototype, 'get').callsFake(localCacheStub.get);
    sinon.stub(LocalCache.prototype, 'set').callsFake(localCacheStub.set);
    sinon.stub(RedisCache.prototype, 'get').callsFake(redisCacheStub.get);
    sinon.stub(RedisCache.prototype, 'set').callsFake(redisCacheStub.set);

    cacheManager = new CacheManager({
      externalFetcher: mockExternalFetcher,
      secondsTTL: 900
    });
  });

  afterEach(() => {
    sinon.restore();
  });

  describe('constructor', () => {
    it('should initialize with default TTL when not provided', () => {
      const manager = new CacheManager({ externalFetcher: () => {} });
      expect(manager.secondsTTL).to.equal(900);
    });

    it('should initialize with custom TTL when provided', () => {
      const customTTL = 1800;
      const manager = new CacheManager({ 
        externalFetcher: () => {}, 
        secondsTTL: customTTL 
      });
      expect(manager.secondsTTL).to.equal(customTTL);
    });

    it('should throw error when externalFetcher is not provided', () => {
      expect(() => new CacheManager()).to.throw('External fetcher not configured');
    });

    it('should initialize LocalCache and RedisCache instances', () => {
      expect(cacheManager.localCache).to.be.instanceOf(LocalCache);
      expect(cacheManager.redisCache).to.be.instanceOf(RedisCache);
    });
  });

  describe('_defaultKeyGenerator', () => {
    it('should generate key with correct format', () => {
      const key = cacheManager._defaultKeyGenerator('PF', 'TOS');
      expect(key).to.equal('PF##TOS');
    });
  });

  describe('get - Local Cache Hit', () => {
    it('should return value from local cache when available', async () => {
      const expectedValue = 'v1.0';
      localCacheStub.get.returns(expectedValue);

      const result = await cacheManager.get('PF', 'TOS');

      expect(result).to.equal(expectedValue);
      expect(localCacheStub.get.calledWith('PF##TOS')).to.be.true;
      expect(redisCacheStub.get.called).to.be.false;
      expect(mockExternalFetcher.called).to.be.false;
    });

    it('should not call Redis or external fetcher on local cache hit', async () => {
      localCacheStub.get.returns('cached_value');

      await cacheManager.get('PF', 'TOS');

      expect(redisCacheStub.get.called).to.be.false;
      expect(mockExternalFetcher.called).to.be.false;
    });
  });

  describe('get - Redis Cache Hit', () => {
    it('should return value from Redis when local cache misses', async () => {
      const redisValue = {
        version: 'v2.0',
        consentType: 'TOS',
        cxType: 'PF',
        expiresAt: Date.now() + 10000
      };
      
      localCacheStub.get.returns(null);
      redisCacheStub.get.resolves(redisValue);
      localCacheStub.set.returns(true);

      const result = await cacheManager.get('PF', 'TOS');

      expect(result).to.equal(redisValue);
      expect(localCacheStub.get.calledWith('PF##TOS')).to.be.true;
      expect(redisCacheStub.get.calledWith('PF##TOS')).to.be.true;
      expect(localCacheStub.set.calledWith('PF##TOS', redisValue, redisValue.expiresAt)).to.be.true;
      expect(mockExternalFetcher.called).to.be.false;
    });

    it('should populate local cache after Redis hit', async () => {
      const redisValue = {
        version: 'v2.0',
        expiresAt: Date.now() + 10000
      };
      
      localCacheStub.get.returns(null);
      redisCacheStub.get.resolves(redisValue);

      await cacheManager.get('PF', 'TOS');

      expect(localCacheStub.set.calledWith('PF##TOS', redisValue, redisValue.expiresAt)).to.be.true;
    });
  });

  describe('get - External Fetcher', () => {
    beforeEach(() => {
      localCacheStub.get.returns(null);
      redisCacheStub.get.resolves(null);
    });

    it('should fetch from external source when both caches miss', async () => {
      const externalValue = 'v3.0';
      mockExternalFetcher.resolves(externalValue);
      localCacheStub.set.returns(true);
      redisCacheStub.set.resolves(true);

      const result = await cacheManager.get('PF', 'TOS');

      expect(result).to.equal(externalValue);
      expect(mockExternalFetcher.calledWith('PF', 'TOS')).to.be.true;
    });

    it('should populate all caches with same value object', async () => {
      const externalValue = 'v3.0';
      mockExternalFetcher.resolves(externalValue);
      localCacheStub.set.returns(false);
      redisCacheStub.set.resolves(false);

      await cacheManager.get('PF', 'TOS');

      const callArgsLocalCache = localCacheStub.set.getCall(0).args;
      expect(callArgsLocalCache[0]).to.equal('PF##TOS');
      expect(callArgsLocalCache[1]).to.deep.include({
        version: externalValue,
        consentType: 'TOS',
        cxType: 'PF'
      });
      expect(callArgsLocalCache[1].expiresAt).to.be.a('number');
      const callArgsRedisCache = redisCacheStub.set.getCall(0).args;
      expect(callArgsRedisCache[0]).to.equal('PF##TOS');
      expect(callArgsRedisCache[1]).to.deep.include({
        version: externalValue,
        consentType: 'TOS',
        cxType: 'PF'
      }); 
      expect(callArgsRedisCache[1].expiresAt).to.be.a('number');
    });

    it('should throw error when external fetcher returns null', async () => {
      mockExternalFetcher.resolves(null);

      try {
        await cacheManager.get('PF', 'TOS');
        expect.fail('Should have thrown an error');
      } catch (error) {
        expect(error.message).to.equal('Value not found for PF_TOS');
      }
    });

    it('should throw error when external fetcher returns undefined', async () => {
      mockExternalFetcher.resolves(undefined);

      try {
        await cacheManager.get('PF', 'TOS');
        expect.fail('Should have thrown an error');
      } catch (error) {
        expect(error.message).to.equal('Value not found for PF_TOS');
      }
    });

    it('should handle external fetcher rejection', async () => {
      const fetchError = new Error('External service unavailable');
      mockExternalFetcher.rejects(fetchError);

      try {
        await cacheManager.get('PF', 'TOS');
        expect.fail('Should have thrown an error');
      } catch (error) {
        expect(error).to.equal(fetchError);
      }
    });
  });

  describe('_getExpiresAt', () => {
    it('should calculate correct expiration timestamp', () => {
      const beforeCall = Date.now();
      const expiresAt = cacheManager._getExpiresAt();
      const afterCall = Date.now();

      const expectedMin = beforeCall + (900 * 1000);
      const expectedMax = afterCall + (900 * 1000);

      expect(expiresAt).to.be.at.least(expectedMin);
      expect(expiresAt).to.be.at.most(expectedMax);
    });
  });
});