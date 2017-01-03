/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.client.resource;

import java.io.InputStream;

import org.eclipse.hawkbit.ddi.dl.rest.api.DdiDlArtifactStoreControllerRestApi;
import org.eclipse.hawkbit.ddi.dl.rest.api.DdiDlRestConstants;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Client binding for the artifact store controller resource of the DDI-DL API.
 */
@FeignClient(name = "DdiDlArtifactStoreControllerClient", url = "${hawkbit.url:localhost:8080}")
public interface DdiDlArtifactStoreControllerResourceClient extends DdiDlArtifactStoreControllerRestApi {

    @Override
    @RequestMapping(method = RequestMethod.GET, value = DdiDlRestConstants.ARTIFACT_DOWNLOAD_BY_FILENAME
            + "/{fileName}")
    ResponseEntity<InputStream> downloadArtifactByFilename(@PathVariable("tenant") final String tenant,
            @PathVariable("fileName") final String fileName,
            @RequestParam(value = "principal", required = false) final Object principal);

}
