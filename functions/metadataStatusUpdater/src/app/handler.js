const { processRecord } = require('./lib/processRecord');

const eventHandler = async (event) => {
  const batchItemFailures = [];

  console.log(`Processing ${event.Records.length} records...`);
  for (const record of event.Records) {
    try {
      await processRecord(record);
    } catch (error) {
      console.error(`Error processing record: ${error.message}`);
      batchItemFailures.push({ itemIdentifier: record.kinesis.sequenceNumber });
    }
  }

  return { batchItemFailures };
};

module.exports = { eventHandler };