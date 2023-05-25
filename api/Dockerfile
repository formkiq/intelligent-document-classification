FROM --platform=linux/amd64 gradle:8-jdk17-alpine as build

WORKDIR /workspace/app

COPY gradlew .
COPY gradle gradle
COPY gradle.properties gradle.properties
COPY build.gradle .
COPY src src
COPY settings.gradle settings.gradle
COPY micronaut-cli.yml micronaut-cli.yml

RUN ./gradlew build -x test

FROM --platform=linux/amd64 eclipse-temurin:17-jre-alpine

RUN apk add --no-cache tesseract-ocr tesseract-ocr-data-eng

WORKDIR /home/app
COPY --from=build /workspace/app/build/docker/main/layers/libs /home/app/libs
COPY --from=build /workspace/app/build/docker/main/layers/classes /home/app/classes
COPY --from=build /workspace/app/build/docker/main/layers/resources /home/app/resources
COPY --from=build /workspace/app/build/docker/main/layers/application.jar /home/app/application.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/home/app/application.jar"]
