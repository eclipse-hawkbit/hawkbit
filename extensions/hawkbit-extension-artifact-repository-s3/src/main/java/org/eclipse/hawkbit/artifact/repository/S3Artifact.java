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
import com.amazonaws.services.s3.model.GetObjectRequest;

/**
 * An {@link DbArtifact} implementation which retrieves the {@link InputStream}
 * from the {@link AmazonS3} client.
 */
public class S3Artifact extends DbArtifact {

    private final AmazonS3 amazonS3;
    private final S3RepositoryProperties s3Properties;
    private final String sha1;

    S3Artifact(final AmazonS3 amazonS3, final S3RepositoryProperties s3Properties, final String sha1) {
        this.amazonS3 = amazonS3;
        this.s3Properties = s3Properties;
        this.sha1 = sha1;
    }

    @Override
    public InputStream getFileInputStream() {
        final GetObjectRequest getObjectRequest = new GetObjectRequest(s3Properties.getBucketName(), sha1);
        return this.amazonS3.getObject(getObjectRequest).getObjectContent();
    }

    @Override
    public String toString() {
        return "S3Artifact [sha1=" + sha1 + ", getArtifactId()=" + getArtifactId() + ", getHashes()=" + getHashes()
                + ", getSize()=" + getSize() + ", getContentType()=" + getContentType() + "]";
    }
}
