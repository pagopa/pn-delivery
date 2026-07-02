#!/usr/bin/env bash
# TC-L5 (corrisponde a TC-B1): simula un cambio stato su una notifica INFORMAL
# con campaignId valorizzato, e verifica scrittura di communicationType=INFORMAL,
# campaignId e chiavi GSI campagna su NotificationsMetadata.
#
# Uso: ./05-put-record-tc-l5-informal.sh

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
      "iun": {"S": "${TEST_INFORMAL_IUN}"},
      "timelineElementId": {"S": "TL-INFORMAL-0001"},
      "communicationType": {"S": "INFORMAL"},
      "statusInfo": {"M": {
        "actual": {"S": "ACCEPTED"},
        "statusChangeTimestamp": {"S": "2026-07-01T09:06:00.000Z"},
        "statusChanged": {"BOOL": true}
      }}
    }
  }
}
EOF

log "Invio evento INFORMAL (statusChanged=true, campaignId=${TEST_CAMPAIGN_ID}) per IUN ${TEST_INFORMAL_IUN} ..."
put_kinesis_record "$PAYLOAD_FILE" "tc-l5"

SENT_AT_MONTH="${TEST_SENT_AT:0:7}"
SENT_AT_MONTH="${SENT_AT_MONTH/-/}"

log "Fatto. Verifica record con:"
echo "  awslocal dynamodb get-item --table-name ${NOTIFICATIONS_METADATA_TABLE} \\"
echo "    --key '{\"iun_recipientId\":{\"S\":\"${TEST_INFORMAL_IUN}##${TEST_INFORMAL_RECIPIENT_ID}\"},\"sentAt\":{\"S\":\"${TEST_SENT_AT}\"}}'"
echo ""
log "Verifica GSI campaignId con:"
echo "  awslocal dynamodb query --table-name ${NOTIFICATIONS_METADATA_TABLE} --index-name campaignId \\"
echo "    --key-condition-expression 'campaignId_creationMonth = :k' \\"
echo "    --expression-attribute-values '{\":k\":{\"S\":\"${TEST_CAMPAIGN_ID}##${SENT_AT_MONTH}\"}}'"
