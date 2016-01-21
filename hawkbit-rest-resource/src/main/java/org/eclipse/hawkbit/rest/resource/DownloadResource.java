/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.hawkbit.artifact.repository.ArtifactRepository;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;
import org.eclipse.hawkbit.cache.CacheConstants;
import org.eclipse.hawkbit.cache.DownloadArtifactCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * A resource for download artifacts.
 * 
 *
 *
 */
@RequestMapping(RestConstants.DOWNLOAD_ID_V1_REQUEST_MAPPING_BASE)
@RestController
public class DownloadResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadResource.class);

    @Autowired
    private ArtifactRepository artifactRepository;

    @Autowired
    @Qualifier(CacheConstants.DOWNLOAD_ID_CACHE)
    private Cache cache;

    /**
     * Handles the GET request for downloading an artifact.
     * 
     * @param downloadId
     *            the generated download id
     * @param response
     *            of the servlet
     * @return {@link ResponseEntity} with status {@link HttpStatus#OK} if
     *         successful
     */
    @RequestMapping(method = RequestMethod.GET, value = RestConstants.DOWNLOAD_ID_V1_REQUEST_MAPPING)
    @ResponseBody
    public ResponseEntity<Void> downloadArtifactByDownloadId(@PathVariable final String downloadId,
            final HttpServletResponse response) {
        try {
            final ValueWrapper cacheWrapper = cache.get(downloadId);
            if (cacheWrapper == null) {
                LOGGER.warn("Download Id {} could not be found", downloadId);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            final DownloadArtifactCache artifactCache = (DownloadArtifactCache) cacheWrapper.get();
            DbArtifact artifact = null;
            switch (artifactCache.getDownloadType()) {
            case BY_SHA1:
                artifact = artifactRepository.getArtifactBySha1(artifactCache.getId());
                break;

            default:
                LOGGER.warn("Download Type {} not supported", artifactCache.getDownloadType());
                break;
            }

            if (artifact == null) {
                LOGGER.warn("Artifact with cached id {} and download type {} could not be found.",
                        artifactCache.getId(), artifactCache.getDownloadType());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            try {
                IOUtils.copy(artifact.getFileInputStream(), response.getOutputStream());
            } catch (final IOException e) {
                LOGGER.error("Cannot copy streams", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } finally {
            cache.evict(downloadId);
        }

        return ResponseEntity.ok().build();
    }
}
