const { DynamoDBClient, ScanCommand, UpdateItemCommand } = require("@aws-sdk/client-dynamodb");
const { fromIni } = require("@aws-sdk/credential-provider-ini");
const { STSClient, AssumeRoleCommand } = require("@aws-sdk/client-sts");

const arguments = process.argv;

if (arguments.length <= 2) {
  console.error("Specify AWS profile as argument");
  process.exit(1);
}

const awsProfile = arguments[2]
const roleArn = arguments[3]

console.log("Using profile " + awsProfile);

function awsProfileConfig() {
  if(awsProfile.indexOf('sso_')>=0){
    return { 
      region: "eu-south-1", 
      credentials: fromIni({ 
        profile: awsProfile,
      })
    }
  }else{
    return { 
      region: "eu-south-1", 
      credentials: fromIni({ 
        profile: awsProfile,
        roleAssumer: async (sourceCredentials, params) => {
          const stsClient = new STSClient({ credentials: sourceCredentials });
          const command = new AssumeRoleCommand({
            RoleArn: roleArn,
            RoleSessionName: "session1"
          });
          const response = await stsClient.send(command);
          return {
            accessKeyId: response.Credentials.AccessKeyId,
            secretAccessKey: response.Credentials.SecretAccessKey,
            sessionToken: response.Credentials.SessionToken,
            expiration: response.Credentials.Expiration
          };
        }
      })
    }
  }
}

const dynamoDBClient = new DynamoDBClient(awsProfileConfig());
console.log("DOCUMENT CLIENT CREATO");

TABLE_NAME = 'pn-NotificationsMetadata'
SCAN_LIMIT = 2000 // max 2000
DELAY_MS = 250; //250 ms

var index = 1;

const params = {
  TableName: TABLE_NAME,
  Limit: SCAN_LIMIT,
  FilterExpression: 'attribute_not_exists(rootSenderId)',
};

// da utilizzare per ri-cominciare da key specifica
// in caso aggiornare anche il valore iniziale di index
/*const params = {
  TableName: TABLE_NAME,
  Limit: SCAN_LIMIT,
  ExclusiveStartKey: { iun: 'UMZL-MPEZ-GQNY-202211-M-1' }
};*/

async function scanTable(params, callback) {
  const scanCommand = new ScanCommand(params);
  dynamoDBClient.send(scanCommand, function(err, data) {
    if (err) {
      callback(err, null);
    } else {
      setTimeout(function() {
        callback(null, data);

        if (typeof data.LastEvaluatedKey !== 'undefined') {
          params.ExclusiveStartKey = data.LastEvaluatedKey;
          //console.log("Params with LEK: ", params)
          scanTable(params, callback);
        }
      }, DELAY_MS);
    }
  });
}


async function main(){

  console.log('start update item');
  let countItem = 0;
  scanTable(params, function(err, data) {
    if (err) {
      console.log(err);
    } else {
      console.log( "Scanned items: ", data.Items.length )
      data.Items.forEach(async function (item) {
        const key = item.iun_recipientId;
         const sortkey = item.sentAt;
        //console.log("Key: ", key, "at Index: ", index++ );
        const updateExpression = "SET #rootSenderId = :value"
        const updateParams = {
          TableName: TABLE_NAME,
          Key: {
            "iun_recipientId": key,
            "sentAt": sortkey
          },
          UpdateExpression: updateExpression,
          ExpressionAttributeNames: {
            "#rootSenderId": 'rootSenderId'
          },
          ExpressionAttributeValues: {
            ":value": item.senderId
          },
          ConditionExpression: "attribute_not_exists(rootSenderId)"
        }
        try {
          const updateItemCommand = new UpdateItemCommand(updateParams);
          await dynamoDBClient.send(updateItemCommand);
            //console.log("Aggiornato elemento con key: ", key);
            countItem++;
        } catch (error) {
          console.error("Errore nell'aggiornamento dell'elemento:", JSON.stringify(error, null, 2));
          console.error("Errore sull'elemento con key: ", key);
        }
      });
      if(countItem % 1000){
        console.log('ELEMENTI AGGIORNATI ATTUALMENTE = ',countItem);
      }
    }
  })
      
}

main();

