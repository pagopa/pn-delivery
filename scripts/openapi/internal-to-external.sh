
cat docs/openapi/api-internal-b2b-pa.yaml \
    | sed -e '/.*<details no-external>.*/,/<\/details>/ d' \
    | grep -v "# NO EXTERNAL" \
    | sed -e '/# ONLY EXTERNAL/s/^#//' \
    > docs/openapi/api-external-b2b-pa.yaml

cat docs/openapi/api-internal-web-recipient.yaml \
    | sed -e '/.*<details no-external>.*/,/<\/details>/ d' \
    | grep -v "# NO EXTERNAL" \
    | sed -e '/# ONLY EXTERNAL/s/^#//' \
    > docs/openapi/api-external-web-recipient.yaml

cat docs/openapi/appio/api-internal-b2b-appio.yaml \
    | sed -e '/.*<details no-external>.*/,/<\/details>/ d' \
    | grep -v "# NO EXTERNAL" \
    | sed -e '/# ONLY EXTERNAL/s/^#//' \
    > docs/openapi/appio/api-external-b2b-appio.yaml

redocly bundle docs/openapi/api-external-b2b-pa.yaml --output docs/openapi/api-external-b2b-pa-bundle.yaml

#spectral lint -r https://italia.github.io/api-oas-checker/spectral-security.yml docs/openapi/api-external-b2b-pa-bundle.yaml
spectral lint -r https://italia.github.io/api-oas-checker/spectral.yml docs/openapi/api-external-b2b-pa-bundle.yaml

# single files checks
#spectral lint -r https://italia.github.io/api-oas-checker/spectral-security.yml docs/openapi/api-internal-b2b-pa.yaml
#spectral lint -r https://italia.github.io/api-oas-checker/spectral-security.yml docs/openapi/api-internal-web-recipient.yaml
#spectral lint -r https://italia.github.io/api-oas-checker/spectral-security.yml docs/openapi/appio/api-internal-b2b-appio.yaml
