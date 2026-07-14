const { processRecord } = require("./lib/processRecord");

const eventHandler = async (event) => {
  const batchItemFailures = [];
  const records = event?.Records ?? [];

  console.log(`[metadataStatusUpdater] Starting batch processing: recordCount=${records.length}`);
  for (const record of records) {
    const sequenceNumber = record.kinesis?.sequenceNumber;
    try {
      console.log(`[metadataStatusUpdater] Processing record: sequenceNumber=${sequenceNumber}`);
      await processRecord(record);
      console.log(`[metadataStatusUpdater] Record processed successfully: sequenceNumber=${sequenceNumber}`);
    } catch (error) {
      console.error(`[metadataStatusUpdater] Record processing failed: sequenceNumber=${sequenceNumber}, error=${error.message}`, error.stack);
      batchItemFailures.push({ itemIdentifier: sequenceNumber });
    }
  }

  console.log(`[metadataStatusUpdater] Batch processing completed: recordCount=${records.length}, failureCount=${batchItemFailures.length}`);
  return { batchItemFailures };
};

module.exports = { eventHandler };
