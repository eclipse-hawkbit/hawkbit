/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.util;

import static org.junit.Assert.assertEquals;

import org.eclipse.hawkbit.AmqpTestConfiguration;
import org.eclipse.hawkbit.api.ArtifactUrlHandler;
import org.eclipse.hawkbit.api.UrlProtocol;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.util.AbstractIntegrationTestWithMongoDB;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Tests for creating urls to download artifacts.
 */
@Features("Component Tests - Artifact URL Handler")
@Stories("Test to generate the artifact download URL")
@SpringApplicationConfiguration(classes = { AmqpTestConfiguration.class,
        org.eclipse.hawkbit.RepositoryApplicationConfiguration.class })

public class PropertyBasedArtifactUrlHandlerTest extends AbstractIntegrationTestWithMongoDB {

    private static final String HTTPS_LOCALHOST = "https://localhost:8080/";
    private static final String HTTP_LOCALHOST = "http://localhost:8080/";

    @Autowired
    private ArtifactUrlHandler urlHandlerProperties;

    private LocalArtifact localArtifact;
    private static final String CONTROLLER_ID = "Test";
    private String fileName;
    private Long softwareModuleId;
    private String sha1Hash;

    @Before
    public void setup() {
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        final SoftwareModule module = dsA.getModules().iterator().next();
        localArtifact = testdataFactory.createLocalArtifacts(module.getId()).stream().findAny().get();
        softwareModuleId = localArtifact.getSoftwareModule().getId();
        fileName = localArtifact.getFilename();
        sha1Hash = localArtifact.getSha1Hash();

    }

    @Test
    @Description("Tests the generation of http download url.")
    public void testHttpUrl() {

        final String url = urlHandlerProperties.getUrl(CONTROLLER_ID, softwareModuleId, fileName, sha1Hash,
                UrlProtocol.HTTP);
        assertEquals("http is build incorrect",
                HTTP_LOCALHOST + tenantAware.getCurrentTenant() + "/controller/v1/" + CONTROLLER_ID
                        + "/softwaremodules/" + localArtifact.getSoftwareModule().getId() + "/artifacts/"
                        + localArtifact.getFilename(),
                url);
    }

    @Test
    @Description("Tests the generation of https download url.")
    public void testHttpsUrl() {
        final String url = urlHandlerProperties.getUrl(CONTROLLER_ID, softwareModuleId, fileName, sha1Hash,
                UrlProtocol.HTTPS);
        assertEquals("https is build incorrect",
                HTTPS_LOCALHOST + tenantAware.getCurrentTenant() + "/controller/v1/" + CONTROLLER_ID
                        + "/softwaremodules/" + localArtifact.getSoftwareModule().getId() + "/artifacts/"
                        + localArtifact.getFilename(),
                url);
    }

    @Test
    @Description("Tests the generation of coap download url.")
    public void testCoapUrl() {
        final String url = urlHandlerProperties.getUrl(CONTROLLER_ID, softwareModuleId, fileName, sha1Hash,
                UrlProtocol.COAP);

        assertEquals("coap is build incorrect", "coap://127.0.0.1:5683/fw/" + tenantAware.getCurrentTenant() + "/"
                + CONTROLLER_ID + "/sha1/" + localArtifact.getSha1Hash(), url);
    }
}
