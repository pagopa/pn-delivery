const { handler } = require("./src/app/eventHandler.js");

exports.handler = async (event) => {
  return handler(event);
};
