/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.rest.resource;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;
import org.eclipse.hawkbit.cache.CacheWriteNotify;
import org.eclipse.hawkbit.ddi.rest.api.DdiArtifactStoreControllerRestApi;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.rest.util.RestResourceConversionHelper;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.eclipse.hawkbit.util.IpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * The {@link DdiArtifactStoreController} of the HawkBit server controller API
 * that is queried by the HawkBit target in order to download artifacts
 * independent of their own individual resource. This is offered in addition to
 * the
 * {@link DdiRootController#downloadArtifact(String, Long, Long, javax.servlet.http.HttpServletResponse)}
 * for legacy controllers that can not be fed with a download URI at runtime.
 */
@RestController
public class DdiArtifactStoreController implements DdiArtifactStoreControllerRestApi {

    private static final Logger LOG = LoggerFactory.getLogger(DdiArtifactStoreController.class);

    @Autowired
    private ArtifactManagement artifactManagement;

    @Autowired
    private ControllerManagement controllerManagement;

    @Autowired
    private CacheWriteNotify cacheWriteNotify;

    @Autowired
    private HawkbitSecurityProperties securityProperties;

    @Override
    public ResponseEntity<Void> downloadArtifactByFilename(final String fileName, final HttpServletResponse response,
            final HttpServletRequest request, final String targetid) {
        ResponseEntity<Void> result;

        final List<LocalArtifact> foundArtifacts = artifactManagement.findLocalArtifactByFilename(fileName);

        if (foundArtifacts.isEmpty()) {
            LOG.warn("Software artifact with name {} could not be found.", fileName);
            result = new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else {
            if (foundArtifacts.size() > 1) {
                LOG.warn("Software artifact name {} is not unique. We will use the first entry.", fileName);
            }

            final LocalArtifact artifact = foundArtifacts.get(0);

            final String ifMatch = request.getHeader("If-Match");
            if (ifMatch != null && !RestResourceConversionHelper.matchesHttpHeader(ifMatch, artifact.getSha1Hash())) {
                result = new ResponseEntity<>(HttpStatus.PRECONDITION_FAILED);
            } else {
                final DbArtifact file = artifactManagement.loadLocalArtifactBinary(artifact);

                // we set a download status only if we are aware of the
                // targetid, i.e. authenticated and not anonymous
                if (targetid != null && !"anonymous".equals(targetid)) {
                    final Action action = checkAndReportDownloadByTarget(request, targetid, artifact);
                    result = RestResourceConversionHelper.writeFileResponse(artifact, response, request, file,
                            cacheWriteNotify, action.getId());
                } else {
                    result = RestResourceConversionHelper.writeFileResponse(artifact, response, request, file);
                }

            }
        }
        return result;
    }

    @Override
    public ResponseEntity<Void> downloadArtifactMD5ByFilename(final String fileName,
            final HttpServletResponse response) {
        final List<LocalArtifact> foundArtifacts = artifactManagement.findLocalArtifactByFilename(fileName);

        if (foundArtifacts.isEmpty()) {
            LOG.warn("Softeare artifact with name {} could not be found.", fileName);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else if (foundArtifacts.size() > 1) {
            LOG.error("Softeare artifact name {} is not unique.", fileName);
        }

        try {
            DataConversionHelper.writeMD5FileResponse(fileName, response, foundArtifacts.get(0));
        } catch (final IOException e) {
            LOG.error("Failed to stream MD5 File", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private Action checkAndReportDownloadByTarget(final HttpServletRequest request, final String targetid,
            final LocalArtifact artifact) {
        final Target target = controllerManagement.updateLastTargetQuery(targetid,
                IpUtil.getClientIpFromRequest(request, securityProperties.getClients().getRemoteIpHeader()));

        final Action action = controllerManagement
                .getActionForDownloadByTargetAndSoftwareModule(target.getControllerId(), artifact.getSoftwareModule());
        final String range = request.getHeader("Range");

        final ActionStatus actionStatus = new ActionStatus();
        actionStatus.setAction(action);
        actionStatus.setOccurredAt(System.currentTimeMillis());
        actionStatus.setStatus(Status.DOWNLOAD);

        if (range != null) {
            actionStatus.addMessage("It is a partial download request: " + range);
        } else {
            actionStatus.addMessage("Target downloads");
        }
        controllerManagement.addActionStatusMessage(actionStatus);
        return action;
    }

}
