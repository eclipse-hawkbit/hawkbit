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
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;
import org.eclipse.hawkbit.ddi.dl.rest.api.DdiDlArtifactStoreControllerRestApi;
import org.eclipse.hawkbit.im.authentication.UserPrincipal;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.Artifact;
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
            @PathVariable("fileName") final String fileName, @AuthenticationPrincipal final Object principal) {
        final Optional<Artifact> foundArtifacts = artifactManagement.findArtifactByFilename(fileName);

        if (!foundArtifacts.isPresent()) {
            LOG.warn("Software artifact with name {} could not be found.", fileName);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        ResponseEntity<InputStream> result;
        final Artifact artifact = foundArtifacts.get();

        final String ifMatch = requestResponseContextHolder.getHttpServletRequest().getHeader("If-Match");
        if (ifMatch != null && !RestResourceConversionHelper.matchesHttpHeader(ifMatch, artifact.getSha1Hash())) {
            result = new ResponseEntity<>(HttpStatus.PRECONDITION_FAILED);
        } else {
            final DbArtifact file = artifactManagement.loadArtifactBinary(artifact.getSha1Hash());

            // we set a download status only if we are aware of the
            // targetid, i.e. authenticated and not anonymous
            if (principal instanceof UserPrincipal && ((UserPrincipal) principal).getUsername() != null
                    && !"anonymous".equals(((UserPrincipal) principal).getUsername())) {
                final ActionStatus actionStatus = checkAndReportDownloadByTarget(
                        requestResponseContextHolder.getHttpServletRequest(), ((UserPrincipal) principal).getUsername(),
                        artifact);
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
        final Optional<Artifact> foundArtifacts = artifactManagement.findArtifactByFilename(fileName);

        if (!foundArtifacts.isPresent()) {
            LOG.warn("Software artifact with name {} could not be found.", fileName);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        try {
            DataConversionHelper.writeMD5FileResponse(fileName, requestResponseContextHolder.getHttpServletResponse(),
                    foundArtifacts.get());
        } catch (final IOException e) {
            LOG.error("Failed to stream MD5 File", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private ActionStatus checkAndReportDownloadByTarget(final HttpServletRequest request, final String targetid,
            final Artifact artifact) {
        final Target target = controllerManagement.updateLastTargetQuery(targetid,
                IpUtil.getClientIpFromRequest(request, securityProperties));

        final Action action = controllerManagement
                .getActionForDownloadByTargetAndSoftwareModule(target.getControllerId(), artifact.getSoftwareModule());
        final String range = request.getHeader("Range");

        String message;
        if (range != null) {
            message = RepositoryConstants.SERVER_MESSAGE_PREFIX + "Target downloads range " + range + " of: "
                    + request.getRequestURI();
        } else {
            message = RepositoryConstants.SERVER_MESSAGE_PREFIX + "Target downloads: " + request.getRequestURI();
        }

        return controllerManagement.addInformationalActionStatus(
                entityFactory.actionStatus().create(action.getId()).status(Status.DOWNLOAD).message(message));
    }

}
