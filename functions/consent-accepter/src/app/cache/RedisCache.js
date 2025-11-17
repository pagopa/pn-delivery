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

  async connectClient() {
    console.log("Connecting to Redis"); 
    let newClient = await this.provider.getClient();
    await newClient.connect();
    console.log("Redis connection OK");
    this.redisClient = newClient;
  }

  async disconnectClient() {
    if (this.redisClient) {
      await this.redisClient.disconnect();
      console.log("Redis disconnected");
      this.redisClient = null;
    }
  } 

  async set(key, value, msTimestamp) {
    try {
      await this.connectClient();

      const serializedValue = JSON.stringify(value);
      const options = {};

      if (msTimestamp) {
        options.PXAT = msTimestamp; // PXAT accetta timestamp in millisecondi
      }

      await this.redisClient.set(this._composeRedisKey(key) , serializedValue, options);
      console.log(`Value set in Redis with expiresAt ${msTimestamp}: ${key}`);
      return true;
    } catch (error) {
      console.error(`Error setting key ${key}:`, error);
      return false;
    } finally {
      await this.disconnectClient();
    } 
  }

  async get(key) {
    try {
      await this.connectClient();
      const serializedValue = await this.redisClient.get(this._composeRedisKey(key));

      if (serializedValue === null) {
        console.log(`Redis Cache miss with key: ${key}`);
        return null;
      }

      console.log(`Redis Cache hit with key: ${key}`);
      return JSON.parse(serializedValue);
    } catch (error) {
      console.error(`Error getting key ${key}:`, error);
      return null;
    } finally {
      await this.disconnectClient();
    }
  } 
}

module.exports = RedisCache;
