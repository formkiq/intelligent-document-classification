#!/bin/bash

#####################################################################
#
# This script STOPS the Intelligent Document Classification API
#
#####################################################################

cd ~/intelligent-document-classification
docker-compose -f docker-compose-prod.yml down