# Eclipse.IoT hawkBit - Artifact Repository MongoDB
HawkBit Artifact Repository is a library for storing binary artifacts and metadata into MongoDB.


## Using Artifact Repository MongoDB Extension
The module contains a spring-boot autoconfiguration for easily integration into spring-boot projects.
For using this extension in the hawkbit-example-application you just need to add the maven dependency.
```
<dependency>
  <groupId>org.eclipse.hawkbit</groupId>
  <artifactId>hawkbit-extension-artifact-repository-mongo</artifactId>
  <version>${project.version}</version>
</dependency>
```

If you do not have a mongoDB running you can use the the flapdoodle project to download and start an mongoDB on demand
```
<dependency>
  <groupId>de.flapdoodle.embed</groupId>
  <artifactId>de.flapdoodle.embed.mongo</artifactId>
  <version>${flapdoodle.version}</version>
</dependency>
``` 
