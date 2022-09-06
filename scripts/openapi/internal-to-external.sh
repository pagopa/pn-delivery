
cat docs/openapi/api-internal-b2b-pa-v1.yaml \
    | grep -v "# NO EXTERNAL" \
    | sed -e '/# ONLY EXTERNAL/s/^#//' \
    > docs/openapi/api-external-b2b-pa-v1.yaml

cat docs/openapi/api-internal-web-recipient-v1.yaml \
    | grep -v "# NO EXTERNAL" \
    | sed -e '/# ONLY EXTERNAL/s/^#//' \
    > docs/openapi/api-external-web-recipient-v1.yaml