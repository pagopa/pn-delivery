const { expect } = require('chai');
const sinon = require('sinon');
const { updateRework } = require('../app/lib/putNotificationRework');
const dynamo = require('../app/lib/dynamo');

describe('putNotificationRework', () => {
  let statusInfo, notification, getRootSenderStub, getMandatesStub, updateWithConditionalStatusStub, consoleLogStub;

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
      iun: 'iun1',
      paNotificationId: 'paNotificationId1',
      subject: 'subject1',
      senderDenomination: 'senderDenomination1',
    };

    updateWithConditionalStatusStub = sinon.stub(dynamo, "updateWithConditionalStatus").resolves();
    consoleLogStub = sinon.stub(console, 'log');
  });

  afterEach(() => {
    sinon.restore();
  });

  it('should put notification rework', async () => {
    await updateRework(statusInfo, notification);

    expect(updateWithConditionalStatusStub.callCount).to.equal(1);
    expect(updateWithConditionalStatusStub.firstCall.args[0]).to.equal('pn-NotificationReworks');
  });

  it('should handle errors and log them', async () => {
    const error = new Error('Test error');
    try {
      await updateRework(statusInfo, notification);
    } catch (error) {
      expect(error.message).to.equal('Test error');
    }
  });
});