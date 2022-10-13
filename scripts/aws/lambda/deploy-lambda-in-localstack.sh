#!/bin/bash
cd "$(dirname "$0")"

endpoint_url='http://localhost:4566'
# recupero l'ARN dello stream della tabella (cambia ogni volta che viene creata)
stream_arn=$(aws --endpoint-url=$endpoint_url dynamodb describe-table --table-name Notifications | grep -o '"LatestStreamArn": "[^"]*' | grep -o '[^"]*$')
# elimino eventuale lambda presente
DEPLOYED_FUNCTIONS=$(aws --endpoint-url=http://localhost:4566 lambda list-functions)

if [[ $DEPLOYED_FUNCTIONS =~ pn-delivery-insert-trigger ]]
then
    aws lambda delete-function --endpoint-url=$endpoint_url --function-name pn-delivery-insert-trigger
else
    echo "pn-delivery-insert-trigger is not currently deployed, no need to delete-function"
fi


# creo la lambda
aws lambda create-function --endpoint-url=$endpoint_url --function-name pn-delivery-insert-trigger --zip-file fileb://./lambda.zip --handler index.handler --environment "Variables={QUEUE_URL=http://localstack:4566/000000000000/local-delivery-push-inputs.fifo,ENV=LOCAL}" --runtime 'nodejs16.x' --role a
# creo lo stream
aws lambda create-event-source-mapping  --endpoint-url=$endpoint_url --function-name pn-delivery-insert-trigger --event-source $stream_arn --batch-size 10 --starting-position TRIM_HORIZON
# elimino il file index.js usato di supporto
rm -f index.js
rm -f lambda.zip


