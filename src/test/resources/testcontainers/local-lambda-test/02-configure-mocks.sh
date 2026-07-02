#!/usr/bin/env bash
# Registra su MockServer (porta 1080, servizio pn-localdev) le dipendenze REST
# usate dalla lambda LEGAL (metadataStatusUpdater) di questo repo:
# getRootSenderId e getMandates. Le expectation vengono create via API
# MockServer (effetto immediato, non richiede restart del container) e salvate
# anche come file statico nel repo pn-localdev sibling
# (services/mock_rest_configs/enabled) per essere ricaricate ad ogni avvio.
#
# Uso: ./02-configure-mocks.sh

set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_common.sh"

MOCKSERVER_ENDPOINT="${MOCKSERVER_ENDPOINT:-http://localhost:1080}"
MOCK_CONFIG_DIR="${PN_LOCALDEV_PATH}/services/mock_rest_configs/enabled"
MOCK_CONFIG_FILE="${MOCK_CONFIG_DIR}/40-pn-delivery-lambda-mocks.json"

log "Attendo che MockServer risponda su ${MOCKSERVER_ENDPOINT} ..."
for _ in $(seq 1 30); do
  if curl -sf "${MOCKSERVER_ENDPOINT}/mockserver/status" -X PUT > /dev/null 2>&1; then
    break
  fi
  sleep 1
done

read -r -d '' MOCK_JSON <<EOF || true
[
  {
    "httpRequest": {
      "method": "GET",
      "path": "/ext-registry-private/pa/v1/.*/root-id",
      "matchType": "REGEX"
    },
    "httpResponse": {
      "statusCode": 200,
      "body": { "type": "JSON", "json": { "rootId": "${TEST_ROOT_SENDER_ID}" } }
    }
  },
  {
    "httpRequest": {
      "method": "GET",
      "path": "/mandate-private/api/v1/mandates-by-internaldelegator/.*",
      "matchType": "REGEX"
    },
    "httpResponse": {
      "statusCode": 200,
      "body": { "type": "JSON", "json": [] }
    }
  }
]
EOF

if [[ -d "$PN_LOCALDEV_PATH" ]]; then
  log "Salvo la configurazione statica in ${MOCK_CONFIG_FILE} (persistente ai restart di mockserver) ..."
  mkdir -p "$MOCK_CONFIG_DIR"
  printf '%s\n' "$MOCK_JSON" > "$MOCK_CONFIG_FILE"
else
  log "ATTENZIONE: repo pn-localdev non trovato in ${PN_LOCALDEV_PATH}, salto la persistenza statica (imposta PN_LOCALDEV_PATH se necessario)."
fi

log "Registro le expectation live su MockServer (nessun restart necessario) ..."
curl -sf -X PUT "${MOCKSERVER_ENDPOINT}/mockserver/expectation" \
  -H 'Content-Type: application/json' \
  -d "$MOCK_JSON" > /dev/null

log "Mock REST configurati: getRootSenderId -> rootId=${TEST_ROOT_SENDER_ID}, getMandates -> []"
