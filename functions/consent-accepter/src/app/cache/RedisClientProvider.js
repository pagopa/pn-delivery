const { createClient } = require("redis");
const { Signer } = require("./Signer");
const { fromNodeProviderChain } = require("@aws-sdk/credential-providers");
const logger = require("../logger");

const AUTHTOKEN_DURATION = 900; //seconds (15 minutes)
const REDIS_URL_CONST = "rediss://" + process.env.REDIS_ENDPOINT + ":6379";

class RedisClientProvider {
  constructor() {
    this.client = null;
    this.expiration = null;
  }

  async getClient(forceRefresh = false) {
    if (
      !forceRefresh &&
      this.client &&
      this.expiration > Date.now()
    ) {
      logger.info("[RedisClientProvider] getClient() - Returning existing valid client");
      return this.client;
    }

    logger.info("[RedisClientProvider] getClient() - Creating new client");
    const credentials = await fromNodeProviderChain()();
    const sign = new Signer({
        region: process.env.AWS_REGION,
        hostname: process.env.REDIS_SERVER_NAME,
        username: process.env.USER_ID_REDIS,
        credentials: credentials,
        expiresIn: AUTHTOKEN_DURATION,
    });
    this.expiration = Date.now() + (AUTHTOKEN_DURATION - 100) * 1000; //seconds

    const presignedUrl = await sign.getAuthToken();

    const redisConfig = {
        url: REDIS_URL_CONST,
        password: presignedUrl,
        username: process.env.USER_ID_REDIS,
        socket: {
            tls: true,
            rejectUnauthorized: false,
        },
    };
    this.client = createClient(redisConfig);
    return this.client;
  }

  invalidateClient() {
    this.client = null;
    this.expiration = null;
  }
}

module.exports = RedisClientProvider;
