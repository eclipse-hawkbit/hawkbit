FROM hawkbit/hawkbit-update-server:0.2.0M4

MAINTAINER Kai Zimmermann <kai.zimmermann@bosch-si.com>

ENV MARIADB_DRIVER_VERSION=2.1.2
    
# Install mariadb driver
RUN set -x \
    && gpg --keyserver pgp.mit.edu --recv-keys CE1A3DD5E3C94F49 f8957c3395910043 \
    && gpg --trusted-key CE1A3DD5E3C94F49 --list-keys f8957c3395910043 \
    && wget -O $JAVA_HOME/lib/ext/mariadb-java-client.jar --no-verbose http://downloads.mariadb.com/Connectors/java/connector-java-$MARIADB_DRIVER_VERSION/mariadb-java-client-$MARIADB_DRIVER_VERSION.jar \
    && wget -O $JAVA_HOME/lib/ext/mariadb-java-client.jar.asc --no-verbose http://downloads.mariadb.com/Connectors/java/connector-java-$MARIADB_DRIVER_VERSION/mariadb-java-client-$MARIADB_DRIVER_VERSION.jar.asc \
    && gpg --verify --batch $JAVA_HOME/lib/ext/mariadb-java-client.jar.asc $JAVA_HOME/lib/ext/mariadb-java-client.jar

ENTRYPOINT ["java","-jar","hawkbit-update-server.jar","--spring.profiles.active=amqp,mysql","-Xmx768m -Xmx768m -XX:MaxMetaspaceSize=250m -XX:MetaspaceSize=250m -Xss300K -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+UseCompressedOops -XX:+HeapDumpOnOutOfMemoryError"]
