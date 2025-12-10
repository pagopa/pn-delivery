const sinon = require('sinon');
const proxyquire = require('proxyquire');

describe('Logger', () => {
  let Logger, logger;
  let consoleDebug, consoleLog, consoleWarn, consoleError;

  beforeEach(() => {
    consoleDebug = sinon.stub(console, 'debug');
    consoleLog = sinon.stub(console, 'log');
    consoleWarn = sinon.stub(console, 'warn');
    consoleError = sinon.stub(console, 'error');
    delete require.cache[require.resolve('../app/logger')];
    delete process.env.LOG_LEVEL;
    logger = require('../app/logger');
  });

  afterEach(() => {
    sinon.restore();
  });

  it('should log info when level is INFO', () => {
    process.env.LOG_LEVEL = 'INFO';
    logger.info('info message');
    sinon.assert.calledWith(consoleLog, 'info message');
  });

  it('should log debug when level is DEBUG', () => {
    process.env.LOG_LEVEL = 'DEBUG';
    delete require.cache[require.resolve('../app/logger')];
    logger = require('../app/logger');
    logger.debug('debug message');
    sinon.assert.calledWith(consoleDebug, 'debug message');
  });

  it('should log warn when level is WARN', () => {
    process.env.LOG_LEVEL = 'WARN';
    logger.warn('warn message');
    sinon.assert.calledWith(consoleWarn, 'warn message');
  });

  it('should log error when level is ERROR', () => {
    process.env.LOG_LEVEL = 'ERROR';
    logger.error('error message');
    sinon.assert.calledWith(consoleError, 'error message');
  });

  it('should not log debug if level is INFO', () => {
    process.env.LOG_LEVEL = 'INFO';
    logger.debug('debug message');
    sinon.assert.notCalled(consoleDebug);
  });

  it('should default to INFO if invalid log level', () => {
    process.env.LOG_LEVEL = 'INVALID';
    logger.info('info message');
    sinon.assert.calledWith(consoleLog, 'info message');
  });

  it('should warn and set currentLevel to INFO if log level is invalid', () => {
    process.env.LOG_LEVEL = 'INVALID';
    delete require.cache[require.resolve('../app/logger')];
    const logger = require('../app/logger');
    sinon.assert.calledWith(consoleWarn, 'Invalid LOG_LEVEL: INVALID. Using INFO as default.');
    logger.info('test');
    sinon.assert.calledWith(consoleLog, 'test');
  });
});