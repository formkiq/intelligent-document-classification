FROM openjdk:19-jdk-alpine3.16

RUN apk update

# Install tesseract library
RUN apk add --no-cache tesseract-ocr

# Check the installation status
RUN tesseract --list-langs    
RUN tesseract -v  
