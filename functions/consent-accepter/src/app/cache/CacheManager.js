const LocalCache = require('./LocalCache');
const RedisCache = require('./RedisCache');

const DEFAULT_SECONDS_TTL = 900; // secondi (15 minuti)

/**
 * CacheManager - Orchestrazione centralizzata del sistema di caching multi-livello
 * Gestisce l'interazione tra cache locale, Redis e data source esterno
 */
class CacheManager {
  /**
   * @param {object} config - Configurazione del cache manager
   * @param {number} config.secondsTTL - TTL cache in secondi (default: 900)
   * @param {Function} config.externalFetcher - Funzione per recuperare dati da fonte esterna
   */
  constructor(config = {}) {
    this.secondsTTL = config.secondsTTL || DEFAULT_SECONDS_TTL;
    this.localCache = new LocalCache();
    this.redisCache = new RedisCache();
    this.externalFetcher = config.externalFetcher;
    if (!this.externalFetcher) {
      throw new Error('External fetcher not configured');
    }
    this.keyGenerator = this._defaultKeyGenerator;
  }

  /**
   * Inizializza la connessione alla cache Redis
   * @return {Promise<void>}
   **/  
  async connect() {
    await this.redisCache.connect();
  }

  /**
   * Chiude la connessione alla cache Redis
   * @return {Promise<void>}
   **/    
  async disconnect() {
    await this.redisCache.disconnect();
  } 

  _defaultKeyGenerator(cxType, consentType) {
    return `${cxType}##${consentType}`;
  }

  /**
   * Recupera un valore con strategia multi-livello
   * @param {string} cxType - Tipo contesto (es: PF, PG)
   * @param {string} consentType - Tipo consenso (es: TOS, PRIVACY)
   * @returns {Promise<string>} - versione del consenso
   */
  async get(cxType, consentType) {
    const key = this.keyGenerator(cxType, consentType);
    
    console.log(`[CacheManager] get with cxType: ${cxType}, consentType: ${consentType} (key: ${key})`);
    
    try {
      const localValue = this.localCache.get(key);
      if (localValue !== null) {
        return localValue.version;
      }
      
      
      const redisValue = await this.redisCache.get(key);
      if (redisValue !== null) {
        // Popola cache locale per prossimi accessi
        this.localCache.set(key, redisValue, redisValue.expiresAt);
        
        return redisValue.version;
      }
      
      // LIVELLO 3: External Source
      console.log(`[CacheManager] Cache miss on every level (${key})`);
      const sourceValue = await this.externalFetcher(cxType, consentType);
      
      if (sourceValue === null || sourceValue === undefined) {
        throw new Error(`[CacheManager] Value not found for ${key} from external source`);
      }
      
      // Popola entrambe le cache
      let consentValue = {
        version: sourceValue,
        consentType: consentType,
        cxType: cxType,
        expiresAt: this._getExpiresAt() 
      };
      await this._populateAllCaches(key, consentValue);
      
      return sourceValue;
      
    } catch (error) {
      console.error(`[CacheManager] Error retrieving ${key}:`, error);
      throw error;
    }
  }

  _getExpiresAt() {
    const ttlMs = this.secondsTTL * 1000;
    const expiresAt = Date.now() + ttlMs;
    return expiresAt; 
  }

  /**
   * Popola tutte le cache con un valore
   * @private
   */
  async _populateAllCaches(key, value) {
    this.localCache.set(key, value, value.expiresAt);
    await this.redisCache.set(key, value, value.expiresAt);
  }
  
}

module.exports = CacheManager;