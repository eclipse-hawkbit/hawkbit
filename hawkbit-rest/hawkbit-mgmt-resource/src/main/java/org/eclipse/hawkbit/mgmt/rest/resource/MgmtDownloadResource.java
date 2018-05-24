/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import java.io.InputStream;

import org.eclipse.hawkbit.artifact.repository.ArtifactRepository;
import org.eclipse.hawkbit.artifact.repository.model.AbstractDbArtifact;
import org.eclipse.hawkbit.cache.DownloadArtifactCache;
import org.eclipse.hawkbit.cache.DownloadIdCache;
import org.eclipse.hawkbit.cache.DownloadType;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDownloadRestApi;
import org.eclipse.hawkbit.rest.util.FileStreamingUtil;
import org.eclipse.hawkbit.rest.util.RequestResponseContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

/**
 * A resource for download artifacts.
 */
@RestController
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class MgmtDownloadResource implements MgmtDownloadRestApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(MgmtDownloadResource.class);

    @Autowired
    private ArtifactRepository artifactRepository;

    @Autowired
    private DownloadIdCache downloadIdCache;

    @Autowired
    private RequestResponseContextHolder requestResponseContextHolder;

    @Override
    @ResponseBody
    public ResponseEntity<InputStream> downloadArtifactByDownloadId(@PathVariable("tenant") final String tenant,
            @PathVariable("downloadId") final String downloadId) {

        try {
            final DownloadArtifactCache artifactCache = downloadIdCache.get(downloadId);
            if (artifactCache == null) {
                LOGGER.warn("Download Id {} could not be found", downloadId);
                return ResponseEntity.notFound().build();
            }

            AbstractDbArtifact artifact = null;

            if (DownloadType.BY_SHA1.equals(artifactCache.getDownloadType())) {
                artifact = artifactRepository.getArtifactBySha1(tenant, artifactCache.getId());
            } else {
                LOGGER.warn("Download Type {} not supported", artifactCache.getDownloadType());
            }

            if (artifact == null) {
                LOGGER.warn("Artifact with cached id {} and download type {} could not be found.",
                        artifactCache.getId(), artifactCache.getDownloadType());
                return ResponseEntity.notFound().build();
            }

            return FileStreamingUtil.writeFileResponse(artifact, downloadId, 0L,
                    requestResponseContextHolder.getHttpServletResponse(),
                    requestResponseContextHolder.getHttpServletRequest(), null);

        } finally {
            downloadIdCache.evict(downloadId);
        }
    }
}
