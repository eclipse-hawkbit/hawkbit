/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.hawkbit.api.ArtifactUrlHandlerProperties.UrlProtocol;
import org.eclipse.hawkbit.api.URLPlaceholder.SoftwareData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for creating urls to download artifacts.
 */
@Feature("Unit Tests - Artifact URL Handler")
@Story("Test to generate the artifact download URL")
@ExtendWith(MockitoExtension.class)
public class PropertyBasedArtifactUrlHandlerTest {

    private static final String TEST_PROTO = "coap";
    private static final String TEST_REL = "download-udp";

    private static final long TENANT_ID = 456789L;
    private static final String CONTROLLER_ID = "Test";
    private static final String FILENAME_DECODE = "test123!§$%&";
    private static final String FILENAME_ENCODE = "test123%21%C2%A7%24%25%26";
    private static final long SOFTWAREMODULEID = 87654L;
    private static final long TARGETID = 3474366L;
    private static final String TARGETID_BASE62 = "EZqA";
    private static final String SHA1HASH = "test12345";
    private static final long ARTIFACTID = 1345678L;
    private static final String ARTIFACTID_BASE62 = "5e4U";
    private static final String TENANT = "TEST_TENANT";

    private static final String HTTP_LOCALHOST = "http://localhost:8080/";

    private ArtifactUrlHandler urlHandlerUnderTest;

    private ArtifactUrlHandlerProperties properties;

    private static URLPlaceholder placeholder = new URLPlaceholder(TENANT, TENANT_ID, CONTROLLER_ID, TARGETID,
            new SoftwareData(SOFTWAREMODULEID, FILENAME_DECODE, ARTIFACTID, SHA1HASH));

    @BeforeEach
    public void setup() {
        properties = new ArtifactUrlHandlerProperties();
        urlHandlerUnderTest = new PropertyBasedArtifactUrlHandler(properties);

    }

    @Test
    @Description("Tests the generation of http download url.")
    public void urlGenerationWithDefaultConfiguration() {
        properties.getProtocols().put("download-http", new UrlProtocol());

        final List<ArtifactUrl> ddiUrls = urlHandlerUnderTest.getUrls(placeholder, ApiType.DDI);
        assertThat(ddiUrls).containsExactly(
                new ArtifactUrl("http".toUpperCase(), "download-http", HTTP_LOCALHOST + TENANT + "/controller/v1/"
                        + CONTROLLER_ID + "/softwaremodules/" + SOFTWAREMODULEID + "/artifacts/" + FILENAME_ENCODE));

        final List<ArtifactUrl> dmfUrls = urlHandlerUnderTest.getUrls(placeholder, ApiType.DMF);
        assertThat(ddiUrls).isEqualTo(dmfUrls);
    }

    @Test
    @Description("Tests the generation of custom download url with a CoAP example that supports DMF only.")
    public void urlGenerationWithCustomConfiguration() {
        final UrlProtocol proto = new UrlProtocol();
        proto.setIp("127.0.0.1");
        proto.setPort(5683);
        proto.setProtocol(TEST_PROTO);
        proto.setRel(TEST_REL);
        proto.setSupports(Arrays.asList(ApiType.DMF));
        proto.setRef("{protocol}://{ip}:{port}/fw/{tenant}/{controllerId}/sha1/{artifactSHA1}");
        properties.getProtocols().put(TEST_PROTO, proto);

        List<ArtifactUrl> urls = urlHandlerUnderTest.getUrls(placeholder, ApiType.DDI);

        assertThat(urls).isEmpty();
        urls = urlHandlerUnderTest.getUrls(placeholder, ApiType.DMF);

        assertThat(urls).containsExactly(new ArtifactUrl(TEST_PROTO.toUpperCase(), TEST_REL,
                "coap://127.0.0.1:5683/fw/" + TENANT + "/" + CONTROLLER_ID + "/sha1/" + SHA1HASH));
    }

    @Test
    @Description("Tests the generation of custom download url using Base62 references with a CoAP example that supports DMF only.")
    public void urlGenerationWithCustomShortConfiguration() {
        final UrlProtocol proto = new UrlProtocol();
        proto.setIp("127.0.0.1");
        proto.setPort(5683);
        proto.setProtocol(TEST_PROTO);
        proto.setRel(TEST_REL);
        proto.setSupports(Arrays.asList(ApiType.DMF));
        proto.setRef("{protocol}://{ip}:{port}/fws/{tenant}/{targetIdBase62}/{artifactIdBase62}");
        properties.getProtocols().put("ftp", proto);

        List<ArtifactUrl> urls = urlHandlerUnderTest.getUrls(placeholder, ApiType.DDI);

        assertThat(urls).isEmpty();
        urls = urlHandlerUnderTest.getUrls(placeholder, ApiType.DMF);

        assertThat(urls).containsExactly(new ArtifactUrl(TEST_PROTO.toUpperCase(), TEST_REL,
                TEST_PROTO + "://127.0.0.1:5683/fws/" + TENANT + "/" + TARGETID_BASE62 + "/" + ARTIFACTID_BASE62));
    }

