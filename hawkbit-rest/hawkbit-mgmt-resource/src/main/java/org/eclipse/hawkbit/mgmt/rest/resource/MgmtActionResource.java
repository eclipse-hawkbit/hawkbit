/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.action.MgmtAction;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtActionRestApi;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.model.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MgmtActionResource implements MgmtActionRestApi {

    private static final Logger LOG = LoggerFactory.getLogger(MgmtActionResource.class);

    private final DeploymentManagement deploymentManagement;

    MgmtActionResource(final DeploymentManagement deploymentManagement) {
        this.deploymentManagement = deploymentManagement;
    }

    @Override
    public ResponseEntity<PagedList<MgmtAction>> getActions(final int pagingOffsetParam, final int pagingLimitParam,
            final String sortParam, final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeActionSortParam(sortParam);
        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Slice<Action> actions;
        final Long totalActionCount;
        if (rsqlParam != null) {
            actions = this.deploymentManagement.findActionsAll(pageable);
            totalActionCount = this.deploymentManagement.countActionsAll();
        } else {
            actions = this.deploymentManagement.findActionsAll(pageable);
            totalActionCount = this.deploymentManagement.countActionsAll();
        }

        return ResponseEntity.ok(new PagedList<>(MgmtActionMapper.toResponse(actions.getContent()), totalActionCount));
    }

}
