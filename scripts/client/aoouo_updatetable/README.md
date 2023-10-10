## AOO/UO DynamoDB Refactor

Installare dipendenze node:
`npm install` 

Eseguire il comando:
`node update_all_notifications_metadata.js <aws-profile>`

Eseguire il comando:
`node update_all_notification_delegation_metadata.js <aws-profile> <role_arn>`

Dove `<aws-profile>` è il profilo dell'account AWS.
`<role_arn>` è necessario solo per utenti che necessitano dell'esecuzione di assume role

Note: 

1) lo script esegue *update_all_notifications_metadata* un aggiornamento massivo della tabella `pn-NotificationsMetadata` quindi si raccomanda di eseguire un backup prima della sua esecuzione.
2) lo script esegue *update_all_notification_delegation_metadata* un aggiornamento massivo della tabella `pn-NotificationsDelegationMetadata` quindi si raccomanda di eseguire un backup prima della sua esecuzione.
3) lo script viene eseguito sempre nella region `eu-south-1` 

