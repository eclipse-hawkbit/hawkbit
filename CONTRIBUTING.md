# Contributing to Eclipse hawkBit

:+1: First off, thanks for taking the time to contribute! We really appreciate this. :+1:

Please read this if you intend to contribute to the project.

## Conventions

### Code Style

* Formatting (all file types - Java, XML, YAML, Markdown, ...):
    * [intellij_codeformatter.xml](intellij_codeformatter.xml) (IntelliJ IDEA code style, spaces only, 4) is the
      single source of truth for formatting, enforced by [scripts/IdeaFormat](scripts/IdeaFormat).
    * check with `mvn validate -PideaFormat`, format with `mvn validate -PideaFormat -Didea.format.args=--format`.
      Covers the files changed against `origin/master` (merge base) in the directory maven is started in - the root, a
      module directory or `-f <module dir>`. More options via `-Didea.format.args="..."`: `--base <ref>` for another
      base, `--all` for all files, paths (files / directories)
    * needs a local IntelliJ IDEA (`idea format`, a single IDEA process for all files). If the `idea` launcher
      (`idea64.exe` on Windows) isn't on the `PATH`, set it with `-Didea.binary=<launcher>`, e.g.
      `-Didea.binary="/Applications/IntelliJ IDEA.app"` (macOS) or `-Didea.binary=<install-dir>/bin/idea.sh` (Linux).
      It runs with its own IDEA config, so it works while the IDE is open
    * the CI `style_check` workflow runs the check on every pull request
    * [eclipse_codeformatter.xml](eclipse_codeformatter.xml) is provided only for Eclipse IDE users. It approximates
      the IntelliJ style but is NOT identical (nested continuation indent, line breaks after lambda arrows, kept
      operator line breaks, text blocks), so format with maven before pushing
* License headers:
    * every file needs the EPL license
      header - [licenses/LICENSE_HEADER_TEMPLATE.txt](licenses/LICENSE_HEADER_TEMPLATE.txt)
      with any year (or year range) and one of the accepted copyright holders (`__HOLDER__` header section in the
      `checkLicense` profile of the root `pom.xml` - add new holders there)
    * check with `mvn license:check -PcheckLicense`, add missing headers (current year) with
      `mvn license:format -PcheckLicense`
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

Test classes are documented with a class level Javadoc in the following format:

```java
/**
 * Feature: TEST_TYPE - HAWKBIT_COMPONENT<br/>
 * Story: Test class description
 */
```

Test types are:

* Unit Tests - for single units tests with a mocked environment
* Component Tests - for complete components including lower layers, e.g. Spring MVC test on rest API including
  repository and database.
* Integration Tests - including clients or the full security / server setup.
* System Tests - on target environments, e.g. Cloud Foundry.

Examples for hawkBit components:

* Management API
* Direct Device Integration API
* Device Management Federation API
* Repository
* Security

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

You do this by adding the `-s` flag when you make the commit (s), e.g.

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

The template is [licenses/LICENSE_HEADER_TEMPLATE.txt](licenses/LICENSE_HEADER_TEMPLATE.txt). Check headers with
`mvn license:check -PcheckLicense` (also run by CI).

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
