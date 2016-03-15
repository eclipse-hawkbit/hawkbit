/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.eclipse.hawkbit.AbstractIntegrationTestWithMongoDB;
import org.eclipse.hawkbit.MockMvcResultPrinter;
import org.eclipse.hawkbit.TestDataUtil;
import org.eclipse.hawkbit.cache.CacheConstants;
import org.eclipse.hawkbit.cache.DownloadArtifactCache;
import org.eclipse.hawkbit.cache.DownloadType;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.context.annotation.Description;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Management API")
@Stories("Download Resource")
public class DownloadResourceTest extends AbstractIntegrationTestWithMongoDB {

    @Autowired
    @Qualifier(CacheConstants.DOWNLOAD_ID_CACHE)
    private Cache downloadIdCache;

    private final String downloadIdSha1 = "downloadIdSha1";

    private final String downloadIdNotAvailable = "downloadIdNotAvailable";

    @Before
    public void setupCache() {

        final DistributionSet distributionSet = TestDataUtil.generateDistributionSet("Test", softwareManagement,
                distributionSetManagement);
        final SoftwareModule softwareModule = distributionSet.getModules().stream().findFirst().get();
        final Artifact artifact = TestDataUtil.generateArtifacts(artifactManagement, softwareModule.getId()).stream()
                .findFirst().get();

        downloadIdCache.put(downloadIdSha1, new DownloadArtifactCache(DownloadType.BY_SHA1, artifact.getSha1Hash()));
    }

    @Test
    @Description("This test verifies the call of download artifact without a valid download id fails.")
    public void testNoDownloadIdAvailable() throws Exception {
        mvc.perform(
                get(RestConstants.DOWNLOAD_ID_V1_REQUEST_MAPPING_BASE + RestConstants.DOWNLOAD_ID_V1_REQUEST_MAPPING,
                        downloadIdNotAvailable))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

    }

    @Test
    @Description("This test verifies the call of download artifact works and the download id will be removed.")
    public void testDownload() throws Exception {
        mvc.perform(
                get(RestConstants.DOWNLOAD_ID_V1_REQUEST_MAPPING_BASE + RestConstants.DOWNLOAD_ID_V1_REQUEST_MAPPING,
                        downloadIdSha1))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        // because cache is empty
        mvc.perform(
                get(RestConstants.DOWNLOAD_ID_V1_REQUEST_MAPPING_BASE + RestConstants.DOWNLOAD_ID_V1_REQUEST_MAPPING,
                        downloadIdSha1))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

    }

}
