# Eclipse hawkBit Documentation

The hawkBit documentation is built with [Hugo](https://www.gohugo.io/) using
the [Material](http://github.com/digitalcraftsman/hugo-material-docs)
theme. Compiling the documentation is not included within the regular Maven build.

## Prerequisites

1. **Install Hugo**: see [installing Hugo](https://gohugo.io/getting-started/installing/) documentation on how to
   install Hugo.
2. **Install NODE.js and npm**
   see [installing Node.js and npm](https://docs.npmjs.com/downloading-and-installing-node-js-and-npm) documentation on
   how to install Node.js and npm
3. **Install Redocly CLI** see [installing Redocly CLI](https://redocly.com/docs/cli/installation/) documentation on how
   to install Redocly CLI
4. **Install hawkBit**: run `mvn install` in the parent directory to generate the latest REST docs for hawkBit.

## Build and Serve documentation

The following Maven targets are available in order to build and serve the documentation:

* `mvn install`: _i._ Copies the generated REST docs to `content/rest-api/` and _ii._ downloads the required Hugo theme
* `mvn site`: Serve the documentation on [localhost:1313/hawkbit/](http://localhost:1313/hawkbit/)

  _Note_: the local port could be different. Please, look at the _mvn site_ command output.
* `mvn clean`: Delete generated artifacts (REST docs, Hugo theme)

## Generate /public folder

In order to generate the `/public` folder, which can be put on a web-server, run the following command:

```bash
$ hugo
``` 