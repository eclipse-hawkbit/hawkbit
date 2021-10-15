package org.eclipse.hawkbit.repository;

import java.io.InputStream;

public interface ArtifactEncryption {

    String encryptionAlgorithm();

    String generateEncryptionKey();

    InputStream encryptStream(final String encryptionKey, final InputStream stream);
}
