package org.eclipse.hawkbit.repository;

import java.util.Optional;

public interface ArtifactEncryptionSecretsStore {

    void addSecret(final long softwareModuleId, final String secretKey, final String secretValue);

    Optional<String> getSecret(final long softwareModuleId, final String secretKey);
}
