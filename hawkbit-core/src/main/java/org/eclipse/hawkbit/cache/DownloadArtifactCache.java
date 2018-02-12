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
 * Cache Object for downloading an artifact.
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
        this.downloadType = downloadType;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public DownloadType getDownloadType() {
        return downloadType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((downloadType == null) ? 0 : downloadType.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DownloadArtifactCache other = (DownloadArtifactCache) obj;
        if (downloadType != other.downloadType) {
            return false;
        }
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }
}
