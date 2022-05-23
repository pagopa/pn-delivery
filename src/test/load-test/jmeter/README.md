# JMeter Test

This are the jmeter tests used to put under stress the application.

To edit file: `jmeter -t src/test/load-test/jmeter/SendNotificationPresigned.jmx &`

To run the test
```bash
jmeter -n -t src/test/load-test/jmeter/SendNotificationPresigned.jmx \
-J loop=10 \
-J numUser=10 \
-J apikey=... 
```

Results are written in _test-result_ directory.

## Parameters

### UDV API Endpoint config
- _protocol_: connection protocol (default _https_)
- _host_: endpoint host for B2B API (default _dev.pn.pagopa.it_)
- _port_: endpoint port for B2B API (default _443_)
- _apikey_: api key B2B API

### UDV General Test Parameter

- _numUser_: number of concurrent user/threads to run (default: _1_)
- _rampUp_: time in seconds to launch all users/threads (default: _1_)
- _loop_: number of loop each user/thread. "infinite" for duration time based test (default: _1_)
- _connTimeout_: http connection timeout (default: _10000_)
- _respTimeout_: http response timeout (default: _30000_)

## Files

### _SendNotificationPresigned.jmx_ 

Using the B2B Api for PA, send notification with attachment preloaded 
via S3 presigned url.

This test reads data from _inputs/notifications_data.csv_ file.

#### UDV Particular Test Parameter
- _doc_path_: path to the document to attach (default: _${configPath}../inputs/multa.pdf_)
- _doc_sha256_: sha256 of the attached document (default: _06e21dbe27ac8e41251a2cfa7003d697c04aea7591ca358c1218071c9ceb3875_)

### 


## Scenario Sender PA1: multiple notification in batch without acceptance check

Simulation of _numUser_ operator sending _loop_ notification.

## Scenario Sender PA2: multiple notification with acceptance check

Simulation of _numUser_ operator sending _loop_ notification.

After each notification sent the user check if the notification
is accepted or reject from the system.

