
echo ""
echo "#### Launch tests with:"
echo bzt -l /tmp/artifacts/bzt.log "$@" /test/main.yaml

echo ""

echo "#### Tests execution:"
bzt -l /tmp/artifacts/bzt.log "$@" /test/main.yaml


echo "#### Compute data summary"
( cd /tmp/artifacts && python3 /summary.py )

echo "#### Copy outputs"
timestamp=$(date +%Y%m%d-%H%M%S)
cp /tmp/artifacts/error.jtl /minimal_outputs/error-${timestamp}.jtl
cp /tmp/artifacts/kpi.jtl   /minimal_outputs/kpi-${timestamp}.jtl
cp /tmp/artifacts/summary.json   /minimal_outputs/summary-${timestamp}.json
cp /tmp/artifacts/chart.png   /minimal_outputs/summary-chart-${timestamp}.png
ls /minimal_outputs/

echo ""
echo "##########################################"
cat /tmp/artifacts/summary.json

