#!/usr/bin/env bash
# TC-L8 (corrisponde a TC-E1): invia un evento INFORMAL_NOTIFICATION_VIEWED
# senza details.recIndex, per verificare che la lambda fallisca in modo
# controllato (errore loggato + record in batchItemFailures per il retry).
#
# Uso: ./08-put-record-tc-l8-missing-recindex.sh

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
      "timelineElementId": {"S": "TL-MISSING-RECINDEX-0001"},
      "category": {"S": "INFORMAL_NOTIFICATION_VIEWED"},
      "details": {"M": {}}
    }
  }
}
EOF

log "Invio evento INFORMAL_NOTIFICATION_VIEWED senza recIndex per IUN ${TEST_INFORMAL_IUN} ..."
put_kinesis_record "$PAYLOAD_FILE" "tc-l8"

log "Fatto. Verifica nei log della lambda metadataUpdater l'errore 'Missing recIndex ...'"
log "e che il record NON venga applicato (nessuna modifica su NotificationsMetadata)."
