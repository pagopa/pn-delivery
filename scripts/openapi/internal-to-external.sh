
cat docs/openapi/api-internal-b2b-pa-v1.yaml \
    | grep -v "# NO EXTERNAL" \
    | sed -e '/# ONLY EXTERNAL/s/^#//' \
    > docs/openapi/api-external-b2b-pa-v1.yaml

cat docs/openapi/api-internal-web-recipient-v1.yaml \
    | grep -v "# NO EXTERNAL" \
    | sed -e '/# ONLY EXTERNAL/s/^#//' \
    > docs/openapi/api-external-web-recipient-v1.yaml

cat docs/openapi/appio/api-internal-b2b-appio-v1.yaml \
    | grep -v "# NO EXTERNAL" \
    | sed -e '/# ONLY EXTERNAL/s/^#//' \
    > docs/openapi/appio/api-external-b2b-appio-v1.yaml

redocly bundle docs/openapi/api-external-b2b-pa-v1.yaml --output docs/openapi/api-external-b2b-pa-bundle.yaml

spectral lint -r https://italia.github.io/api-oas-checker/spectral.yml docs/openapi/api-external-b2b-pa-bundle.yaml