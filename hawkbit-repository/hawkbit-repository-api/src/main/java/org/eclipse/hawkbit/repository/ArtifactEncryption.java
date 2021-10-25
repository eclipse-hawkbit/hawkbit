package org.eclipse.hawkbit.repository;

import java.io.InputStream;

public interface ArtifactEncryption {

    boolean isEncrypted(final long softwareModuleId);

    void generateSecrets(final long softwareModuleId);

    InputStream encryptStream(final long softwareModuleId, final InputStream stream);
}
