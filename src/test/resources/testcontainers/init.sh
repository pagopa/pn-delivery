echo "### CREATE QUEUES FIFO ###"
queues_fifo="local-delivery-push-inputs.fifo"

for qn in  $( echo $queues_fifo | tr " " "\n" ) ; do

    echo creating queue fifo $qn ...

    aws --profile default --region us-east-1 --endpoint-url http://localstack:4566 \
        sqs create-queue \
        --attributes '{"DelaySeconds":"2","FifoQueue": "true","ContentBasedDeduplication": "true"}' \
        --queue-name $qn

done

echo "### CREATE QUEUES ###"

aws --profile default --region us-east-1 --endpoint-url http://localstack:4566 \
    sqs create-queue \
    --attributes '{"DelaySeconds":"2"}' \
    --queue-name "local-asseveration-inputs"


echo " - Create pn-delivery TABLES"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name Notifications \
    --attribute-definitions \
        AttributeName=iun,AttributeType=S \
    --key-schema \
        AttributeName=iun,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5 \
    --stream-specification \
        StreamEnabled=true,StreamViewType=NEW_IMAGE


aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name NotificationsMetadata \
    --attribute-definitions \
        AttributeName=iun_recipientId,AttributeType=S \
        AttributeName=sentAt,AttributeType=S \
        AttributeName=senderId_creationMonth,AttributeType=S \
        AttributeName=senderId_recipientId,AttributeType=S \
        AttributeName=recipientId_creationMonth,AttributeType=S \
    --key-schema \
        AttributeName=iun_recipientId,KeyType=HASH \
        AttributeName=sentAt,KeyType=RANGE \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5 \
    --global-secondary-indexes \
    "[
        {
            \"IndexName\": \"senderId\",
            \"KeySchema\": [{\"AttributeName\":\"senderId_creationMonth\",\"KeyType\":\"HASH\"},
                            {\"AttributeName\":\"sentAt\",\"KeyType\":\"RANGE\"}],
            \"Projection\":{
                \"ProjectionType\":\"ALL\"
            },
            \"ProvisionedThroughput\": {
                \"ReadCapacityUnits\": 10,
                \"WriteCapacityUnits\": 5
            }
        },
        {
            \"IndexName\": \"senderId_recipientId\",
            \"KeySchema\": [{\"AttributeName\":\"senderId_recipientId\",\"KeyType\":\"HASH\"},
                            {\"AttributeName\":\"sentAt\",\"KeyType\":\"RANGE\"}],
            \"Projection\":{
                \"ProjectionType\":\"ALL\"
            },
            \"ProvisionedThroughput\": {
                \"ReadCapacityUnits\": 10,
                \"WriteCapacityUnits\": 5
            }
        },
        {
            \"IndexName\": \"recipientId\",
            \"KeySchema\": [{\"AttributeName\":\"recipientId_creationMonth\",\"KeyType\":\"HASH\"},
                            {\"AttributeName\":\"sentAt\",\"KeyType\":\"RANGE\"}],
            \"Projection\":{
                \"ProjectionType\":\"ALL\"
            },
            \"ProvisionedThroughput\": {
                \"ReadCapacityUnits\": 10,
                \"WriteCapacityUnits\": 5
            }
        }
    ]"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name NotificationDelegationMetadata \
    --attribute-definitions \
        AttributeName=iun_recipientId_delegateId_groupId,AttributeType=S \
        AttributeName=sentAt,AttributeType=S \
        AttributeName=delegateId_creationMonth,AttributeType=S \
        AttributeName=delegateId_groupId_creationMonth,AttributeType=S \
        AttributeName=mandateId,AttributeType=S \
    --key-schema \
        AttributeName=iun_recipientId_delegateId_groupId,KeyType=HASH \
        AttributeName=sentAt,KeyType=RANGE \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5 \
    --global-secondary-indexes \
    "[
        {
            \"IndexName\": \"delegateId\",
            \"KeySchema\": [{\"AttributeName\":\"delegateId_creationMonth\",\"KeyType\":\"HASH\"},
                            {\"AttributeName\":\"sentAt\",\"KeyType\":\"RANGE\"}],
            \"Projection\":{
                \"ProjectionType\":\"ALL\"
            },
            \"ProvisionedThroughput\": {
                \"ReadCapacityUnits\": 10,
                \"WriteCapacityUnits\": 5
            }
        },
        {
            \"IndexName\": \"delegateId_groupId\",
            \"KeySchema\": [{\"AttributeName\":\"delegateId_groupId_creationMonth\",\"KeyType\":\"HASH\"},
                            {\"AttributeName\":\"sentAt\",\"KeyType\":\"RANGE\"}],
            \"Projection\":{
                \"ProjectionType\":\"ALL\"
            },
            \"ProvisionedThroughput\": {
                \"ReadCapacityUnits\": 10,
                \"WriteCapacityUnits\": 5
            }
        },
        {
            \"IndexName\": \"mandateId\",
            \"KeySchema\": [{\"AttributeName\":\"mandateId\",\"KeyType\":\"HASH\"}],
            \"Projection\":{
                \"ProjectionType\":\"ALL\"
            },
            \"ProvisionedThroughput\": {
                \"ReadCapacityUnits\": 10,
                \"WriteCapacityUnits\": 5
            }
        }
    ]"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name NotificationsCost \
    --attribute-definitions \
        AttributeName=creditorTaxId_noticeCode,AttributeType=S \
    --key-schema \
        AttributeName=creditorTaxId_noticeCode,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name NotificationsQR \
    --attribute-definitions \
        AttributeName=aarQRCodeValue,AttributeType=S \
        AttributeName=iun,AttributeType=S \
    --key-schema \
        AttributeName=aarQRCodeValue,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5 \
    --global-secondary-indexes \
    "[
        {
            \"IndexName\": \"iun-index\",
            \"KeySchema\": [{\"AttributeName\":\"iun\",\"KeyType\":\"HASH\"}],
            \"Projection\":{
                \"ProjectionType\":\"ALL\"
            },
            \"ProvisionedThroughput\": {
                \"ReadCapacityUnits\": 10,
                \"WriteCapacityUnits\": 5
            }
        }
    ]"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    ssm put-parameter \
    --name "MapTaxIdWhiteList" \
    --type String \
    --value "[
                 {
                     \"taxId\": \"EEEEEE00E00E000A\"
                 },
                 {
                     \"taxId\": \"EEEEEE00E00E000B\"
                 },
                 {
                     \"taxId\": \"EEEEEE00E00E000C\"
                 },
                 {
                     \"taxId\": \"EEEEEE00E00E000D\"
                 },
                 {
                     \"taxId\": \"FRMTTR76M06B715E\"
                 }
             ]"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    ssm put-parameter \
    --name "MapTaxIdBlackList" \
    --type String \
    --value "[
                 {
                     \"taxId\": \"00000000000\"
                 },
                 {
                     \"taxId\": \"PPPPLT80A01H501V\"
                 }
             ]"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name PaNotificationLimit \
    --attribute-definitions \
        AttributeName=pk,AttributeType=S \
        AttributeName=yearMonth,AttributeType=S \
    --key-schema \
        AttributeName=pk,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5 \
    --global-secondary-indexes \
    "[
        {
            \"IndexName\": \"yearMonth-index\",
            \"KeySchema\": [{\"AttributeName\":\"yearMonth\",\"KeyType\":\"HASH\"}],
            \"Projection\":{
                \"ProjectionType\":\"ALL\"
            },
            \"ProvisionedThroughput\": {
                \"ReadCapacityUnits\": 10,
                \"WriteCapacityUnits\": 5
            }
        }
    ]"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name NotificationRefusedVerification \
    --attribute-definitions \
        AttributeName=pk,AttributeType=S \
    --key-schema \
        AttributeName=pk,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5

aws --profile default --region us-east-1 --endpoint-url http://localstack:4566 \
    sqs create-queue \
    --attributes '{"DelaySeconds":"2"}' \
    --queue-name "pn-notification_refused"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    ssm put-parameter \
    --name "PaActiveForPhysicalAddressLookup" \
    --type String \
    --value "[
                 \"5b994d4a-0fa8-47ac-9c7b-354f1d44a1ce\"
             ]"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    ssm put-parameter \
    --name "AARQrUrlConfigs" \
    --type String \
    --value "{\"1.0.0\": {\"directAccessUrlTemplatePhysical\": \"https://cittadini.dev.notifichedigitali.it/\",\"directAccessUrlTemplateLegal\": \"https://imprese.dev.notifichedigitali.it/\",\"quickAccessUrlAarDetailSuffix\": \"?aar\"}}"


echo "Initialization terminated"
