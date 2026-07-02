#!/usr/bin/env bash
# TC-L7 (corrisponde a TC-C5): invia un evento timeline con categoria non
# gestita (fuori mapping), per verificare che metadataUpdater non produca
# alcun aggiornamento su NotificationsMetadata.
#
# Uso: ./07-put-record-tc-l7-unhandled-category.sh

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
      "timelineElementId": {"S": "TL-UNHANDLED-0001"},
      "category": {"S": "SEND_ANALOG_PROGRESS"},
      "details": {"M": {
        "recIndex": {"N": "0"}
      }}
    }
  }
}
EOF

log "Invio evento categoria=SEND_ANALOG_PROGRESS (non gestita) per IUN ${TEST_INFORMAL_IUN} ..."
log "NOTA: se l'event source mapping replica esattamente i FilterCriteria di produzione,"
log "questo evento non arrivera' nemmeno alla lambda (bloccato dal filtro Kinesis)."
put_kinesis_record "$PAYLOAD_FILE" "tc-l7"

log "Fatto. Verifica nei log CloudWatch/console della lambda metadataUpdater che non ci sia stata invocazione"
log "(o, se invocata per test con filtro allargato, che logghi 'Skipping unhandled category')."
