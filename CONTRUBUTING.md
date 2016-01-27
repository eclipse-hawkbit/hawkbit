# Contributing to eclipse hawkBit

:+1: First off, thanks for taking the time to contribute! We really appreciate this. :+1:

Please read this if you intend to contribute to the project.

## Code Conventions

* Java files:
  * we follow the standard eclipse IDE (built in) code formatter with the following changes:
    * Tab policy: spaces only: 4
  * We recommend using at least Eclipse [Mars](https://www.eclipse.org/mars/) IDE release. It seems that the Java code formatter line break handling has been changed between [Luna](https://www.eclipse.org/luna/) and Mars.
* XML files:
  * we follow the standard eclipse IDE XML formatter with the following changes:
    * Indent using spaces only: 3
* SCSS files:
  * we follow the standard [scss-lint](https://github.com/brigade/scss-lint/) rules with the following exception:
    * disabled rules: ImportantRule, PropertySortOrder
* Sonarqube:
  * Our rule set is defined [here](http://sonar.eu-gb.mybluemix.net)

## Legal considerations for your contribution

The following steps are necessary to comply with the Eclipse Foundation's IP policy.

Please also read [this](http://wiki.eclipse.org/Development_Resources/Contributing_via_Git)

In order for any contributions to be accepted you MUST do the following things.

* Sign the [Eclipse Foundation Contributor License Agreement](http://www.eclipse.org/legal/CLA.php).
To sign the Eclipse CLA you need to:

  * Obtain an Eclipse Foundation userid. Anyone who currently uses Eclipse Bugzilla or Gerrit systems already has one of those.
If you don’t, you need to [register](https://dev.eclipse.org/site_login/createaccount.php).

  * Login into the [projects portal](https://projects.eclipse.org/), select “My Account”, and then the “Contributor License Agreement” tab.

* Add your github username in your Eclipse Foundation account settings. Log in it to Eclipse and go to account settings.

* "Sign-off" your commits

Every commit you make in your patch or pull request MUST be "signed off".

You do this by adding the `-s` flag when you make the commit(s), e.g.

    git commit -s -m "Shave the yak some more"

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

# Further information

* [Eclipse Project Page](http://projects.eclipse.org/projects/iot.hawkbit)
