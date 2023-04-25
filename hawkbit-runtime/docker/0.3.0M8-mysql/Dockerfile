FROM hawkbit/hawkbit-update-server:0.3.0M8

ENV MARIADB_DRIVER_VERSION=2.7.8

COPY KEY .

RUN set -x \
    && apk add --no-cache --virtual build-dependencies gnupg unzip libressl wget \
    && gpg --import KEY \
    && wget -O mariadb-java-client.jar --no-verbose https://downloads.mariadb.com/Connectors/java/connector-java-$MARIADB_DRIVER_VERSION/mariadb-java-client-$MARIADB_DRIVER_VERSION.jar \
    && wget -O mariadb-java-client.jar.asc --no-verbose https://downloads.mariadb.com/Connectors/java/connector-java-$MARIADB_DRIVER_VERSION/mariadb-java-client-$MARIADB_DRIVER_VERSION.jar.asc \
    && gpg --verify --batch mariadb-java-client.jar.asc mariadb-java-client.jar \
    && apk del build-dependencies

ENTRYPOINT ["java","--module-path=mariadb-java-client.jar","--add-modules=org.mariadb.jdbc","-jar","hawkbit-update-server.jar","--spring.profiles.active=mysql","-Xms1024m -Xmx1024m -XX:MaxMetaspaceSize=250m -XX:MetaspaceSize=250m -Xss1024K -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+UseCompressedOops -XX:+HeapDumpOnOutOfMemoryError"]
