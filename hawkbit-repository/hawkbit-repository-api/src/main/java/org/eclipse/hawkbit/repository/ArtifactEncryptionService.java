package org.eclipse.hawkbit.repository;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

public class ArtifactEncryptionService {

    private static final ArtifactEncryptionService SINGLETON = new ArtifactEncryptionService();

    @Autowired(required = false)
    private ArtifactEncryption artifactEncryption;

    @Autowired(required = false)
    private ArtifactEncryptionSecretsStore artifactEncryptionSecretsStore;

    private ArtifactEncryptionService() {
    }

    /**
     * @return the artifact encryption service singleton instance
     */
    public static ArtifactEncryptionService getInstance() {
        return SINGLETON;
    }

    public boolean isEncryptionSupported() {
        return artifactEncryption != null && artifactEncryptionSecretsStore != null;
    }

    public void addSoftwareModuleEncryptionSecrets(final long smId) {
        final Map<String, String> secrets = artifactEncryption.generateSecrets();
        secrets.forEach((key, value) -> artifactEncryptionSecretsStore.addSecret(smId, key, value));
        // we want to clear secrets from memory as soon as possible
        secrets.clear();
    }

    public boolean isSoftwareModuleEncrypted(final long smId) {
        if (!isEncryptionSupported()) {
            return false;
        }

        for (final String requiredSecretsKey : artifactEncryption.requiredSecretKeys()) {
            if (!artifactEncryptionSecretsStore.secretExists(smId, requiredSecretsKey)) {
                return false;
            }
        }

        return true;
    }

    public InputStream encryptSoftwareModuleArtifact(final long smId, final InputStream artifactStream) {
        if (!isEncryptionSupported()) {
            throw new RuntimeException("Encryption not supported!");
        }

        return artifactEncryption.encryptStream(getSoftwareModuleEncryptionSecrets(smId), artifactStream);
    }

    private Map<String, String> getSoftwareModuleEncryptionSecrets(final long smId) {
        final Set<String> requiredSecretsKeys = artifactEncryption.requiredSecretKeys();
        final Map<String, String> requiredSecrets = new HashMap<>();
        for (final String requiredSecretsKey : requiredSecretsKeys) {
            final Optional<String> requiredSecretsValue = artifactEncryptionSecretsStore.getSecret(smId,
                    requiredSecretsKey);
            requiredSecretsValue.ifPresent(secretValue -> requiredSecrets.put(requiredSecretsKey, secretValue));
        }

        return requiredSecrets;
    }

    public InputStream decryptSoftwareModuleArtifact(final long smId, final InputStream encryptedArtifactStream) {
        if (!isEncryptionSupported()) {
            throw new RuntimeException("Decryption not supported!");
        }

        return artifactEncryption.decryptStream(getSoftwareModuleEncryptionSecrets(smId), encryptedArtifactStream);
    }
}
