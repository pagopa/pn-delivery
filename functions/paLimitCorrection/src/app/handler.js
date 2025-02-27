const { processDate } = require("./lib/processDate");

const eventHandler = async (event) => {
  let dateToVerifyLimit = event.dateToVerifyLimit;
  console.log('event.dateToVerifyLimit:', dateToVerifyLimit);
  const now = new Date();
  const fourDaysAgo = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate() - 4, 0, 0, 0));
  console.log('fourDaysAgo:', fourDaysAgo);

  if (dateToVerifyLimit) {
    const dateToVerify = new Date(dateToVerifyLimit);
    console.log('dateToVerify:', dateToVerify);
    if (dateToVerify > fourDaysAgo) {
      console.log('dateToVerifyLimit must be at least 4 days before the current date.');
      return;
    }
    dateToVerifyLimit = dateToVerify;
  } else {
    console.log('dateToVerifyLimit not provided. Using four days ago as dateToVerifyLimit.');
    dateToVerifyLimit = fourDaysAgo;
  }

  await processDate(dateToVerifyLimit);
};

module.exports = { eventHandler };
