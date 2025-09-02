Third-Party Dependencies
===

This folder contains DEPENDENCIES that has the list of all 3rd-party dependencies (including transitive) with their licenses and approval status. Each release (and milestone) holds the release-specific information.

DEPENDENCIES file is generated (automatically and committed) by [../.github/workflows/reusable_workflow_license-scan.yaml](../.github/workflows/reusable_workflow_license-scan.yaml) during the release process ([../.github/workflows/release.yaml](../.github/workflows/release.yaml)) and on daily basis ([../.github/workflows/license-scan.yaml](../.github/workflows/license-scan.yaml)). It is also

DEPENDENCIES file could be generated manually using [Eclipse Dash License Tool](https://github.com/eclipse/dash-licenses) maven plugin by running:
```shell
$ cd .. && mvn license-tool:license-check -Ddash.fail=false -PcheckLicense
```

Note: Some projects (e.g. test artifacts) could be excluded with *--projects* parameter, e.g:
```shell
$ cd .. && mvn license-tool:license-check -Ddash.fail=false -PcheckLicense \ --projects '!org.eclipse.hawkbit:hawkbit-repository-test,!org.eclipse.hawkbit:hawkbit-dmf-rabbitmq-test'
```
