
const { eventHandler } = require('./src/app/handler');

exports.handler = async (event) => {
  return eventHandler(event);
};