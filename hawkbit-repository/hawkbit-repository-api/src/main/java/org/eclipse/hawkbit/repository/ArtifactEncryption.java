package org.eclipse.hawkbit.repository;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;

public interface ArtifactEncryption {

    Set<String> requiredSecretKeys();

    Map<String, String> generateSecrets();

    InputStream encryptStream(final Map<String, String> secrets, final InputStream stream);
}
