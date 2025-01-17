const { Buffer } = require("node:buffer");
const { gunzipSync } = require("node:zlib");

function myGunzip(buffer) {
  return gunzipSync(buffer);
}

function decodePayload(b64Str) {
  const payloadBuf = Buffer.from(b64Str, "base64");

  let parsedJson;
  try {
    parsedJson = JSON.parse(payloadBuf.toString("utf8"));
  } catch (err) {
    const uncompressedBuf = myGunzip(payloadBuf);
    parsedJson = JSON.parse(uncompressedBuf.toString("utf8"));
  }

  return parsedJson;
}

function shouldSkipEvaluation(rec) {
  const allowedEventSources = ["aws:dynamodb"];
  const allowedTables = ["pn-Timelines"];
  return allowedTables.indexOf(rec.tableName) == -1 || allowedEventSources.indexOf(rec.eventSource) == -1 || rec.eventName != "INSERT";
}

function extractYearMonth(date) {
  return date.substring(0,7).replace(/-/g, "");
}

function arrayToString(array) {
  return `[${array.join(",")}]`;
}

module.exports = { decodePayload, shouldSkipEvaluation, extractYearMonth, arrayToString };