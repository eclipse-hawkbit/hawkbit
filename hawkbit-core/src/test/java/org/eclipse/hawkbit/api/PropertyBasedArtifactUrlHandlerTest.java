/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.api;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.hawkbit.api.ArtifactUrlHandlerProperties.UrlProtocol;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Lists;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Tests for creating urls to download artifacts.
 */
@Features("Unit Tests - Artifact URL Handler")
@Stories("Test to generate the artifact download URL")
@RunWith(MockitoJUnitRunner.class)
public class PropertyBasedArtifactUrlHandlerTest {

    private static final String HTTP_LOCALHOST = "http://localhost/";

    private ArtifactUrlHandler urlHandlerUnderTest;

    @Mock
    private TenantAware tenantAwareMock;

    private ArtifactUrlHandlerProperties properties;

    private static final String CONTROLLER_ID = "Test";
    private static final String fileName = "A file 1234";
    private static final Long softwareModuleId = 87654L;
    private static final Long targetId = 3474366L;
    private static final String sha1Hash = "test12345";
    private static final Long artifactId = 1345678L;
    private static final String TENANT = "TEST_TENANT";

    private static URLPlaceholder placeholder = new URLPlaceholder(CONTROLLER_ID, targetId, softwareModuleId, fileName,
            artifactId, sha1Hash);

    @Before
    public void setup() {
        properties = new ArtifactUrlHandlerProperties();
        urlHandlerUnderTest = new PropertyBasedArtifactUrlHandler(properties, tenantAwareMock);
        when(tenantAwareMock.getCurrentTenant()).thenReturn(TENANT);

    }

    @Test
    @Description("Tests the generation of http download url.")
    public void urlGenerationWithDefaultConfiguration() {
        properties.getProtocols().add(new UrlProtocol());

        final List<ArtifactUrl> ddiUrls = urlHandlerUnderTest.getUrls(placeholder, APIType.DDI);
        assertEquals(Lists.newArrayList(new ArtifactUrl("http", "download", HTTP_LOCALHOST + TENANT + "/controller/v1/"
                + CONTROLLER_ID + "/softwaremodules/" + softwareModuleId + "/artifacts/" + fileName)), ddiUrls);

        final List<ArtifactUrl> dmfUrls = urlHandlerUnderTest.getUrls(placeholder, APIType.DMF);
        assertEquals(ddiUrls, dmfUrls);
    }

    @Test
    @Description("Tests the generation of custom download url with a CoAP example that supports DMF only.")
    public void urlGenerationWithCustomConfiguration() {
        final UrlProtocol proto = new UrlProtocol();
        proto.setIp("127.0.0.1");
        proto.setPort(5683);
        proto.setProtocol("coap");
        proto.setRel("coap");
        proto.setSupports(Lists.newArrayList(APIType.DMF));
        proto.setRef("{protocol}://{ip}:{port}/fw/{tenant}/{controllerId}/sha1/{artifactSHA1}");
        properties.getProtocols().add(proto);

        List<ArtifactUrl> urls = urlHandlerUnderTest.getUrls(placeholder, APIType.DDI);

        assertThat(urls).isEmpty();
        urls = urlHandlerUnderTest.getUrls(placeholder, APIType.DMF);

        assertEquals(Lists.newArrayList(new ArtifactUrl("coap", "coap",
                "coap://127.0.0.1:5683/fw/" + TENANT + "/" + CONTROLLER_ID + "/sha1/" + sha1Hash)), urls);
    }

    @Test
    @Description("Tests the generation of custom download url with a CoAP example that supports DMF only.")
    public void urlGenerationWithCustomShortConfiguration() {
        final UrlProtocol proto = new UrlProtocol();
        proto.setIp("127.0.0.1");
        proto.setPort(5683);
        proto.setProtocol("coap");
        proto.setRel("coap");
        proto.setSupports(Lists.newArrayList(APIType.DMF));
        proto.setRef("{protocol}://{ip}:{port}/fws/{tenant}/{targetIdBase62}/{artifactIdBase62}");
        properties.getProtocols().add(proto);

        List<ArtifactUrl> urls = urlHandlerUnderTest.getUrls(placeholder, APIType.DDI);

        assertThat(urls).isEmpty();
        urls = urlHandlerUnderTest.getUrls(placeholder, APIType.DMF);

        assertEquals(Lists.newArrayList(new ArtifactUrl("coap", "coap", "coap://127.0.0.1:5683/fws/" + TENANT + "/"
                + Base62Util.fromBase10(targetId) + "/" + Base62Util.fromBase10(artifactId))), urls);
    }
}
