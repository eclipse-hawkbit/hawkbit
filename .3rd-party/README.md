# Third-Party Dependencies

This folder provides listings of all 3rd-party dependencies incl. their licenses. There is a dedicated subfolder for
each release (and milestone) holding the release-specific information.

The file could be generated using dash tool plugin ([Eclipse Dash License Tool](https://github.com/eclipse/dash-licenses)) by running in root folder:
```shell
mvn clean install -DskipTests -Ddash.skip=false \
  --projects '!org.eclipse.hawkbit:hawkbit-repository-test,!org.eclipse.hawkbit:hawkbit-dmf-rabbitmq-test'
```
