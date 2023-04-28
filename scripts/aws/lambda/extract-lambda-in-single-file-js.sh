#!/bin/bash
cd "$(dirname "$0")"

rm -f index.js
rm -f lambda.zip

# estraggo il contenuto della lambda dal microservice.yml
sed -n -e "/ZipFile: |/,/#ENDpndeliveryinserttrigger/{ /ZipFile: |/d; /#ENDpndeliveryinserttrigger/q; p; }" < ../cfn/microservice.yml | tail -n +1 > index.js


