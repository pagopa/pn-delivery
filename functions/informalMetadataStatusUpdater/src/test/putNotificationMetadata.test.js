const { expect } = require('chai');
const sinon = require('sinon');
const { putNotificationMetadata } = require('../app/lib/putNotificationMetadata');
const dynamo = require('../app/lib/dynamo');

describe('putNotificationMetadata', () => {
  let statusInfo, notification, putMetadataStub, consoleLogStub;

  beforeEach(() => {
    statusInfo = {
      actual: 'ACCEPTED',
      statusChangeTimestamp: '2025-01-01T00:00:00Z',
    };

    notification = {
      senderPaId: 'senderPaId',
      recipients: [
        { recipientId: 'recipientId1' },
      ],
      sentAt: '2025-01-01T00:00:00Z',
      group: 'group1',
      communicationType: 'INFORMAL',
      campaignId: 'campaign1',
      iun: 'iun1',
      paNotificationId: 'paNotificationId1',
      subject: 'subject1',
      senderDenomination: 'senderDenomination1',
    };

    putMetadataStub = sinon.stub(dynamo, "putMetadata").resolves();
    consoleLogStub = sinon.stub(console, 'log');
  });

  afterEach(() => {
    sinon.restore();
  });

  it('should put notification metadata for all recipients', async () => {
    await putNotificationMetadata(statusInfo, notification);

    expect(putMetadataStub.callCount).to.equal(1);
    expect(putMetadataStub.firstCall.args[0]).to.equal('pn-NotificationsMetadata');
  });

  it('should include communicationType INFORMAL in notification metadata payload', async () => {
    await putNotificationMetadata(statusInfo, notification);

    const metadataPayload = putMetadataStub.firstCall.args[1];
    expect(metadataPayload).to.have.property('communicationType', 'INFORMAL');
  });

  it('should include campaignId in notification metadata payload', async () => {
    await putNotificationMetadata(statusInfo, notification);

    const metadataPayload = putMetadataStub.firstCall.args[1];
    expect(metadataPayload).to.have.property('campaignId', 'campaign1');
  });

  it('should include undefined campaignId when not set on notification', async () => {
    delete notification.campaignId;
    await putNotificationMetadata(statusInfo, notification);

    const metadataPayload = putMetadataStub.firstCall.args[1];
    expect(metadataPayload).to.have.property('campaignId', undefined);
  });

  it('should put metadata for each recipient', async () => {
    notification.recipients = [
      { recipientId: 'recipientId1' },
      { recipientId: 'recipientId2' },
      { recipientId: 'recipientId3' },
    ];

    await putNotificationMetadata(statusInfo, notification);

    expect(putMetadataStub.callCount).to.equal(3);
  });

  it('should not include rootSenderId in informal metadata payload', async () => {
    await putNotificationMetadata(statusInfo, notification);

    const metadataPayload = putMetadataStub.firstCall.args[1];
    expect(metadataPayload).to.not.have.property('rootSenderId');
  });

  it('should handle errors and log them', async () => {
    const error = new Error('Test error');
    putMetadataStub.rejects(error);

    try {
      await putNotificationMetadata(statusInfo, notification);
    } catch (error) {
      expect(error.message).to.equal('Test error');
    }
  });
});

