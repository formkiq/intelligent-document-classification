#!/bin/bash

yum -y update

amazon-linux-extras enable java-openjdk11

yum -y install java-11-openjdk

cat <<EOF | tee /etc/yum.repos.d/elasticsearch.repo
[elasticsearch-8.x]
name=Elasticsearch repository for 8.x packages
baseurl=https://artifacts.elastic.co/packages/8.x/yum
gpgcheck=1
gpgkey=https://artifacts.elastic.co/GPG-KEY-elasticsearch
enabled=1
autorefresh=1
type=rpm-md
EOF

rpm --import https://artifacts.elastic.co/GPG-KEY-elasticsearch

yum clean all

yum makecache

yum -y install elasticsearch

sed -i '/xpack.security.enabled/s/: .*/: false/' /etc/elasticsearch/elasticsearch.yml
echo 'discovery.seed_hosts: []' | tee -a /etc/elasticsearch/elasticsearch.yml
echo 'network.host: 0.0.0.0' | tee -a /etc/elasticsearch/elasticsearch.yml

systemctl restart elasticsearch

systemctl enable elasticsearch

# kibana
yum -y install kibana

echo 'server.port: 5601' | tee -a /etc/kibana/kibana.yml
echo 'server.host: "0.0.0.0"' | tee -a /etc/kibana/kibana.yml
echo 'elasticsearch.hosts: ["http://127.0.0.1:9200"]' | tee -a /etc/kibana/kibana.yml

echo "server.publicBaseUrl: \"http://${IP_PUBLIC}:5601\"" | tee -a /etc/kibana/kibana.yml

systemctl restart kibana
systemctl enable kibana
