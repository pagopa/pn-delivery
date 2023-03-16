const AWS = require('aws-sdk');
var credentials = new AWS.SharedIniFileCredentials({profile: 'default'});
AWS.config.credentials = credentials;
AWS.config.update({region: 'us-east-1', endpoint: 'http://localhost:4566'});

const docClient = new AWS.DynamoDB.DocumentClient();

TABLE_NAME = 'Notifications'
SCAN_LIMIT = 1000

const params = {
  TableName: TABLE_NAME,
  Limit: SCAN_LIMIT
};

function scanTable(params, callback) {
  docClient.scan(params, function(err, data) {
    if (err) {
      callback(err, null);
    } else {
      callback(null, data);
      
      if (typeof data.LastEvaluatedKey !== 'undefined') {
        params.ExclusiveStartKey = data.LastEvaluatedKey;
        scanTable(params, callback);
      }
    }
  });
}

scanTable(params, function(err, data) {
  if (err) {
    console.log(err);
  } else {
    data.Items.forEach(function (item) {
      const key = item.iun;
      console.log("Key: ", key);
      if (item.recipients) {
        var recipientIdx = 0;
        item.recipients.forEach(function (recipient) {
          const payment = recipient.payment;
          console.log("Payment: ", payment);
          if (payment) {
            const paymentList = transformPayment(payment);
            console.log("Payment List: ", JSON.stringify(paymentList, null, 2));
            const updateExpression = "SET #rec[" + recipientIdx + "].#payList = :paymentList REMOVE #rec[" + recipientIdx + "].#pay"
            const updateParams = {
              TableName: TABLE_NAME,
              Key: {
                "iun": key
              },
              UpdateExpression: updateExpression,
              ExpressionAttributeNames: {
                "#rec": 'recipients',
                "#payList": 'paymentList',
                "#pay": 'payment'
              },
              ExpressionAttributeValues: {
                ":paymentList": paymentList
              }
            }
            docClient.update(updateParams, (err, data) => {
              if (err) {
                console.error("Errore nell'aggiornamento dell'elemento:", JSON.stringify(err, null, 2));
              } else {
                console.log("Elemento aggiornato con successo:", JSON.stringify(data, null, 2));
              }
            })
          }
          recipientIdx++;
        });     
      }
    });
  }
})

function transformPayment(payment) {
  const paymentList = [
    {
      noticeCode: payment.noticeCode,
      creditorTaxId: payment.creditorTaxId
    }
  ];
  
  if (payment.noticeCodeAlternative) {
    paymentList.push({
      noticeCode: payment.noticeCodeAlternative,
      creditorTaxId: payment.creditorTaxId
    });
  }
  
  if (payment.pagoPaForm) {
    return paymentList.map((item) => ({
      ...item,
      pagoPaForm: {
        contentType: payment.pagoPaForm.contentType,
        digests: {
          sha256: payment.pagoPaForm.digests.sha256,
        },
        ref: {
          key: payment.pagoPaForm.ref.key,
          versionToken: payment.pagoPaForm.ref.versionToken,
        },
      }
    }));
  }
  return paymentList;
}
