/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.controller;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;
import org.eclipse.hawkbit.cache.CacheWriteNotify;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.rest.resource.helper.RestResourceConversionHelper;
import org.eclipse.hawkbit.util.IpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * The {@link ArtifactStoreController} of the SP server controller API that is
 * queried by the SP target in order to download artifacts independent of their
 * own individual resource. This is offered in addition to the
 * {@link RootController#downloadArtifact(String, Long, Long, javax.servlet.http.HttpServletResponse)}
 * for legacy controllers that can not be fed with a download URI at runtime.
 *
 *
 *
 *
 *
 */
@RestController
@RequestMapping(ControllerConstants.ARTIFACTS_V1_REQUEST_MAPPING)
public class ArtifactStoreController implements EnvironmentAware {
    private static final Logger LOG = LoggerFactory.getLogger(ArtifactStoreController.class);

    @Autowired
    private ArtifactManagement artifactManagement;

    @Autowired
    private ControllerManagement controllerManagement;

    @Autowired
    private CacheWriteNotify cacheWriteNotify;

    private static final String SP_SERVER_CONFIG_PREFIX = "hawkbit.server.";
    private RelaxedPropertyResolver environment;

    @Override
    public void setEnvironment(final Environment environment) {
        this.environment = new RelaxedPropertyResolver(environment, SP_SERVER_CONFIG_PREFIX);

    }

    /**
     * Handles GET {@link Artifact} download request. This could be full or
     * partial download request.
     *
     * @param fileName
     *            to search for
     * @param response
     *            to write to
     * @param request
     *            from the client
     * @param targetid
     *            of authenticated target
     *
     * @return response of the servlet which in case of success is status code
     *         {@link HttpStatus#OK} or in case of partial download
     *         {@link HttpStatus#PARTIAL_CONTENT}.
     */
    @RequestMapping(method = RequestMethod.GET, value = ControllerConstants.ARTIFACT_DOWNLOAD_BY_FILENAME
            + "/{fileName}")
    @ResponseBody
    public ResponseEntity<Void> downloadArtifactByFilename(@PathVariable final String fileName,
            final HttpServletResponse response, final HttpServletRequest request,
            @AuthenticationPrincipal final String targetid) {
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
                // targetid, i.e.
                // authenticated and not anonymous
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

    private Action checkAndReportDownloadByTarget(final HttpServletRequest request, final String targetid,
            final LocalArtifact artifact) {
        final Target target = controllerManagement.updateLastTargetQuery(targetid, IpUtil.getClientIpFromRequest(
                request, environment.getProperty("security.rp.remote_ip_header", String.class, "X-Forwarded-For")));

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

    /**
     * Handles GET {@link Artifact} MD5 checksum file download request.
     *
     * @param fileName
     *            to search for
     * @param response
     *            to write to
     *
     * @return response of the servlet
     */
    @RequestMapping(method = RequestMethod.GET, value = ControllerConstants.ARTIFACT_DOWNLOAD_BY_FILENAME
            + "/{fileName}" + ControllerConstants.ARTIFACT_MD5_DWNL_SUFFIX)
    @ResponseBody
    public ResponseEntity<Void> downloadArtifactMD5ByFilename(@PathVariable final String fileName,
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

}
