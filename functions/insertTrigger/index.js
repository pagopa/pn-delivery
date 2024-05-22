const { handler } = require("./src/app/eventHandler.js");

exports.handler = async (event, context) => {
  return handler(event, context);
};
