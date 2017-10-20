/**
 * Copyright (c) Siemens AG, 2017
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.offline.update;

import javax.validation.Valid;

import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.offline.update.model.OfflineUpdateData;
import org.eclipse.hawkbit.offline.update.rest.resource.OfflineUpdateController;
import org.eclipse.hawkbit.offline.update.util.OfflineUpdateConstants;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST resource handling for {@link OfflineUpdateController}.
 */
@RequestMapping(value = OfflineUpdateConstants.BASE_V1_REQUEST_MAPPING)
@FunctionalInterface
public interface OfflineUpdateControllerRestApi {

    /**
     * This end point records the offline software update data for a set of
     * {@link Target}s. The method creates a set of {@link SoftwareModule}s and
     * a {@link DistributionSet} based on the provided offline update data. It
     * then assigns the created {@link DistributionSet} to the {@link Target}s,
     * and finishes the corresponding {@link Action}s.
     *
     * @param offlineUpdateInfo
     *            is the offline update data to be recorded for a set of
     *            targets. The data is expected in JSON format represented by
     *            JSON model defined in {@link OfflineUpdateData}
     * @param files
     *            are the set of files to be uploaded as artifacts. Even if no
     *            files are provided, a distribution set will still be created
     *            with a software module containing no artifact
     *
     * @return new {@link MgmtDistributionSet} created.
     */
    @RequestMapping(value = OfflineUpdateConstants.UPDATE_OFFLINE_TARGET, method = RequestMethod.POST, produces = {
            MediaType.APPLICATION_JSON_VALUE, MediaTypes.HAL_JSON_VALUE })
    ResponseEntity<MgmtDistributionSet> recordOfflineUpdate(
            @Valid @RequestParam("offlineUpdateInfo") final String offlineUpdateInfo,
            @RequestParam("file") final MultipartFile[] files);
}
