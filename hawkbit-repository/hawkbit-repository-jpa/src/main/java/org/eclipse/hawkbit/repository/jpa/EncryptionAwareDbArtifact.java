/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.io.InputStream;
import java.util.function.Function;

import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifactHash;

/**
 * {@link DbArtifact} implementation that decrypts the underlying artifact
 * binary input stream.
 *
 */
public class EncryptionAwareDbArtifact implements DbArtifact {

    private final DbArtifact encryptedDbArtifact;
    private final Function<InputStream, InputStream> decryptionFunction;

    public EncryptionAwareDbArtifact(final DbArtifact encryptedDbArtifact,
            final Function<InputStream, InputStream> decryptionFunction) {
        this.encryptedDbArtifact = encryptedDbArtifact;
        this.decryptionFunction = decryptionFunction;
    }

    @Override
    public String getArtifactId() {
        return encryptedDbArtifact.getArtifactId();
    }

    @Override
    public DbArtifactHash getHashes() {
        return encryptedDbArtifact.getHashes();
    }

    @Override
    public long getSize() {
        return encryptedDbArtifact.getSize();
    }

    @Override
    public String getContentType() {
        return encryptedDbArtifact.getContentType();
    }

    @Override
    public InputStream getFileInputStream() {
        return decryptionFunction.apply(encryptedDbArtifact.getFileInputStream());
    }
}
