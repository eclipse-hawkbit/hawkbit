Eclipse.IoT hawkBit - Artifact Repository File System
===
This module contains the implementation of [ArtifactStorage](../hawkbit-artifact-api/src/main/java/org/eclipse/hawkbit/artifact/ArtifactStorage.java) based on the file-system.
It's a very convenient and easy implementation of storing the artifact binaries into the file-system based on the SHA-1 hash naming.

Due to the limit of many file-systems of files within one directory, the files
are stored in different sub-directories based on the last four digits of the
SHA1-hash `/basepath/[two digit sha1]/[two digit sha1/sha1-hash-filename]`.
