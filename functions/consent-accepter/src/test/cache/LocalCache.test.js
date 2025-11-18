const { expect } = require('chai');
const LocalCache = require('../../app/cache/LocalCache');

describe('LocalCache', () => {
  let cache;

  beforeEach(() => {
    cache = new LocalCache();
  });

  describe('constructor', () => {
    it('should initialize with empty Map', () => {
      expect(cache.localCache).to.be.instanceOf(Map);
      expect(cache.localCache.size).to.equal(0);
    });
  });

  describe('set', () => {
    it('should set value with valid expiration', () => {
      const futureTime = Date.now() + 10000;
      const result = cache.set('key1', 'value1', futureTime);
      
      expect(result).to.be.true;
      expect(cache.localCache.get('key1')).to.deep.equal({
        value: 'value1',
        expiresAt: futureTime
      });
    });

    it('should return false for invalid expiration (null)', () => {
      const result = cache.set('key1', 'value1', null);
      expect(result).to.be.false;
    });

    it('should return false for invalid expiration (zero)', () => {
      const result = cache.set('key1', 'value1', 0);
      expect(result).to.be.false;
    });

    it('should return false for invalid expiration (negative)', () => {
      const result = cache.set('key1', 'value1', -1000);
      expect(result).to.be.false;
    });
  });

  describe('get', () => {
    it('should return value for valid unexpired key', () => {
      const futureTime = Date.now() + 10000;
      cache.set('key1', 'value1', futureTime);
      
      const result = cache.get('key1');
      expect(result).to.equal('value1');
    });

    it('should return null for non-existent key', () => {
      const result = cache.get('nonexistent');
      expect(result).to.be.null;
    });

    it('should return null for expired key and remove it from cache', () => {
      const pastTime = Date.now() - 1000;
      cache.localCache.set('key1', { value: 'value1', expiresAt: pastTime });
      
      const result = cache.get('key1');
      expect(result).to.be.null;
      expect(cache.localCache.has('key1')).to.be.false;
    });
  });

  describe('del', () => {
    it('should delete existing key and return true', () => {
      const futureTime = Date.now() + 10000;
      cache.set('key1', 'value1', futureTime);
      
      const result = cache.del('key1');
      expect(result).to.be.true;
      expect(cache.localCache.has('key1')).to.be.false;
    });

    it('should return false for non-existent key', () => {
      const result = cache.del('nonexistent');
      expect(result).to.be.false;
    });
  });

  describe('_isItemExpired', () => {
    it('should return false for unexpired item', () => {
      const futureTime = Date.now() + 10000;
      const entry = { value: 'test', expiresAt: futureTime };
      cache.localCache.set('key1', entry);
      
      const result = cache._isItemExpired('key1');
      expect(result).to.be.false;
    });

    it('should return true for expired item and remove it', () => {
      const pastTime = Date.now() - 1000;
      const entry = { value: 'test', expiresAt: pastTime };
      cache.localCache.set('key1', entry);
      
      const result = cache._isItemExpired('key1');
      expect(result).to.be.true;
      expect(cache.localCache.has('key1')).to.be.false;
    });

    it('should return false for item without expiration', () => {
      const entry = { value: 'test' };
      cache.localCache.set('key1', entry);
      
      const result = cache._isItemExpired('key1');
      expect(result).to.be.false;
    });
  });

  describe('edge cases', () => {
    it('should handle undefined values', () => {
      const futureTime = Date.now() + 10000;
      cache.set('key1', undefined, futureTime);
      
      const result = cache.get('key1');
      expect(result).to.be.undefined;
    });

    it('should handle null values', () => {
      const futureTime = Date.now() + 10000;
      cache.set('key1', null, futureTime);
      
      const result = cache.get('key1');
      expect(result).to.be.null;
    });

    it('should handle object values', () => {
      const futureTime = Date.now() + 10000;
      const obj = { nested: 'value' };
      cache.set('key1', obj, futureTime);
      
      const result = cache.get('key1');
      expect(result).to.deep.equal(obj);
    });
  });
});