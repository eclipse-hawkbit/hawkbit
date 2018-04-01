FROM hawkbit/hawkbit-update-server:0.2.0M4

MAINTAINER Kai Zimmermann <kai.zimmermann@bosch-si.com>

ENV MARIADB_DRIVER_VERSION=2.1.2

COPY KEYS .
    
# Install mariadb driver
RUN set -x \
    && gpg --import KEYS \
    && wget -O $JAVA_HOME/lib/ext/mariadb-java-client.jar --no-verbose http://downloads.mariadb.com/Connectors/java/connector-java-$MARIADB_DRIVER_VERSION/mariadb-java-client-$MARIADB_DRIVER_VERSION.jar \
    && wget -O $JAVA_HOME/lib/ext/mariadb-java-client.jar.asc --no-verbose http://downloads.mariadb.com/Connectors/java/connector-java-$MARIADB_DRIVER_VERSION/mariadb-java-client-$MARIADB_DRIVER_VERSION.jar.asc \
    && gpg --verify --batch $JAVA_HOME/lib/ext/mariadb-java-client.jar.asc $JAVA_HOME/lib/ext/mariadb-java-client.jar

ENTRYPOINT ["java","-jar","hawkbit-update-server.jar","--spring.profiles.active=amqp,mysql","-Xmx768m -Xmx768m -XX:MaxMetaspaceSize=250m -XX:MetaspaceSize=250m -Xss300K -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+UseCompressedOops -XX:+HeapDumpOnOutOfMemoryError"]
