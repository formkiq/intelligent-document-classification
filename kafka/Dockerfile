FROM --platform=linux/amd64 eclipse-temurin:17-jre-alpine

ENV KAFKA_VERSION 3.4.0
ENV kafka_bin_version=2.12-$KAFKA_VERSION

RUN apk add --no-cache bash curl ca-certificates \
  && mkdir -p /opt/kafka \
  && curl -SLs "https://archive.apache.org/dist/kafka/${KAFKA_VERSION}/kafka_2.13-${KAFKA_VERSION}.tgz" | tar -xzf - --strip-components=1 -C /opt/kafka \
  && rm -rf /var/cache/apk/*

WORKDIR /opt/kafka
ENTRYPOINT ["bin/kafka-server-start.sh"]

RUN sed -i 's/zookeeper.connect=localhost:2181/zookeeper.connect=zookeeper1:2181/' config/server.properties
CMD ["config/server.properties"]
