#!/bin/bash

PROFILE="default"
REGION="us-east-1"
ENDPOINT_URL="http://localhost:4566"
TABLE_NAME="Notifications"

# Retrieve all items from the table
items=$(aws --profile "$PROFILE" --region "$REGION" --endpoint-url="$ENDPOINT_URL" dynamodb scan --table-name "$TABLE_NAME" --attributes-to-get '["iun"]' )
# Loop through each item and update it

for item in $(echo "$items" | jq -c '.Items[]')
do
    # Extract the item's primary key
    key=$(echo "$item" | jq -r '.iun.S')
    # Update the item's attribute values
    aws --profile "$PROFILE" --region "$REGION" --endpoint-url="$ENDPOINT_URL" \
        dynamodb update-item \
        --table-name "$TABLE_NAME" \
        --key "{\"iun\":{\"S\":\"$key\"}}" \
        --update-expression 'SET #na = :nv' \
        --condition-expression 'attribute_not_exists(version)' \
        --expression-attribute-names '{"#na": "version"}' \
        --expression-attribute-values '{":nv": {"N": "1"}}'
done