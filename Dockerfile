# syntax=docker/dockerfile:1
# check=error=true

ARG JDK_VERSION="21.0.2_13-jre"
ARG MAVEN_IMAGE="maven:3.9-eclipse-temurin-21"

FROM ${MAVEN_IMAGE} AS builder
WORKDIR /build
COPY postcheck/pom.xml ./pom.xml
RUN mvn -q -B -DskipTests dependency:go-offline
COPY postcheck/src ./src
RUN mvn -q -B -DskipTests package \
    && cp target/postcheck.jar /postcheck.jar

FROM eclipse-temurin:${JDK_VERSION}

ARG MUSTANG_VERSION="2.23.0"
ARG MUSTANG_SHA256="344c88b8d9bddccae23899a87d1ef31c4d38532383faa6303c381ee489cabe07"

RUN useradd --system --user-group --uid=1001 --shell=/bin/bash mustang \
    && mkdir /opt/jars \
    && chown -R mustang:mustang /opt/jars

USER mustang
WORKDIR /home/mustang

RUN wget --no-verbose --tries=3 --timeout=30 \
        "https://github.com/ZUGFeRD/mustangproject/releases/download/core-${MUSTANG_VERSION}/Mustang-CLI-${MUSTANG_VERSION}.jar" \
        -O /opt/jars/mustang-cli.jar \
    && echo "${MUSTANG_SHA256}  /opt/jars/mustang-cli.jar" | sha256sum -c -

COPY --from=builder /postcheck.jar /opt/jars/postcheck.jar

USER root
RUN ln -sf /opt/jars/mustang-cli.jar /opt/mustang-cli.jar \
    && ln -sf /opt/jars/postcheck.jar /opt/postcheck.jar
COPY entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

USER mustang
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
