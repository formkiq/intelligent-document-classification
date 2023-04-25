FROM alpine:3.16

ENV KAFKA_VERSION 3.4.0
ENV SCALA_VERSION 2.13

LABEL name="kafka" version=${KAFKA_VERSION}

RUN apk add curl

RUN curl https://downloads.apache.org/kafka/3.4.0/kafka_2.13-${KAFKA_VERSION}.tgz | tar -xzf - -C /opt  && mv /opt/kafka_${SCALA_VERSION}-${KAFKA_VERSION} /opt/kafka

ENV PATH /sbin:/opt/kafka/bin/:$PATH

WORKDIR /opt/kafka

VOLUME ["/tmp/kafka-logs"]

EXPOSE 9092 2181

# COPY config/log4j.properties /opt/kafka/config/
COPY config/server.properties /opt/kafka/config/
COPY config/zookeeper.properties /opt/kafka/config/
COPY kafka-entrypoint.sh /kafka-entrypoint.sh
COPY scripts /

ENTRYPOINT ["/kafka-entrypoint.sh"]
CMD ["kafka-server-start.sh", "config/server.properties"]

# HEALTHCHECK --interval=5s --timeout=2s --retries=5 CMD bin/health.sh
