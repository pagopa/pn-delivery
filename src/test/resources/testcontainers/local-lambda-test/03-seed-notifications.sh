#!/usr/bin/env bash
# Semina in Notifications due notifiche di test (LEGAL e INFORMAL) usate
# dagli script put-record-tc-l*.sh per simulare gli eventi Kinesis CDC.
#
# Uso: ./03-seed-notifications.sh

set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_common.sh"

wait_for_localstack
check_prerequisite_infra

log "Semino notifica LEGAL ${TEST_LEGAL_IUN} (recipient ${TEST_LEGAL_RECIPIENT_ID}) ..."
awslocal dynamodb put-item \
  --table-name "$NOTIFICATIONS_TABLE" \
  --item "$(cat <<EOF
{
  "iun": {"S": "${TEST_LEGAL_IUN}"},
  "senderPaId": {"S": "${TEST_SENDER_PA_ID}"},
  "sentAt": {"S": "${TEST_SENT_AT}"},
  "group": {"S": "test-group"},
  "paNotificationId": {"S": "PA-PROT-0001"},
  "subject": {"S": "Notifica di test LEGAL"},
  "senderDenomination": {"S": "Comune di Test"},
  "communicationType": {"S": "LEGAL"},
  "recipients": {"L": [
    {"M": {
      "recipientId": {"S": "${TEST_LEGAL_RECIPIENT_ID}"},
      "payments": {"L": [
        {"M": {
          "creditorTaxId": {"S": "77777777777"},
          "noticeCode": {"S": "123456789012345678"}
        }}
      ]}
    }}
  ]}
}
EOF
)" > /dev/null

log "Semino notifica INFORMAL ${TEST_INFORMAL_IUN} (recipient ${TEST_INFORMAL_RECIPIENT_ID}, campaignId ${TEST_CAMPAIGN_ID}) ..."
awslocal dynamodb put-item \
  --table-name "$NOTIFICATIONS_TABLE" \
  --item "$(cat <<EOF
{
  "iun": {"S": "${TEST_INFORMAL_IUN}"},
  "senderPaId": {"S": "${TEST_SENDER_PA_ID}"},
  "sentAt": {"S": "${TEST_SENT_AT}"},
  "group": {"S": "test-group"},
  "paNotificationId": {"S": "PA-PROT-0002"},
  "subject": {"S": "Notifica di test INFORMAL"},
  "senderDenomination": {"S": "Comune di Test"},
  "communicationType": {"S": "INFORMAL"},
  "campaignId": {"S": "${TEST_CAMPAIGN_ID}"},
  "recipients": {"L": [
    {"M": {
      "recipientId": {"S": "${TEST_INFORMAL_RECIPIENT_ID}"},
      "payments": {"L": []}
    }}
  ]}
}
EOF
)" > /dev/null

log "Seed completato."
