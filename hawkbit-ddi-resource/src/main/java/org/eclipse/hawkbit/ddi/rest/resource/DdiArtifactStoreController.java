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
import java.io.InputStream;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;
import org.eclipse.hawkbit.ddi.dl.rest.api.DdiDlArtifactStoreControllerRestApi;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.rest.util.RequestResponseContextHolder;
import org.eclipse.hawkbit.rest.util.RestResourceConversionHelper;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.eclipse.hawkbit.util.IpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

/**
 * The {@link DdiArtifactStoreController} of the HawkBit server controller API
 * that is queried by the HawkBit target in order to download artifacts
 * independent of their own individual resource. This is offered in addition to
 * the {@link DdiRootController#downloadArtifact(String, Long, String)} for
 * legacy controllers that can not be fed with a download URI at runtime.
 */
@RestController
@Scope(WebApplicationContext.SCOPE_REQUEST)
public class DdiArtifactStoreController implements DdiDlArtifactStoreControllerRestApi {

    private static final Logger LOG = LoggerFactory.getLogger(DdiArtifactStoreController.class);

    @Autowired
    private ArtifactManagement artifactManagement;

    @Autowired
    private ControllerManagement controllerManagement;

    @Autowired
    private HawkbitSecurityProperties securityProperties;

    @Autowired
    private RequestResponseContextHolder requestResponseContextHolder;

    @Autowired
    private EntityFactory entityFactory;

    @Override
    public ResponseEntity<InputStream> downloadArtifactByFilename(@PathVariable("tenant") final String tenant,
            @PathVariable("fileName") final String fileName, @AuthenticationPrincipal final String targetid) {
        final List<LocalArtifact> foundArtifacts = artifactManagement.findLocalArtifactByFilename(fileName);

        if (foundArtifacts.isEmpty()) {
            LOG.warn("Software artifact with name {} could not be found.", fileName);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        if (foundArtifacts.size() > 1) {
            LOG.warn("Software artifact name {} is not unique. We will use the first entry.", fileName);
        }
        ResponseEntity<InputStream> result;
        final LocalArtifact artifact = foundArtifacts.get(0);

        final String ifMatch = requestResponseContextHolder.getHttpServletRequest().getHeader("If-Match");
        if (ifMatch != null && !RestResourceConversionHelper.matchesHttpHeader(ifMatch, artifact.getSha1Hash())) {
            result = new ResponseEntity<>(HttpStatus.PRECONDITION_FAILED);
        } else {
            final DbArtifact file = artifactManagement.loadLocalArtifactBinary(artifact);

            // we set a download status only if we are aware of the
            // targetid, i.e. authenticated and not anonymous
            if (targetid != null && !"anonymous".equals(targetid)) {
                final ActionStatus actionStatus = checkAndReportDownloadByTarget(
                        requestResponseContextHolder.getHttpServletRequest(), targetid, artifact);
                result = RestResourceConversionHelper.writeFileResponse(artifact,
                        requestResponseContextHolder.getHttpServletResponse(),
                        requestResponseContextHolder.getHttpServletRequest(), file, controllerManagement,
                        actionStatus.getId());
            } else {
                result = RestResourceConversionHelper.writeFileResponse(artifact,
                        requestResponseContextHolder.getHttpServletResponse(),
                        requestResponseContextHolder.getHttpServletRequest(), file);
            }

        }
        return result;
    }

    @Override
    public ResponseEntity<Void> downloadArtifactMD5ByFilename(@PathVariable("tenant") final String tenant,
            @PathVariable("fileName") final String fileName) {
        final List<LocalArtifact> foundArtifacts = artifactManagement.findLocalArtifactByFilename(fileName);

        if (foundArtifacts.isEmpty()) {
            LOG.warn("Softeare artifact with name {} could not be found.", fileName);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else if (foundArtifacts.size() > 1) {
            LOG.error("Softeare artifact name {} is not unique.", fileName);
        }

        try {
            DataConversionHelper.writeMD5FileResponse(fileName, requestResponseContextHolder.getHttpServletResponse(),
                    foundArtifacts.get(0));
        } catch (final IOException e) {
            LOG.error("Failed to stream MD5 File", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private ActionStatus checkAndReportDownloadByTarget(final HttpServletRequest request, final String targetid,
            final LocalArtifact artifact) {
        final Target target = controllerManagement.updateLastTargetQuery(targetid,
                IpUtil.getClientIpFromRequest(request, securityProperties));

        final Action action = controllerManagement
                .getActionForDownloadByTargetAndSoftwareModule(target.getControllerId(), artifact.getSoftwareModule());
        final String range = request.getHeader("Range");

        final ActionStatus actionStatus = entityFactory.generateActionStatus();
        actionStatus.setAction(action);
        actionStatus.setOccurredAt(System.currentTimeMillis());
        actionStatus.setStatus(Status.DOWNLOAD);

        if (range != null) {
            actionStatus.addMessage(RepositoryConstants.SERVER_MESSAGE_PREFIX + "Target downloads range " + range
                    + " of: " + request.getRequestURI());
        } else {
            actionStatus.addMessage(
                    RepositoryConstants.SERVER_MESSAGE_PREFIX + "Target downloads: " + request.getRequestURI());
        }

        return controllerManagement.addInformationalActionStatus(actionStatus);
    }

}
