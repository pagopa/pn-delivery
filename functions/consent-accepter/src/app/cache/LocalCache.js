class LocalCache {
  constructor() {
    this.localCache = new Map();
  }

  _isItemExpired(entry, key) {
    const now = Date.now();
    if (entry.expiresAt && now > entry.expiresAt) {
      this.localCache.delete(key);
      return true;
    }
    return false;
  }

  get(key) {
    const entry = this.localCache.get(key);

    if (!entry) {
      console.log(`Local Cache miss with key: ${key}`);
      return null;
    }

    if (this._isItemExpired(entry, key)) {
      return null;
    }

    console.log(`Local Cache hit with key: ${key}`);
    return entry.value;
  }

  set(key, value, expiresAtMs) {
    try {
      if (!expiresAtMs || expiresAtMs <= 0) {
        console.warn(
          `Invalid expiration timestamp for key ${key}: ${expiresAtMs  }.`
        );
        return false;
      }

      this.localCache.set(key, {
        value,
        expiresAt: expiresAtMs
      });

      console.log(
        `Value set in local cache with expiration ${new Date(expiresAtMs).toISOString()}: ${key}`
      );

      return true;
    } catch (error) {
      console.error(`Error writing to local cache for key ${key}:`, error);
      return false;
    }
  }
  
  del(key) {
    const existed = this.localCache.delete(key);

    if (existed) {
      console.log(`Key deleted from local cache with key: ${key}`);
    }

    return existed;
  }
}

module.exports = LocalCache;