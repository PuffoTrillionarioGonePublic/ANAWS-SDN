FROM maven:3.8.3-jdk-8 AS build

WORKDIR /app

COPY ./pom.xml /app/pom.xml
COPY ./lib /app/lib
COPY ./src /app/src

RUN --mount=type=cache,target=/root/.m2 mvn package -Dmaven.test.skip=true

# ---------------------------
FROM openjdk:8-jre-alpine AS run

WORKDIR /app
EXPOSE 6653 8080

# disable tcp checksum offloading for interfaces in $NO_CHECKSUMOFFLOAD_IFS
RUN apk add --no-cache ethtool

RUN echo 'set -e' > /etc/entrypoint.sh
RUN echo 'for if in ${NO_CHECKSUMOFFLOAD_IFS//,/ }; do ethtool --offload $(ip -o addr show | grep -E "inet ${if}/" | cut -d " " -f 2) rx off tx off; done' >> /etc/entrypoint.sh
RUN echo 'exec "$@"' >> /etc/entrypoint.sh

ENTRYPOINT ["/bin/sh", "/etc/entrypoint.sh"]

COPY floodlight.properties .
COPY --from=build /app/target/floodlight.jar .
CMD ["java", "-jar", "floodlight.jar", "-cf", "floodlight.properties"]