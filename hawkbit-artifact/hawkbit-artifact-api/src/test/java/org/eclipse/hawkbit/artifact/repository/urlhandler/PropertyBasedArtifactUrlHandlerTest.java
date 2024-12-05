/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.artifact.repository.urlhandler;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.artifact.repository.urlhandler.ArtifactUrlHandlerProperties.UrlProtocol;
import org.eclipse.hawkbit.artifact.repository.urlhandler.URLPlaceholder.SoftwareData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private static final long SOFTWARE_MODULE_ID = 87654L;
    private static final long TARGET_ID = 3474366L;
    private static final String TARGET_ID_BASE62 = "EZqA";
    private static final String SHA1HASH = "test12345";
    private static final long ARTIFACT_ID = 1345678L;
    private static final String ARTIFACT_ID_BASE62 = "5e4U";
    private static final String TENANT = "TEST_TENANT";

    private static final String HTTP_LOCALHOST = "http://localhost:8080/";
    private static final URLPlaceholder placeHolder = new URLPlaceholder(
            TENANT, TENANT_ID, CONTROLLER_ID, TARGET_ID,
            new SoftwareData(SOFTWARE_MODULE_ID, FILENAME_DECODE, ARTIFACT_ID, SHA1HASH));
    private ArtifactUrlHandler urlHandlerUnderTest;
    private ArtifactUrlHandlerProperties properties;

    @BeforeEach
    public void setup() {
        properties = new ArtifactUrlHandlerProperties();
        urlHandlerUnderTest = new PropertyBasedArtifactUrlHandler(properties, "");
    }

    @Test
    @Description("Tests the generation of http download url.")
    public void urlGenerationWithDefaultConfiguration() {
        properties.getProtocols().put("download-http", new UrlProtocol());

        final List<ArtifactUrl> ddiUrls = urlHandlerUnderTest.getUrls(placeHolder, ApiType.DDI);
        assertThat(ddiUrls).containsExactly(
                new ArtifactUrl(
                        "http".toUpperCase(), "download-http",
                        HTTP_LOCALHOST + TENANT + "/controller/v1/" +
                                CONTROLLER_ID + "/softwaremodules/" + SOFTWARE_MODULE_ID + "/artifacts/" + FILENAME_ENCODE));

        assertThat(ddiUrls).isEqualTo(urlHandlerUnderTest.getUrls(placeHolder, ApiType.DMF));
    }

    @Test
    @Description("Tests the generation of custom download url with a CoAP example that supports DMF only.")
    public void urlGenerationWithCustomConfiguration() {
        final UrlProtocol proto = new UrlProtocol();
        proto.setIp("127.0.0.1");
        proto.setPort(5683);
        proto.setProtocol(TEST_PROTO);
        proto.setRel(TEST_REL);
        proto.setSupports(List.of(ApiType.DMF));
        proto.setRef("{protocol}://{ip}:{port}/fw/{tenant}/{controllerId}/sha1/{artifactSHA1}");
        properties.getProtocols().put(TEST_PROTO, proto);

        assertThat(urlHandlerUnderTest.getUrls(placeHolder, ApiType.DDI)).isEmpty();
        assertThat(urlHandlerUnderTest.getUrls(placeHolder, ApiType.DMF)).containsExactly(
                new ArtifactUrl(
                        TEST_PROTO.toUpperCase(), TEST_REL,
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
        proto.setSupports(List.of(ApiType.DMF));
        proto.setRef("{protocol}://{ip}:{port}/fws/{tenant}/{targetIdBase62}/{artifactIdBase62}");
        properties.getProtocols().put("ftp", proto);

        assertThat(urlHandlerUnderTest.getUrls(placeHolder, ApiType.DDI)).isEmpty();
        assertThat(urlHandlerUnderTest.getUrls(placeHolder, ApiType.DMF)).containsExactly(
                new ArtifactUrl(
                        TEST_PROTO.toUpperCase(), TEST_REL,
                        TEST_PROTO + "://127.0.0.1:5683/fws/" + TENANT + "/" + TARGET_ID_BASE62 + "/" + ARTIFACT_ID_BASE62));
    }

    @Test
    @Description("Verifies that the full qualified host of the statically defined hostname is replaced with the host of the request.")
    public void urlGenerationWithHostFromRequest() throws URISyntaxException {
        final String testHost = "ddi.host.com";

        final UrlProtocol proto = new UrlProtocol();
        proto.setIp("127.0.0.1");
        proto.setPort(5683);
        proto.setProtocol(TEST_PROTO);
        proto.setRel(TEST_REL);
        proto.setSupports(List.of(ApiType.DDI));
        proto.setRef("{protocol}://{hostnameRequest}:{port}/fws/{tenant}/{targetIdBase62}/{artifactIdBase62}");
        properties.getProtocols().put("ftp", proto);

        assertThat(urlHandlerUnderTest.getUrls(placeHolder, ApiType.DDI, new URI("https://" + testHost))).containsExactly(
                new ArtifactUrl(
                        TEST_PROTO.toUpperCase(), TEST_REL,
                        TEST_PROTO + "://" + testHost + ":5683/fws/" + TENANT + "/" + TARGET_ID_BASE62 + "/" + ARTIFACT_ID_BASE62));
    }

    @Test
    @Description("Verifies that the protocol of the statically defined hostname is replaced with the protocol of the request.")
    public void urlGenerationWithProtocolFromRequest() throws URISyntaxException {
        final String testHost = "ddi.host.com";

        final UrlProtocol proto = new UrlProtocol();
        proto.setRef("{protocolRequest}://{hostname}:{port}/fws/{tenant}/{targetIdBase62}/{artifactIdBase62}");
        properties.getProtocols().put("download-http", proto);

        assertThat(urlHandlerUnderTest.getUrls(placeHolder, ApiType.DDI, new URI("https://" + testHost))).containsExactly(
                new ArtifactUrl(
                        "http".toUpperCase(), "download-http",
                        "https://localhost:8080/fws/" + TENANT + "/" + TARGET_ID_BASE62 + "/" + ARTIFACT_ID_BASE62));
    }

    @Test
    @Description("Verifies that the port of the statically defined hostname is replaced with the port of the request.")
    public void urlGenerationWithPortFromRequest() throws URISyntaxException {
        final UrlProtocol proto = new UrlProtocol();
        proto.setRef("{protocol}://{hostname}:{portRequest}/{tenant}/controller/v1/{controllerId}/softwaremodules/{softwareModuleId}/artifacts/{artifactFileName}");

        properties.getProtocols().put("download-http", proto);

        assertThat(urlHandlerUnderTest.getUrls(placeHolder, ApiType.DDI, new URI("http://anotherHost.com:8083"))).containsExactly(
                new ArtifactUrl(
                        "http".toUpperCase(), "download-http",
                        "http://localhost:8083/" + TENANT + "/controller/v1/" +
                                CONTROLLER_ID + "/softwaremodules/" + SOFTWARE_MODULE_ID + "/artifacts/" + FILENAME_ENCODE));

        assertThat(urlHandlerUnderTest.getUrls(placeHolder, ApiType.DMF)).containsExactly(
                new ArtifactUrl(
                        "http".toUpperCase(), "download-http",
                        "http://localhost:8080/" + TENANT + "/controller/v1/" +
                                CONTROLLER_ID + "/softwaremodules/" + SOFTWARE_MODULE_ID + "/artifacts/" + FILENAME_ENCODE));
    }

    @Test
    @Description("Verifies that if default protocol port in request is used then url is returned without port")
    public void urlGenerationWithPortFromRequestForHttps() throws URISyntaxException {
        final String protocol = "https";
        final UrlProtocol proto = new UrlProtocol();
        proto.setRef("{protocolRequest}://{hostnameRequest}:{portRequest}/{tenant}/controller/v1/{controllerId}/softwaremodules/{softwareModuleId}/artifacts/{artifactFileName}");
        proto.setProtocol(protocol);
        properties.getProtocols().put("download-http", proto);

        final URI uri = new URI(protocol + "://anotherHost.com");
        assertThat(urlHandlerUnderTest.getUrls(placeHolder, ApiType.DDI, uri)).containsExactly(
                new ArtifactUrl(
                        protocol.toUpperCase(), "download-http",
                        uri + "/" + TENANT + "/controller/v1/" +
                                CONTROLLER_ID + "/softwaremodules/" + SOFTWARE_MODULE_ID + "/artifacts/" + FILENAME_ENCODE));

    }

    @Test
    @Description("Verifies that the domain of the statically defined hostname is replaced with the domain of the request.")
    public void urlGenerationWithDomainFromRequest() throws URISyntaxException {
        final UrlProtocol proto = new UrlProtocol();
        proto.setHostname("host.bumlux.net");
        proto.setRef("{protocol}://{domainRequest}/{tenant}/controller/v1/{controllerId}/softwaremodules/{softwareModuleId}/artifacts/{artifactFileName}");

        properties.getProtocols().put("download-http", proto);

        assertThat(urlHandlerUnderTest.getUrls(placeHolder, ApiType.DDI, new URI("http://anotherHost.com:8083"))).containsExactly(
                new ArtifactUrl(
                        "http".toUpperCase(), "download-http",
                        "http://host.com/" + TENANT + "/controller/v1/" +
                                CONTROLLER_ID + "/softwaremodules/" + SOFTWARE_MODULE_ID + "/artifacts/" + FILENAME_ENCODE));

        assertThat(urlHandlerUnderTest.getUrls(placeHolder, ApiType.DMF)).containsExactly(
                new ArtifactUrl(
                        "http".toUpperCase(), "download-http",
                        "http://host.bumlux.net/" + TENANT + "/controller/v1/" +
                                CONTROLLER_ID + "/softwaremodules/" + SOFTWARE_MODULE_ID + "/artifacts/" + FILENAME_ENCODE));
    }
}