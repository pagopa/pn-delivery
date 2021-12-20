Comando per la creazione dell'immagine docker:
```
docker build -t pn-load-test .
```
Comando per il lancio dell'immagine:
```
docker run --name load-test -v $(pwd)/outputs:/tmp/artifacts --rm pn-load-test \
        -o settings.env.API_KEY=<api-key> \
        -o settings.env.TEST_TIME=10s \
        -o settings.env.SCENARIO=SearchNotificationByReceiver \
         /test/main.yaml
```
Settings:
- API_KEY
- CONCURRENCY ( Default: 2 )
- TEST_TIME ( Default: 1m )
- RAMP_UP ( Default: 0m )
- SCENARIO ( Default: SendNotification )
- INPUT_FILE ( Default: pa_protocol_input.csv )

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