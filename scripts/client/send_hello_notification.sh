#!/bin/bash

if ( [ 1 = 1 ] ) then 
  echo "Usage: paId paNotificationId recipientCf recipientDenomination recipientPec attachement"
  exit 1
fi

paId="pa_id"
paNotificationId=$1
#baseUrl='https://h4hcl6trzf.execute-api.eu-central-1.amazonaws.com/beta/'
#apiKeySecret=spikey
filePath=$2

mkdir ./tmp
echo -n "" > ./tmp/file.txt
echo -n '{
  "paNotificationId": "'${paNotificationId}'",
  "subject": "string",
  "cancelledIun": "string",
  "recipients": [
    {
      "taxId": "CGNNMO80A01H501M",
      "denomination": "string",
      "digitalDomicile": {
        "type": "PEC",
        "address": "nome1.cognome1@fail-first.mypec.it"
      },
      "physicalAddress": {
        "at": "string",
        "address": "string",
        "addressDetails": "string",
        "zip": "string",
        "municipality": "string",
        "province": "string"
      }
    }
  ],
  "documents": [
    {
      "digests": {
        "sha256": "loSha256DelFile"
      },
      "contentType": "application/pdf",
      "body": "' >> ./tmp/file.txt
base64 -i $filePath | sed 's/$/"/' >> ./tmp/file.txt
echo -n '
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
}' >> ./tmp/file.txt


curl -v -X 'POST' \
  "${baseUrl}/delivery/notifications/sent" \
  -H 'accept: */*' \
  -H "X-PagoPA-PN-PA: ${paId}" \
  -H 'Content-Type: application/json' \
  -H "x-api-key: ${apiKeySecret}" \
  --data-binary  "@tmp/file.txt"