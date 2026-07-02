#!/usr/bin/env bash
# Setup di TUTTO cio' che riguarda le lambda di pn-delivery (metadataStatusUpdater,
# informalMetadataStatusUpdater, metadataUpdater) su LocalStack: stream Kinesis CDC,
# deploy delle lambda (packaging + create-function + event source mapping) e
# configurazione dei mock REST usati dalla lambda LEGAL.
#
# Viene invocato da init.sh SOLO quando gira nell'ambiente completo pn-localdev
# (rilevato tramite la variabile LAMBDA_DOCKER_NETWORK, assente nei test di
# integrazione Java), dopo aver scaricato il sorgente di questo repo da GitHub
# (init.sh, eseguito dentro il container, non ha accesso al checkout locale).
#
# Uso: bash init_lambda.sh (eseguito da init.sh dalla root del checkout scaricato)

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "### CREATE KINESIS CDC STREAM (for functions/* lambdas: metadataStatusUpdater, informalMetadataStatusUpdater, metadataUpdater) ###"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    kinesis create-stream \
    --stream-name pn-cdc-timelines \
    --shard-count 1

echo "### DEPLOY LAMBDA (metadataStatusUpdater, informalMetadataStatusUpdater, metadataUpdater) ###"

export MOCKSERVER_ENDPOINT="${MOCKSERVER_ENDPOINT:-http://mockserver:1080}"

bash "${SCRIPT_DIR}/local-lambda-test/01-deploy-lambdas.sh"
bash "${SCRIPT_DIR}/local-lambda-test/02-configure-mocks.sh"

echo "### LAMBDA SETUP TERMINATED ###"
