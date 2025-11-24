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
      return null;
    }

    if (this._isItemExpired(key)) {
      return null;
    }

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

      return true;
    } catch (error) {
      console.error(`[LocalCache] Error writing for key ${key}:`, error);
      return false;
    }
  }
}

module.exports = LocalCache;