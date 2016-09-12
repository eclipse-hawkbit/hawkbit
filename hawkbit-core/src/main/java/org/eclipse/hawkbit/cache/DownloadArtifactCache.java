/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.cache;

/**
 * Cache Object for download a Artifact.
 */
public class DownloadArtifactCache {

    private final DownloadType downloadType;
    private final String id;

    /**
     * Constructor.
     *
     * @param downloadType
     *            the type for searching the artifact.
     * @param id
     *            the searching id e.g. sha1, md5
     */
    public DownloadArtifactCache(final DownloadType downloadType, final String id) {
        super();
        this.downloadType = downloadType;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public DownloadType getDownloadType() {
        return downloadType;
    }

}
