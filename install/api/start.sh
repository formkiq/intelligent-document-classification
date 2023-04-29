#!/bin/bash

cd ~/intelligent-document-classification
echo "Launching Intelligent Document Classification Application"
docker-compose -f docker-compose-prod.yml up -d