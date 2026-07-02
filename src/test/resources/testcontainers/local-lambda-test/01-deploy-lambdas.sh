#!/usr/bin/env bash
# Impacchetta e deploya su LocalStack le 3 lambda di questo repo
# (metadataStatusUpdater, informalMetadataStatusUpdater, metadataUpdater), e
# configura gli Event Source Mapping da Kinesis (pn-cdc-timelines) replicando
# i FilterCriteria definiti in scripts/aws/cfn/microservice.yml.
#
# NOTA: in pn-localdev questo script viene gia' richiamato automaticamente da
# src/test/resources/testcontainers/init.sh (ready hook di LocalStack), che
# scarica il sorgente di questo repo da GitHub per eseguirlo dentro il
# container. Puoi comunque rilanciarlo manualmente da qui (con pn-localdev
# gia' in esecuzione) per ri-deployare le lambda dopo una modifica al codice,
# senza riavviare l'intero ambiente.
#
# Precondizione: pn-localdev in esecuzione con l'init.sh di questo repo gia'
# eseguito (stream Kinesis + tabelle create).
#
# Uso: ./01-deploy-lambdas.sh

set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_common.sh"

wait_for_localstack
check_prerequisite_infra

STREAM_ARN="$(awslocal kinesis describe-stream --stream-name "$KINESIS_STREAM_NAME" \
  --query 'StreamDescription.StreamARN' --output text)"
log "Kinesis stream ARN: ${STREAM_ARN}"

WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT

# Impacchetta una lambda: $1 = nome funzione/cartella in functions/
package_lambda() {
  local fn_name="$1"
  local fn_dir="${PN_DELIVERY_PATH}/functions/${fn_name}"
  local zip_path="${WORKDIR}/${fn_name}.zip"

  if [[ ! -d "$fn_dir" ]]; then
    echo "ERRORE: cartella lambda non trovata: $fn_dir" >&2
    return 1
  fi
  if [[ ! -d "${fn_dir}/node_modules" ]]; then
    log "node_modules assente per ${fn_name}, eseguo npm install ..."
    (cd "$fn_dir" && npm install --no-audit --no-fund)
  fi

  log "Creo zip per ${fn_name} ..."
  (cd "$fn_dir" && zip -q -r "$zip_path" . \
    -x 'src/test/*' -x 'coverage/*' -x '.nyc_output/*' -x '*.md' -x '*.env' -x '.scannerwork/*')
  echo "$zip_path"
}

# Crea o aggiorna una funzione lambda: $1=nome $2=zip_path $3=env_vars_json (es: '{"FOO":"bar"}')
deploy_lambda() {
  local fn_name="$1"
  local zip_path="$2"
  local env_json="$3"
  local env_file="${WORKDIR}/${fn_name}-env.json"

  # AWS CLI in alcuni ambienti (es. container LocalStack) e' sensibile al
  # quoting di --environment: usare file:// evita errori di parsing.
  printf '{"Variables":%s}\n' "$env_json" > "$env_file"

  if awslocal lambda get-function --function-name "$fn_name" > /dev/null 2>&1; then
    log "Funzione '${fn_name}' gia' esistente: aggiorno codice e configurazione."
    awslocal lambda update-function-code \
      --function-name "$fn_name" \
      --zip-file "fileb://${zip_path}" > /dev/null
    awslocal lambda wait function-updated --function-name "$fn_name"
    awslocal lambda update-function-configuration \
      --function-name "$fn_name" \
      --timeout 30 \
      --environment "file://${env_file}" > /dev/null
  else
    log "Creo funzione '${fn_name}' ..."
    awslocal lambda create-function \
      --function-name "$fn_name" \
      --runtime "$LAMBDA_RUNTIME" \
      --handler index.handler \
      --role "$LAMBDA_ROLE_ARN" \
      --timeout 30 \
      --zip-file "fileb://${zip_path}" \
        --environment "file://${env_file}" > /dev/null
  fi
  awslocal lambda wait function-active --function-name "$fn_name"
  log "Funzione '${fn_name}' attiva."
}

