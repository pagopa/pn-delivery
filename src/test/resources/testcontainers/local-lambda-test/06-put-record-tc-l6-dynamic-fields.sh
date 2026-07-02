#!/usr/bin/env bash
# TC-L6 (corrisponde a TC-C1/C2/C3): simula gli eventi timeline che aggiornano
# i campi dinamici (viewed/delivered/desiredFeedback) sul recipient indicato
# da recIndex. Uso: ./06-put-record-tc-l6-dynamic-fields.sh [viewed|delivered|workflow_done|all]

set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_common.sh"

WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT

send_event() {
  local category="$1"
  local timeline_id="$2"
  local payload_file="${WORKDIR}/payload-${category}.json"

  cat > "$payload_file" <<EOF
{
  "tableName": "pn-Timelines",
  "eventName": "INSERT",
  "dynamodb": {
    "NewImage": {
      "iun": {"S": "${TEST_INFORMAL_IUN}"},
      "timelineElementId": {"S": "${timeline_id}"},
      "category": {"S": "${category}"},
      "details": {"M": {
        "recIndex": {"N": "0"}
      }}
    }
  }
}
EOF

  log "Invio evento categoria=${category} per IUN ${TEST_INFORMAL_IUN} recIndex=0 ..."
  put_kinesis_record "$payload_file" "tc-l6-${category}"
}

MODE="${1:-all}"
case "$MODE" in
  viewed)
    send_event "INFORMAL_NOTIFICATION_VIEWED" "TL-VIEWED-0001"
    ;;
  delivered)
    send_event "DELIVERED" "TL-DELIVERED-0001"
    ;;
  workflow_done)
    send_event "WORKFLOW_DONE" "TL-WORKFLOW-0001"
    ;;
  all)
    send_event "INFORMAL_NOTIFICATION_VIEWED" "TL-VIEWED-0001"
    send_event "DELIVERED" "TL-DELIVERED-0001"
    send_event "WORKFLOW_DONE" "TL-WORKFLOW-0001"
    ;;
  *)
    echo "Uso: $0 [viewed|delivered|workflow_done|all]" >&2
    exit 1
    ;;
esac

log "Fatto. Verifica record con:"
echo "  awslocal dynamodb get-item --table-name ${NOTIFICATIONS_METADATA_TABLE} \\"
echo "    --key '{\"iun_recipientId\":{\"S\":\"${TEST_INFORMAL_IUN}##${TEST_INFORMAL_RECIPIENT_ID}\"},\"sentAt\":{\"S\":\"${TEST_SENT_AT}\"}}'"
