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
                var recipientIdx = 0;
                item.recipients.forEach(function (recipient) {
                    const payment = recipient.payment;
                    console.log("Payment: ", payment);
                    if (payment) {
                        const paymentList = transformPayment(payment);
                        console.log("Payment List: ", JSON.stringify(paymentList, null, 2));
                        const updateExpression = "SET #rec[" + recipientIdx + "].#payList = :paymentList REMOVE #rec[" + recipientIdx + "].#pay"
                        const updateParams = {
                            TableName: 'Notifications',
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
});

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
