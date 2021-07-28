paId="abc"
paNotificationId="prot1-APIGW"
#baseUrl='http://3.67.133.88:8080'
baseUrl='https://h4hcl6trzf.execute-api.eu-central-1.amazonaws.com/beta/'
apiKeySecret=zewBLfImEB3PEuwwwoxxk8DxJ1TkZxRD7ZpZ5qb4

curl -v -X 'POST' \
  "${baseUrl}/delivery/notifications/sent" \
  -H 'accept: */*' \
  -H "X-PagoPA-PN-PA: ${paId}" \
  -H 'Content-Type: application/json' \
  -H "x-api-key: ${apiKeySecret}" \
  -d '{
  "paNotificationId": "'${paNotificationId}'",
  "subject": "string",
  "cancelledIun": "string",
  "recipients": [
    {
      "fc": "string",
      "digitalDomicile": {
        "type": "PEC",
        "address": "string"
      },
      "physicalAddress": [
        "string"
      ]
    }
  ],
  "documents": [
    {
      "digests": {
        "sha256": "string"
      },
      "contentType": "string",
      "body": "string"
    }
  ],
  "payment": {
    "iuv": "string",
    "notificationFeePolicy": "FLAT_RATE",
    "f24": {
      "flatRate": {
        "digests": {
          "sha256": "string"
        },
        "contentType": "string",
        "body": "string"
      },
      "digital": {
        "digests": {
          "sha256": "string"
        },
        "contentType": "string",
        "body": "string"
      },
      "analog": {
        "digests": {
          "sha256": "string"
        },
        "contentType": "string",
        "body": "string"
      }
    }
  }
}'