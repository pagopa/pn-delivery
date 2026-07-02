#!/usr/bin/env bash
# Il deploy delle lambda (01) e la configurazione dei mock REST (02) vengono
# gia' eseguiti automaticamente da src/test/resources/testcontainers/init.sh
# all'avvio di pn-localdev. Questo script si limita a seminare i dati di test
# (03) e a ricordare come lanciare i singoli scenari TC-L4..TC-L8.
# Richiede che pn-localdev (docker compose up) sia gia' in esecuzione.
#
# Uso: ./run-all.sh

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

"${SCRIPT_DIR}/03-seed-notifications.sh"

echo ""
echo "Setup completato. Ora puoi lanciare i singoli scenari di test:"
echo "  ${SCRIPT_DIR}/04-put-record-tc-l4-legal.sh"
echo "  ${SCRIPT_DIR}/05-put-record-tc-l5-informal.sh"
echo "  ${SCRIPT_DIR}/06-put-record-tc-l6-dynamic-fields.sh [viewed|delivered|workflow_done|all]"
echo "  ${SCRIPT_DIR}/07-put-record-tc-l7-unhandled-category.sh"
echo "  ${SCRIPT_DIR}/08-put-record-tc-l8-missing-recindex.sh"
echo ""
echo "Poi verifica lo stato con:"
echo "  ${SCRIPT_DIR}/verify.sh"
