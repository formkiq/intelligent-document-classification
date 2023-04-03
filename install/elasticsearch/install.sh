#!/bin/bash

sudo yum -y update

sudo amazon-linux-extras enable java-openjdk11

sudo yum -y install java-11-openjdk

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

sudo sed -i '/xpack.security.enabled/s/: .*/: false/' /etc/elasticsearch/elasticsearch.yml
echo 'discovery.seed_hosts: []' | sudo tee -a /etc/elasticsearch/elasticsearch.yml
echo 'network.host: 0.0.0.0' | sudo tee -a /etc/elasticsearch/elasticsearch.yml

sudo systemctl restart elasticsearch

sudo systemctl enable elasticsearch

# kibana
sudo yum -y install kibana

echo 'server.port: 5601' | sudo tee -a /etc/kibana/kibana.yml
echo 'server.host: "0.0.0.0"' | sudo tee -a /etc/kibana/kibana.yml
echo 'elasticsearch.hosts: ["http://127.0.0.1:9200"]' | sudo tee -a /etc/kibana/kibana.yml

echo "server.publicBaseUrl: \"http://${IP_PUBLIC}:5601\"" | sudo tee -a /etc/kibana/kibana.yml

sudo systemctl restart kibana
sudo systemctl enable kibana
