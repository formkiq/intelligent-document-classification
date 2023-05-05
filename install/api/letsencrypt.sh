#!/bin/bash

echo "Generating Lets Encrypt Certificate"
rm -r -f /etc/letsencrypt/
docker run --rm -v /var/www/certbot/:/var/www/certbot/ -v /etc/letsencrypt/:/etc/letsencrypt/ certbot/certbot certonly --webroot --register-unsafely-without-email --agree-tos --webroot-path=/var/www/certbot/ -d "app.${IP_PUBLIC}.nip.io"

if [ ! -f "${path}/fullchain.pem" ]; then
  echo "Defaulting to Self-signed certificate"
  rm -r -f /etc/letsencrypt/
  mkdir -p "$path"
  openssl req -x509 -nodes -newkey rsa:4096 -days 1000 -keyout "${path}/privkey.pem" -out "${path}/fullchain.pem" -subj "/CN=localhost"
fi
