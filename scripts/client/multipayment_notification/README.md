## Multple Payment DynamoDB Refactor

Installare dipendenze node:
`npm install` 

Eseguire il comando:
`node update_all_notification_with_paymentList.js <aws-profile>`

Eseguire il comando:
`node update_all_notification_with_version.js <aws-profile>`

Dove `<aws-profile>` è il profilo dell'account AWS.

Note: 

1) lo script esegue un aggiornamento massivo della tabella `pn-Notifications`quindi si raccomanda di eseguire un backup prima della sua esecuzione.

2) lo script viene eseguito sempre nella region `eu-south-1` 

