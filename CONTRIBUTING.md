# Contributing to Eclipse hawkBit

:+1: First off, thanks for taking the time to contribute! We really appreciate this. :+1:

Please read this if you intend to contribute to the project.

## Conventions

### Code Style

* Java files:
    * we follow the standard eclipse IDE (built in) code formatter with the following changes:
        * Tab policy: spaces only: 4
    * We recommend using at least Eclipse [Mars](https://www.eclipse.org/mars/) IDE release. It seems that the Java code
      formatter line break handling has been changed between [Luna](https://www.eclipse.org/luna/) and Mars.
* XML files:
    * we follow the standard eclipse IDE XML formatter with the following changes:
        * Indent using spaces only: 3
* SCSS files:
    * we follow the standard [scss-lint](https://github.com/brigade/scss-lint/) rules with the following exception:
        * disabled rules: ImportantRule, PropertySortOrder
* Sonarqube:
    * Our rule set can be found [here](https://sonarcloud.io/organizations/bosch-iot-rollouts/rules)
    * Sonarqube reports can be
      found [here](https://sonarcloud.io/project/overview?id=org.eclipse.hawkbit%3Ahawkbit-parent)

### Utility library usage

hawkBit has currently [Apache commons lang](https://commons.apache.org/proper/commons-lang/) on the classpath in several
of its modules. However, we see introducing too many utility libraries problematic as we force these as transitive
dependencies on hawkBit users. We in fact are looking into reducing them in future not adding new ones.

So we kindly ask contributors:

* not introduce extra utility library dependencies
* keep them out of the core modules (e.g. hawkbit-core, hawkbit-rest-core) to avoid that all
  modules have them as transitive dependency
* use utility functions in general based in the following priority:
    * use utility functions from JDK if feasible
    * use Spring utility classes if feasible
    * use [Apache commons lang](https://commons.apache.org/proper/commons-lang/) if feasible

### Test documentation

Please document the test cases that you contribute by means of [Allure](https://docs.qameta.io/allure/) annotations and
proper test method naming.

All test classes are documented with [Allure's](https://docs.qameta.io/allure/#_behaviours_mapping) **@Feature** and *
*@Story** annotations in the following format:

```java
@Feature("TEST_TYPE - HAWKBIT_COMPONENT")
@Story("Test class description")
```

Test types are:

* Unit Tests - for single units tests with a mocked environment
* Component Tests - for complete components including lower layers, e.g. Spring MVC test on rest API including
  repository and database.
* Integration Tests - including clients, e.g. Selenium UI tests with various browsers.
* System Tests - on target environments, e.g. Cloud Foundry.

Examples for hawkBit components:

* Management API
* Direct Device Integration API
* Device Management Federation API
* Repository
* Security

```java
@Feature("Component Tests - Management API")
@Story("Distribution Set Type Resource")
```

In addition all test method's name describes in **camel case** what the test is all about and has in addition a long
description in Allures **@Description** annotation.

## Legal considerations for your contribution

Before your contribution can be accepted by the project team contributors must
electronically sign the [Eclipse Contributor Agreement (ECA)](http://www.eclipse.org/legal/ECA.php).

Commits that are provided by non-committers must have a Signed-off-by field in
the footer indicating that the author is aware of the terms by which the
contribution has been provided to the project. The non-committer must
additionally have an Eclipse Foundation account and must have a signed Eclipse
Contributor Agreement (ECA) on file.

For more information, please see the Eclipse Committer Handbook:
[https://www.eclipse.org/projects/handbook/#resources-commit](https://www.eclipse.org/projects/handbook/#resources-commit)

HowTo "Sign-off" your commits:

You do this by adding the `-s` flag when you make the commit(s), e.g.

```bash
git commit -s -m "Shave the yak some more"
```

### License Header

Please make sure newly created files contain a proper license header like this:

```java
/**
 * Copyright (c) {date} {owner} [and others]
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
```

## Making your changes

* Fork the repository on GitHub
* Create a new branch for your changes
* Make your changes
* Make sure you include tests
* Make sure the tests pass after your changes
* Commit your changes into that branch
* Use descriptive and meaningful commit messages
* If you have a lot of commits squash them into a single commit
* Make sure you use the `-s` flag when committing as explained above.
* Push your changes to your branch in your forked repository

## Submitting the changes

Submit a pull request via the normal GitHub UI (desktop or web).

## After submitting

* Do not use your branch for any other development, otherwise further changes that you make will be visible in the PR.

## Reporting a security vulnerability

If you find a vulnerability, **DO NOT** disclose it in the public immediately! Instead, give us the possibility to fix
it beforehand.
So please don’t report your finding using GitHub issues and better head over
to [https://eclipse.org/security](https://eclipse.org/security) and learn how to disclose a vulnerability in a safe and
responsible manner

## Further information

* [Eclipse Project Page](http://projects.eclipse.org/projects/iot.hawkbit)
