const { expect } = require('chai');
const sinon = require('sinon');
const { putNotificationMetadata } = require('../app/lib/putNotificationMetadata');
const RestClient = require('../app/lib/services');
const dynamo = require('../app/lib/dynamo');

describe('putNotificationMetadata', () => {
  let statusInfo, notification, acceptedAt, getRootSenderStub, getMandatesStub, putNotificationMetadataRecordStub, putDelegationMetadataRecordStub, consoleLogStub;

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
      communicationType: 'LEGAL',
      iun: 'iun1',
      paNotificationId: 'paNotificationId1',
      subject: 'subject1',
      senderDenomination: 'senderDenomination1',
    };

    getRootSenderStub = sinon.stub(RestClient, 'getRootSenderId').resolves('rootSenderId');
    getMandatesStub = sinon.stub(RestClient, 'getMandates').resolves([{ mandateId: 'mandate1', delegate: 'delegate1' }]);
    putNotificationMetadataRecordStub = sinon.stub(dynamo, "putNotificationMetadataRecord").resolves();
    putDelegationMetadataRecordStub = sinon.stub(dynamo, "putDelegationMetadataRecord").resolves();
    consoleLogStub = sinon.stub(console, 'log');
  });

  afterEach(() => {
    sinon.restore();
  });

  it('should put notification metadata and compute delegation metadata entries', async () => {
    await putNotificationMetadata(statusInfo, notification);

    expect(getRootSenderStub.firstCall.args[0]).to.be.deep.equal('senderPaId');
    expect(putNotificationMetadataRecordStub.callCount).to.equal(1);
    expect(putNotificationMetadataRecordStub.firstCall.args[0]).to.equal('pn-NotificationsMetadata');
    expect(putNotificationMetadataRecordStub.firstCall.args[1]).to.have.property('communicationType', 'LEGAL');
    expect(putDelegationMetadataRecordStub.callCount).to.equal(1);
    expect(putDelegationMetadataRecordStub.firstCall.args[0]).to.equal('pn-NotificationDelegationMetadata');
  });

  it('should include communicationType in notification metadata payload', async () => {
    notification.communicationType = 'LEGAL';
    await putNotificationMetadata(statusInfo, notification);

    const metadataPayload = putNotificationMetadataRecordStub.firstCall.args[1];
    expect(metadataPayload).to.have.property('communicationType', 'LEGAL');
  });

  it('should include undefined communicationType when not set on notification', async () => {
    delete notification.communicationType;
    await putNotificationMetadata(statusInfo, notification);

    const metadataPayload = putNotificationMetadataRecordStub.firstCall.args[1];
    expect(metadataPayload).to.have.property('communicationType', undefined);
  });

  it('should put notification metadata and compute 2 delegation metadata entries', async () => {
    getMandatesStub.resolves([
      { mandateId: 'mandate1', delegate: 'delegate1', visibilityIds: [] }, 
      { mandateId: 'mandate2', delegate: 'delegate2', visibilityIds: ['rootSenderId'] },
      { mandateId: 'mandate3', delegate: 'delegate3', visibilityIds: ['otherSenderId'] } // should not be persisted
    ]);
    await putNotificationMetadata(statusInfo, notification);

    expect(getRootSenderStub.firstCall.args[0]).to.be.deep.equal('senderPaId');
    expect(putNotificationMetadataRecordStub.callCount).to.equal(1);
    expect(putNotificationMetadataRecordStub.firstCall.args[0]).to.equal('pn-NotificationsMetadata');
    expect(putDelegationMetadataRecordStub.callCount).to.equal(2);
    expect(putDelegationMetadataRecordStub.firstCall.args[0]).to.equal('pn-NotificationDelegationMetadata');
  });

  it('should log and return if no mandates are found', async () => {
    RestClient.getMandates.resolves([]);

    await putNotificationMetadata(statusInfo, notification);

    expect(consoleLogStub.secondCall.args[0]).to.equal('No mandates found for recipient recipientId1');
    expect(putNotificationMetadataRecordStub.callCount).to.equal(1);
    expect(putDelegationMetadataRecordStub.callCount).to.equal(0);
  });

  it('should handle errors and log them', async () => {
    const error = new Error('Test error');
    RestClient.getRootSenderId.rejects(error);

    try {
      await putNotificationMetadata(statusInfo, notification);
    } catch (error) {
      expect(error.message).to.equal('Test error');
    }
  });
});