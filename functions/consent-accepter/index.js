const { handle } = require("./src/app/eventHandler.js");

exports.handler = async (event) => {
  return handle(event);
};
