/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.eclipse.hawkbit.artifact.repository.model.AbstractDbArtifact;
import org.eclipse.hawkbit.cache.DownloadArtifactCache;
import org.eclipse.hawkbit.cache.DownloadIdCache;
import org.eclipse.hawkbit.cache.DownloadType;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Management API")
@Stories("Download Resource")
public class MgmtDownloadResourceTest extends AbstractManagementApiIntegrationTest {

    @Autowired
    private DownloadIdCache downloadIdCache;

    private static final String DOWNLOADID_SHA1 = "downloadIdSha1";

    private static final String DOWNLOADID_NOT_AVAILABLE = "downloadIdNotAvailable";

    private Artifact artifact;
    private AbstractDbArtifact file;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);

    @Before
    public void setup() {

        final DistributionSet distributionSet = testdataFactory.createDistributionSet("Test");
        final SoftwareModule softwareModule = distributionSet.getModules().stream().findAny().get();
        artifact = testdataFactory.createArtifacts(softwareModule.getId()).stream().findAny().get();
        file = artifactManagement.loadArtifactBinary(artifact.getSha1Hash()).get();

        downloadIdCache.put(DOWNLOADID_SHA1, new DownloadArtifactCache(DownloadType.BY_SHA1, artifact.getSha1Hash()));
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    @Test
    @Description("This test verifies the call of download artifact without a valid download id fails.")
    public void testNoDownloadIdAvailable() throws Exception {
        mvc.perform(get(
                MgmtRestConstants.DOWNLOAD_ID_V1_REQUEST_MAPPING_BASE
                        + MgmtRestConstants.DOWNLOAD_ID_V1_REQUEST_MAPPING,
                tenantAware.getCurrentTenant(), DOWNLOADID_NOT_AVAILABLE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

    }

    @Test
    @Description("This test verifies the call of download artifact works and the download id will be removed.")
    public void testDownload() throws Exception {
        mvc.perform(get(
                MgmtRestConstants.DOWNLOAD_ID_V1_REQUEST_MAPPING_BASE
                        + MgmtRestConstants.DOWNLOAD_ID_V1_REQUEST_MAPPING,
                tenantAware.getCurrentTenant(), DOWNLOADID_SHA1)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(header().string("Accept-Ranges", "bytes"))
                .andExpect(header().string("Last-Modified", dateFormat.format(new Date(artifact.getCreatedAt()))))
                .andExpect(header().string("Content-Disposition", "attachment;filename=" + DOWNLOADID_SHA1));

        // because cache is empty
        mvc.perform(get(
                MgmtRestConstants.DOWNLOAD_ID_V1_REQUEST_MAPPING_BASE
                        + MgmtRestConstants.DOWNLOAD_ID_V1_REQUEST_MAPPING,
                tenantAware.getCurrentTenant(), DOWNLOADID_SHA1)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

    }

}
