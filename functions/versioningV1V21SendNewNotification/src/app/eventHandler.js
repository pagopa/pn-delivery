const { validateRequest, generateResponse } = require('./requestHelper')

exports.handleEvent = async (event) => {
    console.log('event', event)

    const isRequestValid = validateRequest(event)
    if(isRequestValid.length > 0 ){
        return generateResponse({ resultCode: '404.00', resultDescription: 'Not found', errorList: isRequestValid }, 404, {})
    }

    const eventValidationErrors = validateNewNotification(event.body)
    if(eventValidationErrors.length>0){
        return generateResponse({ resultCode: '400.00', resultDescription: 'Validation error', errorList: eventValidationErrors }, 400, {})
    }

    // creazione oggetto NewNotificationRequestV21
    // mapping tra NewNotificationRequest e NewNotificationRequestV21
    var newNotificationRequestV21 = new createNewNotificationRequesV21(event.body);

    

    // post verso pn-delivery
    const url = process.env.PN_DELIVERY_URL.concat('/requests');
    console.log ('calling ',url);
    return fetch(url, {
        method: "POST",
        body: JSON.stringify(newNotificationRequestV21),
        headers: headers
    })
      .then(response => {
          console.log('risposta da fetch');

          if (response.ok){
            return response.json().then(res => {
                const transformedObject = transformObject(res);
                console.log('ritorno risposta trasformata ',transformedObject);

                const ret = {
                  statusCode: 200,
                  body: JSON.stringify(transformedObject)
                };
                return ret;
              });
          }else{
            console.log('risposta negativa: ', response);
            const err = {
              statusCode: response.status,
              body: response.statusText
            };
            return err;
          }

      });

    // creazione della response


    };


    class NewNotificationRequestV21 {
        constructor() {
          this.idempotenceToken = '';
          this.paProtocolNumber = '';
          this.subject = '';
          this.abstract = '';
          this.recipients = [
            {
              recipientType: '',
              taxId: '',
              denomination: '',
              digitalDomicile: {
                type: '',
                address: '',
              },
              physicalAddress: {
                at: '',
                address: '',
                addressDetails: '',
                zip: '',
                municipality: '',
                municipalityDetails: '',
                province: '',
                foreignState: '',
              },
              payments: [
                {
                  pagoPa: {
                    noticeCode: '',
                    creditorTaxId: '',
                    applyCost: false,
                    attachment: {
                      digests: {
                        sha256: '',
                      },
                      contentType: '',
                      ref: {
                        key: '',
                        versionToken: '',
                      },
                    },
                  },
                  f24: {
                    title: '',
                    applyCost: false,
                    metadataAttachment: {
                      digests: {
                        sha256: '',
                      },
                      contentType: '',
                      ref: {
                        key: '',
                        versionToken: '',
                      },
                    },
                  },
                },
              ],
            },
          ];
          this.documents = [
            {
              digests: {
                sha256: '',
              },
              contentType: '',
              ref: {
                key: '',
                versionToken: '',
              },
              title: '',
              docIdx: '',
            },
          ];
          this.notificationFeePolicy = '';
          this.cancelledIun = '';
          this.physicalCommunicationType = '';
          this.senderDenomination = '';
          this.senderTaxId = '';
          this.group = '';
          this.amount = 0;
          this.paymentExpirationDate = '';
          this.taxonomyCode = '';
          this.paFee = 0;
          this.vat = 0;
          this.pagoPaIntMode = '';
        }
      }
      
      // Esempio di utilizzo del costruttore:
      const emptyNotificationRequest = new NewNotificationRequestV21();
      console.log(emptyNotificationRequest);
      