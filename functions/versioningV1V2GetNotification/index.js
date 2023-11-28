const { versioning } = require("./src/app/eventHandler.js");

exports.handler = async (event, context) => {
  return versioning(event, context);
};
