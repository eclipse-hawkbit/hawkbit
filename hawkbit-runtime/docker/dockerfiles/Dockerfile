# set Java
ARG JAVA_VERSION=17.0.9_9
# extracts spring layers from the app jar (to optimize boot)
FROM eclipse-temurin:${JAVA_VERSION}-jre-alpine as build

ARG HAWKBIT_APP=hawkbit-update-server
ARG HAWKBIT_VERSION=0.3.0

ENV BUILD_DIR=/opt/hawkbit_build
ENV APP=${HAWKBIT_APP}
ENV VERSION=${HAWKBIT_VERSION}

COPY KEY .

RUN set -x &&\
 apk add --no-cache --virtual build-dependencies gnupg unzip libressl wget &&\
 gpg --import KEY &&\
 mkdir -p ${BUILD_DIR} &&\
 cd ${BUILD_DIR} &&\
 wget -O ${APP}.jar --no-verbose https://repo1.maven.org/maven2/org/eclipse/hawkbit/${APP}/${VERSION}/${APP}-${VERSION}.jar &&\
 wget -O ${APP}.jar.asc --no-verbose https://repo1.maven.org/maven2/org/eclipse/hawkbit/${APP}/${VERSION}/${APP}-${VERSION}.jar.asc &&\
 gpg --batch --verify ${APP}.jar.asc ${APP}.jar &&\
 apk del build-dependencies &&\
 java -Djarmode=layertools -jar ${APP}.jar extract --destination . &&\
 rm ${APP}.jar ${APP}.jar.asc /KEY

FROM eclipse-temurin:${JAVA_VERSION}-jre-alpine

RUN addgroup -S rollouts && adduser -D sp -G rollouts
USER sp

ENV BUILD_DIR=/opt/hawkbit_build
COPY --from=build ${BUILD_DIR}/dependencies/ ./
COPY --from=build ${BUILD_DIR}/spring-boot-loader/ ./
COPY --from=build ${BUILD_DIR}/snapshot-dependencies/ ./
COPY --from=build ${BUILD_DIR}/application/ ./

ARG CONTAINER_PORT=8080
ENV JAVA_OPTS="-Xms768m -Xmx768m -XX:MaxMetaspaceSize=250m -XX:MetaspaceSize=250m -Xss300K -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+UseCompressedOops -XX:+HeapDumpOnOutOfMemoryError"

EXPOSE ${CONTAINER_PORT}
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} org.springframework.boot.loader.JarLauncher ${@}"]
