/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.hawkbit.artifact.repository.ArtifactRepository;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;
import org.eclipse.hawkbit.cache.DownloadArtifactCache;
import org.eclipse.hawkbit.cache.DownloadIdCache;
import org.eclipse.hawkbit.cache.DownloadType;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDownloadRestApi;
import org.eclipse.hawkbit.rest.util.RequestResponseContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import com.google.common.io.ByteStreams;
import com.google.common.net.HttpHeaders;

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
    public ResponseEntity<Void> downloadArtifactByDownloadId(@PathVariable("tenant") final String tenant,
            @PathVariable("downloadId") final String downloadId) {
        try {
            final DownloadArtifactCache artifactCache = downloadIdCache.get(downloadId);
            if (artifactCache == null) {
                LOGGER.warn("Download Id {} could not be found", downloadId);
                return ResponseEntity.notFound().build();
            }

            DbArtifact artifact = null;

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

            final HttpServletResponse response = requestResponseContextHolder.getHttpServletResponse();
            final String etag = artifact.getHashes().getSha1();
            final long length = artifact.getSize();
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + downloadId);
            response.setHeader(HttpHeaders.ETAG, etag);
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setContentLengthLong(length);

            try (InputStream inputstream = artifact.getFileInputStream()) {
                ByteStreams.copy(inputstream, requestResponseContextHolder.getHttpServletResponse().getOutputStream());
            } catch (final IOException e) {
                LOGGER.error("Cannot copy streams", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } finally {
            downloadIdCache.evict(downloadId);
        }

        return ResponseEntity.ok().build();
    }
}
