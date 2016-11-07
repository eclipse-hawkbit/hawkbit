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
 * A interface declaration of the download-id-cache which allows to store
 * volatile generated download-IDs used to have a single unique download-request
 * e.g. for distributed download-server.
 * 
 * A valid download-id is created during a successful download-authorization
 * request e.g. from a download-server. In the DMF response a unique
 * download-URL containing a generated download-id which can be used to download
 * the artifact using this single download-url.
 * 
 * The {@link DownloadIdCache} handles storing this unique download-id from the
 * DMF authorization request until the actual artifact download-request via HTTP
 * with the unique ID is performed.
 * 
 */
public interface DownloadIdCache {

    /**
     * Puts a given artifact cache object with the given downloadId key into the
     * cache.
     * 
     * @param downloadId
     *            the ID to store the cache object to look it up later on
     * @param downloadArtifactCacheObject
     *            the object to store into the cache
     */
    void put(final String downloadId, final DownloadArtifactCache downloadArtifactCacheObject);

    /**
     * Retrieves a {@link DownloadArtifactCache} by a given downloadId.
     * 
     * @param downloadId
     *            the ID to retrieve the artifact cache object
     * @return the found {@link DownloadArtifactCache} or {@code null} if none
     *         exists for the given ID
     */
    DownloadArtifactCache get(final String downloadId);

    /**
     * Evicts a {@link DownloadArtifactCache} for the given downloadId
     * 
     * @param downloadId
     *            the ID to be evicted
     */
    void evict(String downloadId);

}
