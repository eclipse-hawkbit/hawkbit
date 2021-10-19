package org.eclipse.hawkbit.repository.jpa;

import java.io.InputStream;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.hawkbit.repository.ArtifactEncryption;

public class DefaultArtifactEncryption implements ArtifactEncryption {

    private static final String ENCRYPTION_ALGORITHM = "AES";
    private static final int KEY_LENGTH = 256;
    private static final String ENCRYPTION_CYPHER = "AES/GCM/NoPadding";
    private static final int AUTH_TAG_LENGTH = 128;
    private static final int AUTH_IV_LENGTH = 12;

    @Override
    public String encryptionAlgorithm() {
        return ENCRYPTION_ALGORITHM + KEY_LENGTH;
    }

    @Override
    public String generateEncryptionKey() {
        try {
            final KeyGenerator generator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM);
            generator.init(KEY_LENGTH);
            final Key key = generator.generateKey();

            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String generateEncryptionIV() {
        try {
            final byte[] iv = new byte[AUTH_IV_LENGTH];
            SecureRandom.getInstanceStrong().nextBytes(iv);

            return Base64.getEncoder().encodeToString(iv);
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public InputStream encryptStream(final String base64EncryptionKey, final String base64EncryptionIV,
            final InputStream stream) {
        try {
            final Cipher cipher = Cipher.getInstance(ENCRYPTION_CYPHER);
            final Key key = new SecretKeySpec(Base64.getDecoder().decode(base64EncryptionKey), ENCRYPTION_ALGORITHM);
            final GCMParameterSpec cipherParamSpec = new GCMParameterSpec(AUTH_TAG_LENGTH,
                    Base64.getDecoder().decode(base64EncryptionIV));
            cipher.init(Cipher.ENCRYPT_MODE, key, cipherParamSpec);

            return new CipherInputStream(stream, cipher);
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
