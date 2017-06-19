/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

import java.io.InputStream;

import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;

import com.amazonaws.services.s3.AmazonS3;

/**
 * An {@link DbArtifact} implementation which retrieves the {@link InputStream}
 * from the {@link AmazonS3} client.
 */
public class S3Artifact extends DbArtifact {

    private final AmazonS3 amazonS3;
    private final S3RepositoryProperties s3Properties;
    private final String key;

    S3Artifact(final AmazonS3 amazonS3, final S3RepositoryProperties s3Properties, final String key) {
        this.amazonS3 = amazonS3;
        this.s3Properties = s3Properties;
        this.key = key;
    }

    @Override
    public InputStream getFileInputStream() {
        return amazonS3.getObject(s3Properties.getBucketName(), key).getObjectContent();
    }

    @Override
    public String toString() {
        return "S3Artifact [key=" + key + ", getArtifactId()=" + getArtifactId() + ", getHashes()=" + getHashes()
                + ", getSize()=" + getSize() + ", getContentType()=" + getContentType() + "]";
    }
}
