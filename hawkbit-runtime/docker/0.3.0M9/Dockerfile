FROM eclipse-temurin:17.0.7_7-jre-alpine

ENV HAWKBIT_VERSION=0.3.0M9 \
    HAWKBIT_HOME=/opt/hawkbit

EXPOSE 8080

COPY KEY .

RUN set -x \
    && apk add --no-cache --virtual build-dependencies gnupg unzip libressl wget \
    && gpg --import KEY \
    && mkdir -p $HAWKBIT_HOME \
    && cd $HAWKBIT_HOME \
    && wget -O hawkbit-update-server.jar --no-verbose https://repo1.maven.org/maven2/org/eclipse/hawkbit/hawkbit-update-server/$HAWKBIT_VERSION/hawkbit-update-server-$HAWKBIT_VERSION.jar \
    && wget -O hawkbit-update-server.jar.asc --no-verbose https://repo1.maven.org/maven2/org/eclipse/hawkbit/hawkbit-update-server/$HAWKBIT_VERSION/hawkbit-update-server-$HAWKBIT_VERSION.jar.asc \
    && gpg --batch --verify hawkbit-update-server.jar.asc hawkbit-update-server.jar \
    && apk del build-dependencies

VOLUME "$HAWKBIT_HOME/data"

WORKDIR $HAWKBIT_HOME
ENTRYPOINT ["java","-jar","hawkbit-update-server.jar","-Xms1024m -Xmx1024m -XX:MaxMetaspaceSize=250m -XX:MetaspaceSize=250m -Xss1024K -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+UseCompressedOops -XX:+HeapDumpOnOutOfMemoryError"]
