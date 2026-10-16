Eclipse.IoT hawkBit - Artifact Repository File System
===
This module contains the implementation
of [ArtifactStorage](../hawkbit-artifact-api/src/main/java/org/eclipse/hawkbit/artifact/ArtifactStorage.java) based on
the file-system.
It's a very convenient and easy implementation of storing the artifact binaries into the file-system based on the SHA-1
hash naming.

Due to the limit of many file-systems of files within one directory, the files
are stored in different sub-directories based on the last four characters of the
SHA1-hash: `<basepath>/<TENANT>/<sha1 chars [-4..-2)>/<sha1 last 2 chars>/<sha1-hash-filename>`.
For instance, an artifact with SHA-1 `...a1b2` of tenant `default` is stored as
`<basepath>/DEFAULT/a1/b2/...a1b2`. The tenant directory is the trimmed, upper-cased tenant name.

The base path is configured via `org.eclipse.hawkbit.artifact.fs.path` (default `./artifactrepo`).
