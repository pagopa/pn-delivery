#!/usr/bin/env bash
# Configurazione condivisa dagli script di test locale delle lambda pn-delivery
# (metadataStatusUpdater, informalMetadataStatusUpdater, metadataUpdater) su
# LocalStack (stack pn-localdev). Va "sourced", non eseguito direttamente.
#
# Precondizione: pn-localdev (docker compose up) in esecuzione, con l'init.sh
# di questo repo (src/test/resources/testcontainers/init.sh) gia' eseguito dal
# ready hook di LocalStack: crea lo stream Kinesis "pn-cdc-timelines" e le
# tabelle DynamoDB (Notifications, NotificationsMetadata, ...) gia' usate dal
# servizio Java, sulle quali agiscono anche queste lambda.
#
# NOTA: il codice sorgente delle lambda (functions/*) hardcoda i nomi tabella
# con prefisso "pn-" (es. "pn-Notifications"). init_lambda.sh crea queste
# tabelle rieseguendo init.sh con TABLE_PREFIX=pn-, IN AGGIUNTA (non al posto
# di) alle tabelle senza prefisso usate dal servizio Java: le due famiglie di
# tabelle coesistono nello stesso account/region LocalStack senza conflitti.

set -euo pipefail

export AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID:-PN-TEST}"
export AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY:-PN-TEST}"
export AWS_DEFAULT_REGION="${AWS_DEFAULT_REGION:-us-east-1}"

# Endpoint LocalStack visto dall'host (dove girano questi script)
LOCALSTACK_ENDPOINT="${LOCALSTACK_ENDPOINT:-http://localhost:4566}"
# Endpoint LocalStack/MockServer visti dai container Lambda spawnati da LocalStack
# (raggiungibili perche' LAMBDA_DOCKER_NETWORK=pn-develop-network in pn-localdev/docker-compose.yml)
MOCKSERVER_ENDPOINT_FROM_LAMBDA="http://mockserver:1080"

KINESIS_STREAM_NAME="pn-cdc-timelines"

# Nomi tabella CON prefisso "pn-": sono quelli hardcodati nel codice sorgente
# delle lambda (functions/*/src/app/lib/processRecord.js e
# putNotificationMetadata.js), quindi questi script devono leggere/scrivere
# su questi nomi (non su quelli senza prefisso usati dal servizio Java).
NOTIFICATIONS_TABLE="pn-Notifications"
NOTIFICATIONS_METADATA_TABLE="pn-NotificationsMetadata"

LAMBDA_RUNTIME="nodejs22.x"
LAMBDA_ROLE_ARN="arn:aws:iam::000000000000:role/lambda-role"

LEGAL_LAMBDA_NAME="metadataStatusUpdater"
INFORMAL_LAMBDA_NAME="informalMetadataStatusUpdater"
METADATA_UPDATER_LAMBDA_NAME="metadataUpdater"

# Dati di test condivisi da seed e put-record, cosi' da poter verificare
# sempre la stessa entry su NotificationsMetadata (PK iun_recipientId, SK sentAt)
TEST_SENT_AT="2026-07-01T09:00:00.000Z"
TEST_SENDER_PA_ID="5b994d4a-0fa8-47ac-9c7b-354f1d44a1ce"
TEST_ROOT_SENDER_ID="ROOT-5b994d4a"

TEST_LEGAL_IUN="LEGAL-TEST-0001"
TEST_LEGAL_RECIPIENT_ID="RECIPIENT-LEGAL-001"

TEST_INFORMAL_IUN="INFORMAL-TEST-0001"
TEST_INFORMAL_RECIPIENT_ID="RECIPIENT-INFORMAL-001"
TEST_CAMPAIGN_ID="CAMP-001"

# Root di questo repo (functions/* sono qui, relative a questo script).
# Questo script vive in src/test/resources/testcontainers/local-lambda-test/,
# quindi servono 5 livelli per risalire alla root del repo.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PN_DELIVERY_PATH="$(cd "$SCRIPT_DIR/../../../../.." && pwd)"

# Path del repo pn-localdev (sibling di questo repo), usato solo per scrivere
# la configurazione statica di MockServer in services/mock_rest_configs.
PROJECT_ROOT="$(cd "$PN_DELIVERY_PATH/.." && pwd)"
PN_LOCALDEV_PATH="${PN_LOCALDEV_PATH:-$PROJECT_ROOT/pn-localdev}"

awslocal() {
  aws --endpoint-url="$LOCALSTACK_ENDPOINT" "$@"
}

log() {
  # Scrive su stderr (non stdout): alcune funzioni chiamanti (es.
  # package_lambda) vengono invocate con "$(...)" per catturarne il valore di
  # ritorno su stdout, quindi log() non deve inquinare quel canale.
  echo "[local-lambda-test] $*" >&2
}

# Pubblica un record Kinesis a partire da un file JSON contenente il payload
# CDC ({"tableName":..., "eventName":..., "dynamodb": {"NewImage": {...}}}),
# nello stesso formato che le lambda si aspettano di decodificare da
# record.kinesis.data (vedi utils.decodePayload nelle lambda).
put_kinesis_record() {
  local payload_file="$1"
  local partition_key="${2:-local-test}"
  awslocal kinesis put-record \
    --stream-name "$KINESIS_STREAM_NAME" \
    --partition-key "$partition_key" \
    --cli-binary-format raw-in-base64-out \
    --data "fileb://${payload_file}"
}

# Attende che LocalStack risponda su /_localstack/health
wait_for_localstack() {
  log "Attendo che LocalStack sia pronto su ${LOCALSTACK_ENDPOINT} ..."
  for _ in $(seq 1 60); do
    if curl -sf "${LOCALSTACK_ENDPOINT}/_localstack/health" > /dev/null 2>&1; then
      log "LocalStack pronto."
      return 0
    fi
    sleep 2
  done
  echo "[local-lambda-test] ERRORE: LocalStack non risponde dopo 120s" >&2
  return 1
}

# Verifica che lo stream Kinesis e le tabelle esistano gia' (create
# dall'init.sh di questo repo eseguito dal ready hook di LocalStack). Se
# mancano, l'utente deve far ripartire pn-localdev (o eseguire manualmente
# src/test/resources/testcontainers/init.sh) prima di procedere.
check_prerequisite_infra() {
  if ! awslocal kinesis describe-stream --stream-name "$KINESIS_STREAM_NAME" > /dev/null 2>&1; then
    echo "[local-lambda-test] ERRORE: stream Kinesis '${KINESIS_STREAM_NAME}' non trovato." >&2
    echo "Assicurati che pn-localdev sia stato avviato (docker compose up) e che" >&2
    echo "src/test/resources/testcontainers/init.sh sia stato eseguito dal ready hook di LocalStack." >&2
    return 1
  fi
  if ! awslocal dynamodb describe-table --table-name "$NOTIFICATIONS_TABLE" > /dev/null 2>&1; then
    echo "[local-lambda-test] ERRORE: tabella '${NOTIFICATIONS_TABLE}' non trovata." >&2
    return 1
  fi
  if ! awslocal dynamodb describe-table --table-name "$NOTIFICATIONS_METADATA_TABLE" > /dev/null 2>&1; then
    echo "[local-lambda-test] ERRORE: tabella '${NOTIFICATIONS_METADATA_TABLE}' non trovata." >&2
    return 1
  fi
}
