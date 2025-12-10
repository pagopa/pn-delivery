const configLog = {
    logLevel: process.env.LOG_LEVEL
}

class Logger {
    static LOG_LEVELS = {
        DEBUG: 0,
        INFO: 1,
        WARN: 2,
        ERROR: 3
    };

    constructor() {
        this._initializeLogLevel();
    }

    _initializeLogLevel() {
        let logLevel;

        let currentLevel;

        if (configLog && configLog.logLevel) {
            logLevel = configLog.logLevel;
        } else {
            logLevel = 'INFO';
        }

        const normalizedLevel = logLevel.toUpperCase();

        if (!Logger.LOG_LEVELS.hasOwnProperty(normalizedLevel)) {
            console.warn(`Invalid LOG_LEVEL: ${normalizedLevel}. Using INFO as default.`);
            this.currentLevel = Logger.LOG_LEVELS.INFO;
        } else {
            this.currentLevel = Logger.LOG_LEVELS[normalizedLevel];
        }

        console.log(`Logger initialized with level: ${normalizedLevel} (${this.currentLevel})`);
    }

    _shouldLog(level) {
        return Logger.LOG_LEVELS[level] >= this.currentLevel;
    }

    debug(...args) {
        if (this._shouldLog('DEBUG')) {
            console.debug(...args);
        }
    }

    info(...args) {
        if (this._shouldLog('INFO')) {
            console.log(...args);
        }
    }

    warn(...args) {
        if (this._shouldLog('WARN')) {
            console.warn(...args);
        }
    }

    error(...args) {
        if (this._shouldLog('ERROR')) {
            console.error(...args);
        }
    }
}

const logger = new Logger();

module.exports = logger;