const dynamo = require("./dynamo");

const updateRework = async (
  timelineElement,
  expectedState,
  newState,
  flagInvalidatedIds
) => {
  await dynamo.updateWithConditionalStatus(
    "pn-NotificationReworks",
    flagInvalidatedIds ? buildUpdateReworkWithInvalidatedIds(timelineElement, newState): buildUpdateRework(timelineElement, newState),
    "iun_reworkId",
    expectedState
  );
};

const buildUpdateRework = (
  timelineElement,
  newState
) => {
  return {
    iun:   timelineElement.iun,
    reworkId:   timelineElement.reworkId,
    status: newState,
    updatedAt: new Date()
  };
};

const buildUpdateReworkWithInvalidatedIds = (
  timelineElement,
  newState
) => {
  return {
    iun:   timelineElement.iun,
    reworkId:   timelineElement.reworkId,
    flagInvalidatedIds: timelineElement.invalidatedTimelineElementIds,
    status: newState,
    updatedAt: new Date()
  };
};

module.exports = { updateRework };