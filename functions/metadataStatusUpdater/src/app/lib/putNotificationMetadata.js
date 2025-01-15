const utils = require('./utils');
const RestClient = require('./services');
const dynamo = require('./dynamo');

const putNotificationMetadata = async (statusInfo, notification, acceptedAt) => {
  const rootSenderId = await RestClient.getRootSenderId(notification.senderPaId);

  for (const recipient of notification.recipients) {
    const notificationMetadata = buildNotificationMetadata(statusInfo, notification, acceptedAt, rootSenderId, recipient);
    await dynamo.putMetadata("pn-NotificationsMetadata", notificationMetadata, "iun_recipientId");

    const mandates = await RestClient.getMandates(recipient.recipientId);
    if (!mandates || mandates.length === 0) {
      console.log(`No mandates found for recipient ${recipient.recipientId}`);
      continue;
    }
    await computeDelegationMetadataEntries(notificationMetadata, mandates);
  }
};

const buildNotificationMetadata = (statusInfo, notification, acceptedAt, rootSenderId, recipient) => {
  const recipientId = recipient.recipientId;
  const sentAtMonth = utils.extractYearMonth(notification.sentAt);
  const recipientIds = notification.recipients.map(r => r.recipientId);

  return {
    notificationStatus: statusInfo.actual,
    notificationStatusTimestamp: statusInfo.statusChangeTimestamp,
    senderId: notification.senderPaId,
    rootSenderId: rootSenderId,
    recipientId: recipientId,
    sentAt: notification.sentAt,
    notificationGroup: notification.group,
    recipientIds,
    tableRow: {
      iun: notification.iun,
      recipientsIds: utils.arrayToString(recipientIds),
      paProtocolNumber: notification.paNotificationId,
      subject: notification.subject,
      senderDenomination: notification.senderDenomination,
      acceptedAt: acceptedAt
    },
    senderId_recipientId: `${notification.senderPaId}##${recipientId}`,
    senderId_creationMonth: `${notification.senderPaId}##${sentAtMonth}`,
    recipientId_creationMonth: `${recipientId}##${sentAtMonth}`,
    iun_recipientId: `${notification.iun}##${recipientId}`,
    recipientOne: notification.recipients.indexOf(recipient) === 0
  };
};

const computeDelegationMetadataEntries = async (notificationMetadata, mandates) => {
  for (const mandate of mandates) {
    if (!mandate.visibilityIds || mandate.visibilityIds.length === 0 || mandate.visibilityIds.includes(notificationMetadata.rootSenderId)) {
      const record = buildDelegationMetadataRecord(notificationMetadata, mandate);
      await dynamo.putMetadata('pn-NotificationDelegationMetadata', record, 'iun_recipientId_delegateId_groupId');
    }
  }
};

const buildDelegationMetadataRecord = (notificationMetadata, mandate) => {
  return {
    iun_recipientId_delegateId_groupId: `${notificationMetadata.iun_recipientId}##${mandate.delegate}`,
    sentAt: notificationMetadata.sentAt,
    delegateId_creationMonth: `${mandate.delegate}##${utils.extractYearMonth(notificationMetadata.sentAt)}`,
    mandateId: mandate.mandateId,
    senderId: notificationMetadata.senderId,
    rootSenderId: notificationMetadata.rootSenderId,
    recipientId: notificationMetadata.recipientId,
    recipientIds: notificationMetadata.recipientIds,
    notificationStatus: notificationMetadata.notificationStatus,
    notificationStatusTimestamp: notificationMetadata.notificationStatusTimestamp,
    senderId_creationMonth: notificationMetadata.senderId_creationMonth,
    recipientId_creationMonth: notificationMetadata.recipientId_creationMonth,
    senderId_recipientId: notificationMetadata.senderId_recipientId,
    tableRow: notificationMetadata.tableRow
  };
};

module.exports = { putNotificationMetadata };