    @Test
    @Description("Verfies that the full qualified host of the statically defined hostname is replaced with the host of the request.")
    public void urlGenerationWithHostFromRequest() throws URISyntaxException {
        final String testHost = "ddi.host.com";

        final UrlProtocol proto = new UrlProtocol();
        proto.setIp("127.0.0.1");
        proto.setPort(5683);
        proto.setProtocol(TEST_PROTO);
        proto.setRel(TEST_REL);
        proto.setSupports(Arrays.asList(ApiType.DDI));
        proto.setRef("{protocol}://{hostnameRequest}:{port}/fws/{tenant}/{targetIdBase62}/{artifactIdBase62}");
        properties.getProtocols().put("ftp", proto);

        final List<ArtifactUrl> urls = urlHandlerUnderTest.getUrls(placeholder, ApiType.DDI,
                new URI("https://" + testHost));

        assertThat(urls).containsExactly(new ArtifactUrl(TEST_PROTO.toUpperCase(), TEST_REL, TEST_PROTO + "://"
                + testHost + ":5683/fws/" + TENANT + "/" + TARGETID_BASE62 + "/" + ARTIFACTID_BASE62));
    }

    @Test
    @Description("Verfies that the port of the statically defined hostname is replaced with the port of the request.")
    public void urlGenerationWithPortFromRequest() throws URISyntaxException {
        final UrlProtocol proto = new UrlProtocol();
        proto.setRef(
                "{protocol}://{hostname}:{portRequest}/{tenant}/controller/v1/{controllerId}/softwaremodules/{softwareModuleId}/artifacts/{artifactFileName}");

        properties.getProtocols().put("download-http", proto);

        final List<ArtifactUrl> ddiUrls = urlHandlerUnderTest.getUrls(placeholder, ApiType.DDI,
                new URI("http://anotherHost.com:8083"));

        assertThat(ddiUrls).containsExactly(new ArtifactUrl("http".toUpperCase(), "download-http",
                "http://localhost:8083/" + TENANT + "/controller/v1/" + CONTROLLER_ID + "/softwaremodules/"
                        + SOFTWAREMODULEID + "/artifacts/" + FILENAME_ENCODE));

        final List<ArtifactUrl> dmfUrls = urlHandlerUnderTest.getUrls(placeholder, ApiType.DMF);

        assertThat(dmfUrls).containsExactly(new ArtifactUrl("http".toUpperCase(), "download-http",
                "http://localhost:8080/" + TENANT + "/controller/v1/" + CONTROLLER_ID + "/softwaremodules/"
                        + SOFTWAREMODULEID + "/artifacts/" + FILENAME_ENCODE));
    }

    @Test
    @Description("Verfies that the domain of the statically defined hostname is replaced with the domain of the request.")
    public void urlGenerationWithDomainFromRequest() throws URISyntaxException {
        final UrlProtocol proto = new UrlProtocol();
        proto.setHostname("host.bumlux.net");
        proto.setRef(
                "{protocol}://{domainRequest}/{tenant}/controller/v1/{controllerId}/softwaremodules/{softwareModuleId}/artifacts/{artifactFileName}");

        properties.getProtocols().put("download-http", proto);

        final List<ArtifactUrl> ddiUrls = urlHandlerUnderTest.getUrls(placeholder, ApiType.DDI,
                new URI("http://anotherHost.com:8083"));
        assertThat(ddiUrls).containsExactly(
                new ArtifactUrl("http".toUpperCase(), "download-http", "http://host.com/" + TENANT + "/controller/v1/"
                        + CONTROLLER_ID + "/softwaremodules/" + SOFTWAREMODULEID + "/artifacts/" + FILENAME_ENCODE));

        final List<ArtifactUrl> dmfUrls = urlHandlerUnderTest.getUrls(placeholder, ApiType.DMF);
        assertThat(dmfUrls).containsExactly(new ArtifactUrl("http".toUpperCase(), "download-http",
                "http://host.bumlux.net/" + TENANT + "/controller/v1/" + CONTROLLER_ID + "/softwaremodules/"
                        + SOFTWAREMODULEID + "/artifacts/" + FILENAME_ENCODE));

    }
}
