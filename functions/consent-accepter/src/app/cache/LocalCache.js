class LocalCache {
  constructor() {
    this.localCache = new Map();
  }

  _isItemExpired(key) {
    const now = Date.now();
    const entry = this.localCache.get(key);
    if (entry.expiresAt && now > entry.expiresAt) {
      this.localCache.delete(key);
      return true;
    }
    return false;
  }

  get(key) {
    const entry = this.localCache.get(key);

    if (!entry) {
      console.log(`[LocalCache] miss with key: ${key}`);
      return null;
    }

    if (this._isItemExpired(key)) {
      return null;
    }

    console.log(`[LocalCache] hit with key: ${key}`);
    return entry.value;
  }

  set(key, value, expiresAtMs) {
    try {
      if (!expiresAtMs || expiresAtMs <= 0) {
        console.warn(
          `[LocalCache] Invalid expiration timestamp for key ${key}: ${expiresAtMs  }.`
        );
        return false;
      }

      this.localCache.set(key, {
        value,
        expiresAt: expiresAtMs
      });

      console.log(
        `[LocalCache] Value set in with expiration ${new Date(expiresAtMs).toISOString()}: ${key}`
      );

      return true;
    } catch (error) {
      console.error(`[LocalCache] Error writing for key ${key}:`, error);
      return false;
    }
  }
  
  del(key) {
    const existed = this.localCache.delete(key);

    if (existed) {
      console.log(`[LocalCache] Key deleted with key: ${key}`);
    }

    return existed;
  }
}

module.exports = LocalCache;