#!/bin/bash

# https://maikroservice.com/how-to-install-elasticsearch-kibana-and-winlogbeat-in-your-cloudlab-the-lazy-way

sudo yum -y update

sudo yum -y install java-openjdk java-openjdk-devel

cat <<EOF | sudo tee /etc/yum.repos.d/elasticsearch.repo
[elasticsearch-8.x]
name=Elasticsearch repository for 8.x packages
baseurl=https://artifacts.elastic.co/packages/8.x/yum
gpgcheck=1
gpgkey=https://artifacts.elastic.co/GPG-KEY-elasticsearch
enabled=1
autorefresh=1
type=rpm-md
EOF

sudo rpm --import https://artifacts.elastic.co/GPG-KEY-elasticsearch

sudo yum clean all

sudo yum makecache

sudo yum -y install elasticsearch

sudo nano /etc/elasticsearch/elasticsearch.yml

network.host: 0.0.0.0

http.port: 9200

discovery.seed_hosts: []
xpack.security.enabled: false

sudo systemctl enable --now elasticsearch.service

sudo systemctl restart elasticsearch.service

# kibana
sudo yum -y install kibana

sudo systemctl enable --now kibana

/etc/kibana/kibana.yml

server.port: 5601

server.host: "0.0.0.0"

server.publicBaseUrl: "http://<your_server_ip_goes_here>:5601"

elasticsearch.hosts: ["http://127.0.0.1:9200"]

sudo systemctl restart kibana 

sudo systemctl status kibana

http://<your-kibana-server-ip-here>:5601

# curl -IGET http://127.0.0.1:9200