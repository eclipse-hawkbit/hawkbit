FROM openjdk:8u121-jre-alpine

MAINTAINER Kai Zimmermann <kai.zimmermann@bosch-si.com>

ENV HAWKBIT_VERSION=0.2.0M3 \
    HAWKBIT_HOME=/opt/hawkbit

# Http port
EXPOSE 8080

COPY KEY .

RUN set -x \
    && apk add --no-cache gnupg unzip \
    && apk add --no-cache libressl wget \
    && gpg --import KEY \
    && mkdir -p $HAWKBIT_HOME \
    && cd $HAWKBIT_HOME \
    && wget -O hawkbit-update-server.jar --no-verbose http://repo1.maven.org/maven2/org/eclipse/hawkbit/hawkbit-update-server/$HAWKBIT_VERSION/hawkbit-update-server-$HAWKBIT_VERSION.jar \
    && wget -O hawkbit-update-server.jar.asc --no-verbose http://repo1.maven.org/maven2/org/eclipse/hawkbit/hawkbit-update-server/$HAWKBIT_VERSION/hawkbit-update-server-$HAWKBIT_VERSION.jar.asc \
    && gpg --batch --verify hawkbit-update-server.jar.asc hawkbit-update-server.jar

VOLUME "$HAWKBIT_HOME/data"

WORKDIR $HAWKBIT_HOME
ENTRYPOINT ["java","-jar","hawkbit-update-server.jar","-Xmx768m -Xmx768m -XX:MaxMetaspaceSize=250m -XX:MetaspaceSize=250m -Xss300K -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+UseCompressedOops -XX:+HeapDumpOnOutOfMemoryError"]
