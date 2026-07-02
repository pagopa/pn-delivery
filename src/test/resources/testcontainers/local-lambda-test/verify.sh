#!/usr/bin/env bash
# Helper per ispezionare lo stato dopo aver lanciato i put-record-tc-l*.sh:
# legge i record di test da NotificationsMetadata e mostra gli ultimi log
# delle 3 lambda.
#
# Uso: ./verify.sh

set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_common.sh"

echo "### Record LEGAL (${TEST_LEGAL_IUN}##${TEST_LEGAL_RECIPIENT_ID}) ###"
awslocal dynamodb get-item \
  --table-name "$NOTIFICATIONS_METADATA_TABLE" \
  --key "{\"iun_recipientId\":{\"S\":\"${TEST_LEGAL_IUN}##${TEST_LEGAL_RECIPIENT_ID}\"},\"sentAt\":{\"S\":\"${TEST_SENT_AT}\"}}" \
  || echo "(nessun record trovato)"

echo ""
echo "### Record INFORMAL (${TEST_INFORMAL_IUN}##${TEST_INFORMAL_RECIPIENT_ID}) ###"
awslocal dynamodb get-item \
  --table-name "$NOTIFICATIONS_METADATA_TABLE" \
  --key "{\"iun_recipientId\":{\"S\":\"${TEST_INFORMAL_IUN}##${TEST_INFORMAL_RECIPIENT_ID}\"},\"sentAt\":{\"S\":\"${TEST_SENT_AT}\"}}" \
  || echo "(nessun record trovato)"

for fn in "$LEGAL_LAMBDA_NAME" "$INFORMAL_LAMBDA_NAME" "$METADATA_UPDATER_LAMBDA_NAME"; do
  echo ""
  echo "### Ultimi log di ${fn} ###"
  awslocal logs tail "/aws/lambda/${fn}" --since 15m 2>/dev/null \
    || echo "(nessun log group ancora creato per ${fn}, la lambda non e' mai stata invocata?)"
done
