const AWS = require('aws-sdk');
AWS.config.update({profile: 'default', region: 'us-east-1', endpoint: 'http://localhost:4566'});

const docClient = new AWS.DynamoDB.DocumentClient();

const params = {
    TableName: 'Notifications'
};

docClient.scan(params, (err, data) => {
    if (err) {
        console.error("Error during scan:", JSON.stringify(err, null, 2));
    } else {
        //console.log("Scan succeeded:", JSON.stringify(data, null, 2));
        data.Items.forEach(function (item) {
            const key = item.iun;
            console.log("Key: ", key);
            if (item.recipients) {
                const payment = item.recipients?.payment;
                if (payment) {
                    const paymentList = transformPayment(payment);
                    console.log("Payment List: ", JSON.stringify(paymentList, null, 2));
                    const updateParams = {
                        TableName: 'Notifications',
                        Key: {
                            "iun": key
                        },
                        UpdateExpression: "SET #p = :p",
                        ExpressionAttributeNames: {
                            "#p": "payment"
                        },
                        ExpressionAttributeValues: {
                            ":p": paymentList
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
            }
        });
    }
});


function transformPayment(payment) {
    const paymentList = [
      {
        noticeCode: payment.noticeCode,
        creditorTaxId: payment.creditorTaxId,
        pagoPaForm: {
          contentType: payment.pagoPaForm.contentType,
          digests: {
            sha256: payment.pagoPaForm.digests.sha256,
          },
          ref: {
            key: payment.pagoPaForm.ref.key,
            versionToken: payment.pagoPaForm.ref.versionToken,
          },
        },
      },
    ];
  
    if (payment.noticeCodeAlternative) {
      paymentList.push({
        noticeCode: payment.noticeCodeAlternative,
        creditorTaxId: payment.creditorTaxId,
        pagoPaForm: {
          contentType: payment.pagoPaForm.contentType,
          digests: {
            sha256: payment.pagoPaForm.digests.sha256,
          },
          ref: {
            key: payment.pagoPaForm.ref.key,
            versionToken: payment.pagoPaForm.ref.versionToken,
          },
        },
      });
    }
  
    return paymentList;
  }
  