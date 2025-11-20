const RedisClientProvider = require("./RedisClientProvider");

class RedisCache {
  constructor() {
    this.provider = new RedisClientProvider();
    this.redisClient = null;
    this.REDIS_NAMESPACE = process.env.REDIS_NAMESPACE;
    if(!this.REDIS_NAMESPACE) {
      throw new Error("REDIS_NAMESPACE environment variable is not set");
    }
    this.REDIS_PREFIX = "consent::";
  }

  _composeRedisKey(key) {
    return `${this.REDIS_NAMESPACE}${this.REDIS_PREFIX}${key}`;
  }

  async connect() {
    console.log("[RedisCache] connect() - START");

    if (this.redisClient?.isReady) {
      console.log("[RedisCache] connect() - Already connected, skipping");
      return;
    }

    try {
      const client = await this.provider.getClient();
      await client.connect();
      this.redisClient = client;
      console.log("[RedisCache] connect() - Redis connection OK");
    } catch (error) {
      console.error("[RedisCache] connect() - Error during connection:", error);
      this.provider.invalidateClient();
      this.redisClient = null;
    }
  }

  async disconnect() {
    console.log("[RedisCache] disconnect() - START");
    
    try {
      // Check se c'è un client da disconnettere
      if (!this.redisClient) {
        console.log("[RedisCache] disconnect() - No client to disconnect");
        return;
      }

      await this.redisClient.quit();
      this.redisClient = null;
      console.log("[RedisCache] disconnect() - Quit successful");
      
    } catch (error) {
      console.error("[RedisCache] disconnect() - Error during disconnection:", error);
      // Forza il reset del client anche in caso di errore
      this.redisClient = null;
    }
  }

  async set(key, value, msTimestamp) {
    try {
      const serializedValue = JSON.stringify(value);
      const options = {};

      if (msTimestamp) {
        options.PXAT = msTimestamp; // PXAT accetta timestamp in millisecondi
      }

      await this.redisClient.set(this._composeRedisKey(key) , serializedValue, options);
      console.log(`[RedisCache] Value set in Redis with expiresAt ${msTimestamp}: ${key}`);
      return true;
    } catch (error) {
      console.error(`[RedisCache] Error setting key ${key}:`, error);
      return false;
    }
  }

  async get(key) {
    try {
      const serializedValue = await this.redisClient.get(this._composeRedisKey(key));

      if (serializedValue === null) {
        console.log(`[RedisCache] Redis Cache miss with key: ${key}`);
        return null;
      }

      console.log(`[RedisCache] Redis Cache hit with key: ${key}`);
      return JSON.parse(serializedValue);
    } catch (error) {
      console.error(`[RedisCache] Error getting key ${key}:`, error);
      return null;
    }
  } 
}

module.exports = RedisCache;
