const { versioning } = require("./src/app/eventHandler.js");

exports.handler = async (event, context) => {
  console.log("Event: ", event);
  return versioning(event, context);
};
