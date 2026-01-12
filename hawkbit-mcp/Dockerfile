#
# Copyright (c) 2025 Contributors to the Eclipse Foundation
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#

FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="Eclipse hawkBit Project"
LABEL org.opencontainers.image.source="https://github.com/eclipse-hawkbit/hawkbit"
LABEL org.opencontainers.image.description="Standalone MCP Server for hawkBit"

ARG JAR_FILE=target/*.jar

COPY ${JAR_FILE} app.jar

EXPOSE 8081

ENV HAWKBIT_URL=http://localhost:8080

ENTRYPOINT ["java", "-jar", "/app.jar"]
