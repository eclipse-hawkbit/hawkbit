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

import java.util.List;

import org.eclipse.hawkbit.api.ArtifactUrlHandlerProperties.UrlProtocol;
import org.eclipse.hawkbit.api.URLPlaceholder.SoftwareData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

    private static final String TEST_PROTO = "coap";
    private static final String TEST_REL = "download-udp";

    private static final long TENANT_ID = 456789L;
    private static final String CONTROLLER_ID = "Test";
    private static final String FILENAME = "Afile1234";
    private static final long SOFTWAREMODULEID = 87654L;
    private static final long TARGETID = 3474366L;
    private static final String TARGETID_BASE62 = "EZqA";
    private static final String SHA1HASH = "test12345";
    private static final long ARTIFACTID = 1345678L;
    private static final String ARTIFACTID_BASE10 = "5e4U";
    private static final String TENANT = "TEST_TENANT";

    private static final String HTTP_LOCALHOST = "http://localhost:8080/";

    private ArtifactUrlHandler urlHandlerUnderTest;

    private ArtifactUrlHandlerProperties properties;

    private static URLPlaceholder placeholder = new URLPlaceholder(TENANT, TENANT_ID, CONTROLLER_ID, TARGETID,
            new SoftwareData(SOFTWAREMODULEID, FILENAME, ARTIFACTID, SHA1HASH));

    @Before
    public void setup() {
        properties = new ArtifactUrlHandlerProperties();
        urlHandlerUnderTest = new PropertyBasedArtifactUrlHandler(properties);

    }

    @Test
    @Description("Tests the generation of http download url.")
    public void urlGenerationWithDefaultConfiguration() {
        properties.getProtocols().put("download-http", new UrlProtocol());

        final List<ArtifactUrl> ddiUrls = urlHandlerUnderTest.getUrls(placeholder, APIType.DDI);
        assertEquals(
                Lists.newArrayList(new ArtifactUrl("http", "download-http", HTTP_LOCALHOST + TENANT + "/controller/v1/"
                        + CONTROLLER_ID + "/softwaremodules/" + SOFTWAREMODULEID + "/artifacts/" + FILENAME)),
                ddiUrls);

        final List<ArtifactUrl> dmfUrls = urlHandlerUnderTest.getUrls(placeholder, APIType.DMF);
        assertEquals(ddiUrls, dmfUrls);
    }

    @Test
    @Description("Tests the generation of custom download url with a CoAP example that supports DMF only.")
    public void urlGenerationWithCustomConfiguration() {
        final UrlProtocol proto = new UrlProtocol();
        proto.setIp("127.0.0.1");
        proto.setPort(5683);
        proto.setProtocol(TEST_PROTO);
        proto.setRel(TEST_REL);
        proto.setSupports(Lists.newArrayList(APIType.DMF));
        proto.setRef("{protocol}://{ip}:{port}/fw/{tenant}/{controllerId}/sha1/{artifactSHA1}");
        properties.getProtocols().put(TEST_PROTO, proto);

        List<ArtifactUrl> urls = urlHandlerUnderTest.getUrls(placeholder, APIType.DDI);

        assertThat(urls).isEmpty();
        urls = urlHandlerUnderTest.getUrls(placeholder, APIType.DMF);

        assertEquals(Lists.newArrayList(new ArtifactUrl(TEST_PROTO, TEST_REL,
                "coap://127.0.0.1:5683/fw/" + TENANT + "/" + CONTROLLER_ID + "/sha1/" + SHA1HASH)), urls);
    }

    @Test
    @Description("Tests the generation of custom download url using Base62 references with a CoAP example that supports DMF only.")
    public void urlGenerationWithCustomShortConfiguration() {
        final UrlProtocol proto = new UrlProtocol();
        proto.setIp("127.0.0.1");
        proto.setPort(5683);
        proto.setProtocol(TEST_PROTO);
        proto.setRel(TEST_REL);
        proto.setSupports(Lists.newArrayList(APIType.DMF));
        proto.setRef("{protocol}://{ip}:{port}/fws/{tenant}/{targetIdBase62}/{artifactIdBase62}");
        properties.getProtocols().put("ftp", proto);

        List<ArtifactUrl> urls = urlHandlerUnderTest.getUrls(placeholder, APIType.DDI);

        assertThat(urls).isEmpty();
        urls = urlHandlerUnderTest.getUrls(placeholder, APIType.DMF);

        assertEquals(Lists.newArrayList(new ArtifactUrl(TEST_PROTO, TEST_REL,
                TEST_PROTO + "://127.0.0.1:5683/fws/" + TENANT + "/" + TARGETID_BASE62 + "/" + ARTIFACTID_BASE10)),
                urls);
    }
}