# Crea l'event source mapping Kinesis -> lambda con FilterCriteria: $1=nome $2=filter_json_path
create_event_source_mapping() {
  local fn_name="$1"
  local filter_json_path="$2"

  local existing
  existing="$(awslocal lambda list-event-source-mappings --function-name "$fn_name" \
    --event-source-arn "$STREAM_ARN" --query 'EventSourceMappings[0].UUID' --output text 2>/dev/null || true)"

  if [[ -n "$existing" && "$existing" != "None" ]]; then
    log "Event source mapping gia' presente per '${fn_name}' (UUID ${existing}), aggiorno FilterCriteria."
    awslocal lambda update-event-source-mapping \
      --uuid "$existing" \
      --filter-criteria "file://${filter_json_path}" > /dev/null
  else
    log "Creo event source mapping Kinesis -> ${fn_name} ..."
    awslocal lambda create-event-source-mapping \
      --function-name "$fn_name" \
      --event-source-arn "$STREAM_ARN" \
      --batch-size 10 \
      --starting-position TRIM_HORIZON \
      --function-response-types ReportBatchItemFailures \
      --filter-criteria "file://${filter_json_path}" > /dev/null
  fi
}

### metadataStatusUpdater (LEGAL) ###
LEGAL_ENV=$(cat <<EOF
{"REGION":"${AWS_DEFAULT_REGION}","PN_EXTERNAL_REGISTRIES_BASE_URL":"${MOCKSERVER_ENDPOINT_FROM_LAMBDA}","PN_MANDATE_BASE_URL":"${MOCKSERVER_ENDPOINT_FROM_LAMBDA}"}
EOF
)
zip_legal="$(package_lambda "$LEGAL_LAMBDA_NAME")"
deploy_lambda "$LEGAL_LAMBDA_NAME" "$zip_legal" "$LEGAL_ENV"

cat > "${WORKDIR}/filter-legal.json" <<'EOF'
{
  "Filters": [
    { "Pattern": "{ \"data\" : { \"tableName\" : [ \"pn-Timelines\" ], \"dynamodb\" : { \"NewImage\" : { \"statusInfo\": { \"M\": { \"statusChanged\": { \"BOOL\": [ true ]}} }, \"communicationType\": {\"S\": [{ \"exists\": false }, \"LEGAL\"]}}} }}" }
  ]
}
EOF
create_event_source_mapping "$LEGAL_LAMBDA_NAME" "${WORKDIR}/filter-legal.json"

### informalMetadataStatusUpdater (INFORMAL) ###
INFORMAL_ENV=$(cat <<EOF
{"REGION":"${AWS_DEFAULT_REGION}"}
EOF
)
zip_informal="$(package_lambda "$INFORMAL_LAMBDA_NAME")"
deploy_lambda "$INFORMAL_LAMBDA_NAME" "$zip_informal" "$INFORMAL_ENV"

cat > "${WORKDIR}/filter-informal.json" <<'EOF'
{
  "Filters": [
    { "Pattern": "{ \"data\" : { \"tableName\" : [ \"pn-Timelines\" ], \"dynamodb\" : { \"NewImage\" : { \"statusInfo\": { \"M\": { \"statusChanged\": { \"BOOL\": [ true ]}} }, \"communicationType\": {\"S\": [\"INFORMAL\"]}} } }}" }
  ]
}
EOF
create_event_source_mapping "$INFORMAL_LAMBDA_NAME" "${WORKDIR}/filter-informal.json"

### metadataUpdater (viewed/delivered/desiredFeedback) ###
UPDATER_ENV=$(cat <<EOF
{"REGION":"${AWS_DEFAULT_REGION}"}
EOF
)
zip_updater="$(package_lambda "$METADATA_UPDATER_LAMBDA_NAME")"
deploy_lambda "$METADATA_UPDATER_LAMBDA_NAME" "$zip_updater" "$UPDATER_ENV"

cat > "${WORKDIR}/filter-updater.json" <<'EOF'
{
  "Filters": [
    { "Pattern": "{ \"data\" : { \"tableName\" : [ \"pn-Timelines\" ], \"eventName\" : [ \"INSERT\" ], \"dynamodb\" : { \"NewImage\" : { \"category\" : { \"S\": [ \"INFORMAL_NOTIFICATION_VIEWED\" ] } } } } }" },
    { "Pattern": "{ \"data\" : { \"tableName\" : [ \"pn-Timelines\" ], \"eventName\" : [ \"INSERT\" ], \"dynamodb\" : { \"NewImage\" : { \"category\" : { \"S\": [ \"DELIVERED\" ] } } } } }" },
    { "Pattern": "{ \"data\" : { \"tableName\" : [ \"pn-Timelines\" ], \"eventName\" : [ \"INSERT\" ], \"dynamodb\" : { \"NewImage\" : { \"category\" : { \"S\": [ \"WORKFLOW_DONE\" ] } } } } }" }
  ]
}
EOF
create_event_source_mapping "$METADATA_UPDATER_LAMBDA_NAME" "${WORKDIR}/filter-updater.json"

log "Deploy completato. Le 3 lambda sono cablate sullo stream '${KINESIS_STREAM_NAME}'."
