#!/bin/bash

set -e

yum -y update

yum -y install docker git

curl -L https://github.com/docker/compose/releases/download/1.22.0/docker-compose-$(uname -s)-$(uname -m) -o /usr/local/bin/docker-compose

chmod +x /usr/local/bin/docker-compose

rm -r -f ~/intelligent-document-classification
git clone https://github.com/formkiq/intelligent-document-classification.git ~/intelligent-document-classification
cd ~/intelligent-document-classification
git checkout --track origin/v1

echo "Enable Docker Service"
systemctl enable docker.service

echo "Starting Docker Service"
systemctl start docker.service

rm -r -f /etc/letsencrypt/
path="/etc/letsencrypt/live/app.${IP_PUBLIC}.nip.io"
mkdir -p "$path"

echo "Generating Self Signed Certificate"
docker-compose -f docker-compose-prod.yml run --rm --entrypoint "\
  openssl req -x509 -nodes -newkey rsa:4096 -days 1000\
    -keyout '$path/privkey.pem' \
    -out '$path/fullchain.pem' \
    -subj '/CN=localhost'" certbot

echo "Building Docker Project"
docker-compose -f docker-compose-prod.yml build --build-arg SERVER_NAME="app.${IP_PUBLIC}.nip.io"

echo "Launching Docker Project"
docker-compose -f docker-compose-prod.yml up -d

echo "Generating Lets Encrypt Certificate"
rm -r -f /etc/letsencrypt/
docker run -it --rm -v /var/www/certbot/:/var/www/certbot/ -v /etc/letsencrypt/:/etc/letsencrypt/ certbot/certbot certonly --webroot --register-unsafely-without-email --agree-tos --webroot-path=/var/www/certbot/ -d "app.${IP_PUBLIC}.nip.io"

if [ $? -eq 0 ] 
then 
  echo "Successfully created domain certificate"
  docker-compose -f docker-compose-prod.yml down 
else 
  docker-compose -f docker-compose-prod.yml down
  echo "Defaulting to Self-signed certificate"
  rm -r -f /etc/letsencrypt/
  mkdir -p "$path"

  docker-compose -f docker-compose-prod.yml run --rm --entrypoint "\
    openssl req -x509 -nodes -newkey rsa:4096 -days 1000\
      -keyout '$path/privkey.pem' \
      -out '$path/fullchain.pem' \
      -subj '/CN=localhost'" certbot
fi

echo "Launching Intelligent Document Classification Application"
docker-compose -f docker-compose-prod.yml up -d