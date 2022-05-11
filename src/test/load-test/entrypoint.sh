export JVM_ARGS="-Dnashorn.args=--no-deprecation-warning"

echo ""
echo "#### Launch tests with:"
echo bzt -l /tmp/artifacts/bzt.log "$@" /test/main.yaml

echo ""

echo "#### Tests execution:"
bzt -l /tmp/artifacts/bzt.log "$@" /test/main.yaml


echo "#### Compute data summary"
( cd /tmp/artifacts && python3 /summary.py )
timestamp=$(date +%Y%m%d-%H%M%S)
echo "#### Copy outputs with timestamp=$timestamp"
cp /tmp/artifacts/error.jtl /minimal_outputs/error-${timestamp}.jtl
cp /tmp/artifacts/kpi.jtl   /minimal_outputs/kpi-${timestamp}.jtl
cp /tmp/artifacts/summary.json   /minimal_outputs/summary-${timestamp}.json
ls /minimal_outputs/

echo ""
echo "##########################################"
cat /tmp/artifacts/summary.json | jq

