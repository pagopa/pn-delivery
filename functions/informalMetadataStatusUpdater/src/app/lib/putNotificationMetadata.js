const utils = require("./utils");
const dynamo = require("./dynamo");

const putNotificationMetadata = async (
  statusInfo,
  notification
) => {
  for (const recipient of notification.recipients) {
    const notificationMetadata = buildNotificationMetadata(
      statusInfo,
      notification,
      recipient
    );
    await dynamo.putMetadata(
      "pn-NotificationsMetadata",
      notificationMetadata,
      "iun_recipientId"
    );
  }
};

const buildNotificationMetadata = (
  statusInfo,
  notification,
  recipient
) => {
  const recipientId = recipient.recipientId;
  const sentAtMonth = utils.extractYearMonth(notification.sentAt);
  const recipientIds = notification.recipients.map((r) => r.recipientId);

  return {
    notificationStatus: statusInfo.actual,
    notificationStatusTimestamp: statusInfo.statusChangeTimestamp,
    senderId: notification.senderPaId,
    recipientId: recipientId,
    sentAt: notification.sentAt,
    notificationGroup: notification.group,
    communicationType: notification.communicationType,
    campaignId: notification.campaignId,
    recipientIds,
    tableRow: {
      iun: notification.iun,
      recipientsIds: utils.arrayToString(recipientIds),
      paProtocolNumber: notification.paNotificationId,
      subject: notification.subject,
      senderDenomination: notification.senderDenomination
    },
    senderId_recipientId: `${notification.senderPaId}##${recipientId}`,
    senderId_creationMonth: `${notification.senderPaId}##${sentAtMonth}`,
    recipientId_creationMonth: `${recipientId}##${sentAtMonth}`,
    iun_recipientId: `${notification.iun}##${recipientId}`,
    recipientOne: notification.recipients.indexOf(recipient) === 0,
  };
};

module.exports = { putNotificationMetadata };

