# Eclipse hawkBit Documentation

The hawkBit documentation is based on [Jekyll](http://jekyllrb.com/). Jekyll is a Ruby gem and thus requires a Ruby runtime to be executed. Compiling the documentation is not included within the regular Maven build.

# Build and Serve documentation

## Unix / Mac
On a unix or mac you don't need to extra install Jekyll. The Maven build is downloading the ruby runtime and the necessary ruby-gems via the Maven rubygems-proxy repository. The Ruby runtime is downloaded into the `target` folder and executed during the build.

To serve the current documentation you only need to call `mvn install gem:exec@jekyll-serve` (within the `docs` folder). It automatically monitors the filesystem and every local changes are generated on-demand on the local server [http://127.0.0.1:4000/hawkbit/](http://127.0.0.1:4000/hawkbit/).

## Windows
On a Windows operating system you'll need to install Jekyll manually. If you don't have installed Jekyll on your machine you can use the [PortableJekyll](https://github.com/madhur/PortableJekyll) project. Just clone the Github repository and start the `setpath.cmd` which setups the necessary path entries into the CMD (don't forget to copy them into the environment path variable to have the path set for every command prompt).

The Maven build on windows just executes the `Jekyll` process using the maven-exec plugin. This allows to use Maven to build and serve the documentation on a windows machine as well.

To serve the current documentation you only need to call `mvn exec:exec@jekyll-serve`. It automatically monitors the filesystem and every local changes are generated on-demand on the local server [http://127.0.0.1:4000/hawkbit/](http://127.0.0.1:4000/hawkbit/).
