#!/bin/bash

sudo yum -y update

sudo yum -y install docker

sudo curl -L https://github.com/docker/compose/releases/download/1.22.0/docker-compose-$(uname -s)-$(uname -m) -o /usr/local/bin/docker-compose

sudo chmod +x /usr/local/bin/docker-compose

# sudo usermod -a -G docker ec2-user
# id ec2-user
# newgrp docker

sudo systemctl enable docker.service

sudo systemctl start docker.service

echo "${AZL_IP}  elasticsearch" | sudo tee -a /etc/hosts

# curl -IGET http://elasticsearch:9200