#!/bin/bash

#####################################################################
#
# This script Restarts the Intelligent Document Classification API
#
#####################################################################

cd ~/intelligent-document-classification

docker-compose -f docker-compose-prod.yml down
docker-compose -f docker-compose-prod.yml up -d