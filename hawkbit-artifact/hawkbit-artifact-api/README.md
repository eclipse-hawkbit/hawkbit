hawkBit Artifact API
===
The module contains artifact API classes supporting following main concepts:
* Artifact Storage - represented by the [ArtifactStorage](src/main/java/org/eclipse/hawkbit/artifact/ArtifactStorage.java) interface. It serves for artifact binary store operations
* Artifact Encryption - represented by the [ArtifactEncryptionService](src/main/java/org/eclipse/hawkbit/artifact/encryption/ArtifactEncryptionService.java). It is a pluggable implementation of artifact encryption operations.
* Artifact URL handling - represented by[ArtifactUrlResolver](src/main/java/org/eclipse/hawkbit/artifact/urlresolver/ArtifactUrlResolver.java) interface. It provides resolving URLs to the artifacts. The module provides a simple property based implementation ([PropertyBasedArtifactUrlResolver](src/main/java/org/eclipse/hawkbit/artifact/urlresolver/PropertyBasedArtifactUrlResolver.java))