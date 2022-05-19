Comando per la creazione dell'immagine docker:
```
docker build -t pn-load-test .
```
Comando per il lancio dell'immagine:
```
docker run --name load-test --rm \ 
        -v $(pwd)/outputs:/tmp/artifacts \
        -v $(pwd)/outputs:/minimal_outputs \
        pn-load-test \
        -o settings.env.BASE_URL=https://dominio/stage/  \
        -o settings.env.API_KEY=<api-key> \
        -o settings.env.TEST_TIME=10s \
        -o settings.env.SCENARIO=SearchNotificationByReceiver \
         /test/main.yaml
```
Settings:
- BASE_URL ( Default: 'http://localhost:8080/' )
- API_KEY
- CONCURRENCY ( Default: 2 )
- TEST_TIME ( Default: 1m )
- RAMP_UP ( Default: 0m )
- SCENARIO ( Default: SendNotification )
- INPUT_FILE ( Default: pa_protocol_input.csv )

Volumi:
- **/tmp/artifacts** contiene tutti i file generati da taurus.  
- **/minimal_outputs** contiene solo l'elenco degli errori e i kpi prodotti da 
  jmeter + un summary contenente alcuni indicatori statistici relativi ai tempi 
  di risposta.   


Scenari:
- SendNotificationScenarios.yaml
  - SendNotification
  - SendNotificationPresigned
- SearchNotificationScenarios.yaml
  - SearchNotificationBySender
  - SearchNotificationByReceiver

Tool per generare uno scenario partendo da un test creato tramite JMeter (.jmx file)
```
jmx2yaml sample.jmx
```