pa_id=123
file_name=$1

curl -X 'POST' \
  'http://localhost:8080/delivery/notifications/sent/' \
  -H 'accept: */*' \
  -H "X-PagoPA-PN-PA: $pa_id" \
  -H 'Content-Type: application/json' \
  --data "@$file_name"