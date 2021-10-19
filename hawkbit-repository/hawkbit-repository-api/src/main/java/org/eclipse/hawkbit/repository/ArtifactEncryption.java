package org.eclipse.hawkbit.repository;

import java.io.InputStream;

public interface ArtifactEncryption {

    String encryptionAlgorithm();

    String generateEncryptionKey();

    String generateEncryptionIV();

    InputStream encryptStream(final String base64EncryptionKey, final String base64EncryptionIV,
            final InputStream stream);
}
