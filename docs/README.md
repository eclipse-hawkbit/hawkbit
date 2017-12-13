# Eclipse hawkBit Documentation
The hawkBit documenation is based on the [Jekyll](http://jekyllrb.com/)

Jekyll is a ruby gem and needs ruby to execute. 

# Build and Serve documentation
## Unix / Mac
On a unix or mac you don't need to extra install Jekyll. The maven build is downloading the ruby runtime and the necessary ruby-gems via the maven rubygems-proxy repository. The ruby runtime is downloaded into the `target` folder and executed during the build.

To serve the current documentation you only need to call `mvn install gem:exec@jekyll-serve`. It automatically monitors the filesystem and every local changes are generated on-demand on the local server [http://127.0.0.1:4000/](http://127.0.0.1:4000/). 

## Windows
On a windows operating system you'll need to install Jekyll manually. If you don't have installed Jekyll on your machine you can just use the [PortableJekyll](https://github.com/madhur/PortableJekyll) project. Just clone the Github repository and start the `setpath.cmd` which setups the necessary path entries into the CMD (Don't forget to copy them into the environment path variable to have the path set for every command prompt).

The maven build on windows just executes the `Jekyll` process using the maven-exec plugin. This allows to also use maven build to build and servce the documentation on a windows machine. 

To serve the current documentation you only need to call `mvn exec:exec@jekyll-serve`. It automatically monitors the filesystem and every local changes are generated on-demand on the local server [http://127.0.0.1:4000/](http://127.0.0.1:4000/).