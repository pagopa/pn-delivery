const logger = require("../logger");

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
    logger.debug(`[LocalCache] miss with key: ${key}`);
      return null;
    }

    if (this._isItemExpired(key)) {
      return null;
    }

    logger.debug(`[LocalCache] hit with key: ${key}`);
    return entry.value;
  }

  set(key, value, expiresAtMs) {
    try {
      if (!expiresAtMs || expiresAtMs <= 0) {
        logger.warn(
          `[LocalCache] Invalid expiration timestamp for key ${key}: ${expiresAtMs  }.`
        );
        return false;
      }

      this.localCache.set(key, {
        value,
        expiresAt: expiresAtMs
      });

      logger.debug(`[LocalCache] Value set with expiration ${new Date(expiresAtMs).toISOString()}: ${key}`);
      return true;
    } catch (error) {
      logger.error(`[LocalCache] Error writing for key ${key}:`, error);
      return false;
    }
  }
}

module.exports = LocalCache;