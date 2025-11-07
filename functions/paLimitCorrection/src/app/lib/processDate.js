const dynamo = require("./dynamo.js");
const slaLambda = require('./slaLambda.js');
const cloudWatch = require('./cloudWatch.js');

const processDate = async (dateToVerifyLimit) => {
  console.log('Processing record for dateToVerifyLimit:', dateToVerifyLimit);

  // Recupera l'anno, il mese e il giorno da dateToVerifyLimit
  const year = dateToVerifyLimit.getFullYear();
  const month = ('0' + (dateToVerifyLimit.getMonth() + 1)).slice(-2); // Aggiunge uno zero iniziale se necessario
  const dayOfMonth = dateToVerifyLimit.getDate().toString().padStart(2, '0');
  const yearMonth = `${year}##${month}`;

  const notificationLimit = await dynamo.getNotificationLimit(yearMonth);

  if (notificationLimit.length === 0) {
    console.log('Notification limit NOT found for yearMonth:', yearMonth);
    return;
  }

  //check sulla metrica IteratorAge delle lambda pn-activityStepManagerLambda e pn-slaViolationCheckerLambda
  await checkIteratorAge();

  //chiamata lambda
  const results = await slaLambda.searchSLAViolations(dateToVerifyLimit);
  console.log('SLA Violations filtered:', JSON.stringify(results));
  const numNotificationBlocked = results.length;
  console.log('numNotificationBlocked:', numNotificationBlocked);

  for (const limit of notificationLimit) {
    const keyMetadata = `${limit.paId}##${year}${month}`;
    const notificationMetadata = await dynamo.getNotificationMetadata(keyMetadata, dateToVerifyLimit.toISOString().substring(0, 10));

    const numNotificationSent = notificationMetadata.length;
    console.log(`Number of notifications sent: ${numNotificationSent}`);

    const attributeName = `dailyCounter${dayOfMonth}`;
    const dailyCounterValue = limit[attributeName] !== undefined ? limit[attributeName] : 0;
    console.log(`${attributeName}: ${dailyCounterValue}`);

    const deltaDailyCounter = dailyCounterValue - (numNotificationSent + numNotificationBlocked);
    console.log(`Difference from ${attributeName} (=${dailyCounterValue}) and numNotificationSent (=${numNotificationSent}) + numNotificationBlocked (=${numNotificationBlocked}): ${deltaDailyCounter}`);

    if (deltaDailyCounter > 0) {
      //update notificationLimit
      const keyLimit = `${limit.paId}##${year}##${month}`;
      await updateNotificationLimit(keyLimit, attributeName, dailyCounterValue, deltaDailyCounter);
    } else {
      console.log('Nothing to update: deltaDailyCounter <= 0');
    }
  }

};

async function updateNotificationLimit(keyLimit, attributeName, dailyCounterValue, deltaDailyCounter) {
  console.log("DEBUG_MODE:", process.env.DEBUG_MODE);
  if (process.env.DEBUG_MODE === 'false') {
    await dynamo.updateNotificationLimit(keyLimit, attributeName, dailyCounterValue, deltaDailyCounter);
  } else {
    console.log(`UpdateNotificationLimit skipped for key ${keyLimit}: ${deltaDailyCounter} slot not returned`);
  }
}

async function checkIteratorAge() {
  try {
    const lambdaNames = ['pn-activityStepManagerLambda', 'pn-slaViolationCheckerLambda'];
    let totalMaxIteratorAge = 0;

    for (const lambdaName of lambdaNames) {
      const datapoints = await cloudWatch.getIteratorAgeMetrics(lambdaName);
      if (datapoints.length > 0) {
        const maxIteratorAge = Math.max(...datapoints.map(dp => dp.Maximum));
        console.log(`Max iteratorAge for ${lambdaName}: ${maxIteratorAge}`);
        totalMaxIteratorAge += maxIteratorAge;
      }
    }

    console.log('Total maxIteratorAge:', totalMaxIteratorAge);
    console.log('MAXIMUM_ITERATOR_AGE:', process.env.MAXIMUM_ITERATOR_AGE);
    
    if (totalMaxIteratorAge < process.env.MAXIMUM_ITERATOR_AGE) {
      console.log('Total maxIteratorAge is less than MAXIMUM_ITERATOR_AGE defined. Proceeding with SLA violation search.');
    } else {
      console.error('Total maxIteratorAge exceeds MAXIMUM_ITERATOR_AGE defined. Cannot proceed with SLA violation search.');
      throw new Error('Total maxIteratorAge exceeds MAXIMUM_ITERATOR_AGE defined.');
    }
  } catch (error) {
    console.error('Error:', error);
    throw error;
  }
}

module.exports = { processDate };