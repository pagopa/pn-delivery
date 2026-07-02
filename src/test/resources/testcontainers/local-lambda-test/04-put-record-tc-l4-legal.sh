#!/usr/bin/env bash
# TC-L4 (corrisponde a TC-A1): simula un cambio stato su una notifica LEGAL
# (statusChanged=true, communicationType=LEGAL) e verifica che venga scritto
# communicationType=LEGAL su NotificationsMetadata.
#
# Uso: ./04-put-record-tc-l4-legal.sh

set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_common.sh"

WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT
PAYLOAD_FILE="${WORKDIR}/payload.json"

cat > "$PAYLOAD_FILE" <<EOF
{
  "tableName": "pn-Timelines",
  "eventName": "INSERT",
  "dynamodb": {
    "NewImage": {
      "iun": {"S": "${TEST_LEGAL_IUN}"},
      "timelineElementId": {"S": "TL-LEGAL-0001"},
      "communicationType": {"S": "LEGAL"},
      "statusInfo": {"M": {
        "actual": {"S": "ACCEPTED"},
        "statusChangeTimestamp": {"S": "2026-07-01T09:05:00.000Z"},
        "statusChanged": {"BOOL": true}
      }}
    }
  }
}
EOF

log "Invio evento LEGAL (statusChanged=true) per IUN ${TEST_LEGAL_IUN} sullo stream ${KINESIS_STREAM_NAME} ..."
put_kinesis_record "$PAYLOAD_FILE" "tc-l4"

log "Fatto. Verifica dopo qualche secondo con:"
echo "  awslocal dynamodb get-item --table-name ${NOTIFICATIONS_METADATA_TABLE} \\"
echo "    --key '{\"iun_recipientId\":{\"S\":\"${TEST_LEGAL_IUN}##${TEST_LEGAL_RECIPIENT_ID}\"},\"sentAt\":{\"S\":\"${TEST_SENT_AT}\"}}'"
