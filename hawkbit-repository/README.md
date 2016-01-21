# hawkBit metadata repository

The repository is in charge for managing the meta data of the update server, e.g. provisioning targets, distribution sets, software modules etc.

# Build

[indent=0]
----
	$ mvn clean install
----

Note, in order to build correctly in your IDE, you have to add ./target/generated-sources/apt to your build path.