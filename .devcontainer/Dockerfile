# [Choice] Java version: 8. 11, 15
ARG VARIANT=8
FROM mcr.microsoft.com/vscode/devcontainers/java:${VARIANT}

RUN su vscode -c "source /usr/local/sdkman/bin/sdkman-init.sh && sdk install maven \"${MAVEN_VERSION}\""

# [Optional] Uncomment this section to install additional OS packages.
# RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
#     && apt-get -y install --no-install-recommends <your-package-list-here>
