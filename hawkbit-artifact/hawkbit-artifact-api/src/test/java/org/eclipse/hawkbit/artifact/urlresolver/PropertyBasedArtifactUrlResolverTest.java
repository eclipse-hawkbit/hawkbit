/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.artifact.urlresolver;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.hawkbit.artifact.urlresolver.ArtifactUrlResolver.DownloadDescriptor;
import org.eclipse.hawkbit.artifact.urlresolver.PropertyBasedArtifactUrlResolverProperties.UrlProtocol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for creating urls to download artifacts.
 * <p/>
 * Feature: Unit Tests - Artifact URL Handler<br/>
 * Story: Test to generate the artifact download URL
 */
@ExtendWith(MockitoExtension.class)
class PropertyBasedArtifactUrlResolverTest {

    private static final String TEST_PROTO = "https";
    private static final String TEST_REL = "download-udp";

    private static final String TENANT = "TEST_TENANT";
    private static final String CONTROLLER_ID = "Test";
    private static final long SOFTWARE_MODULE_ID = 87654L;
    private static final String FILENAME = "test123!§$%&";
    private static final String FILENAME_ENCODED = "test123%21%C2%A7%24%25%26";
    private static final String SHA1 = "0123456789012345678901234567890123456789";

    private static final String HTTP_LOCALHOST = "http://localhost:8080/";
    private static final DownloadDescriptor DOWNLOAD_DESCRIPTOR = new DownloadDescriptor(TENANT, CONTROLLER_ID, SOFTWARE_MODULE_ID, FILENAME,
            SHA1);

    private ArtifactUrlResolver urlHandlerUnderTest;
    private PropertyBasedArtifactUrlResolverProperties properties;

    @BeforeEach
    void setup() {
        properties = new PropertyBasedArtifactUrlResolverProperties();
        urlHandlerUnderTest = new PropertyBasedArtifactUrlResolver(properties, "");
    }

    @Test
    void urlGenerationWithDefaultConfiguration() {
        properties.getProtocols().put("download-http", new UrlProtocol());

        final List<ArtifactUrl> ddiUrls = urlHandlerUnderTest.getUrls(DOWNLOAD_DESCRIPTOR, ArtifactUrlResolver.ApiType.DDI);
        assertThat(ddiUrls).containsExactly(
                        new ArtifactUrl(
                                "http".toUpperCase(), "download-http",
                                HTTP_LOCALHOST + TENANT + "/controller/v1/" +
                                        CONTROLLER_ID + "/softwaremodules/" + SOFTWARE_MODULE_ID + "/artifacts/" + FILENAME_ENCODED))
                .isEqualTo(urlHandlerUnderTest.getUrls(DOWNLOAD_DESCRIPTOR, ArtifactUrlResolver.ApiType.DMF));
    }

    @Test
    void urlGenerationWithCustomConfiguration() {
        final UrlProtocol proto = new UrlProtocol();
        proto.setIp("127.0.0.1");
        proto.setPort(5683);
        proto.setProtocol(TEST_PROTO);
        proto.setRel(TEST_REL);
        proto.setSupports(List.of(ArtifactUrlResolver.ApiType.DMF));
        proto.setRef("{protocol}://{ip}:{port}/fw/{tenant}/{controllerId}/sha1/{artifactFileName}");
        properties.getProtocols().put(TEST_PROTO, proto);

        assertThat(urlHandlerUnderTest.getUrls(DOWNLOAD_DESCRIPTOR, ArtifactUrlResolver.ApiType.DDI)).isEmpty();
        assertThat(urlHandlerUnderTest.getUrls(DOWNLOAD_DESCRIPTOR, ArtifactUrlResolver.ApiType.DMF)).containsExactly(
                new ArtifactUrl(
                        TEST_PROTO.toUpperCase(), TEST_REL,
                        TEST_PROTO + "://127.0.0.1:5683/fw/" + TENANT + "/" + CONTROLLER_ID + "/sha1/" + FILENAME_ENCODED));
    }

    @Test
    void urlGenerationWithHostFromRequest() throws URISyntaxException {
        final String testHost = "ddi.host.com";

        final UrlProtocol proto = new UrlProtocol();
        proto.setIp("127.0.0.1");
        proto.setPort(5683);
        proto.setProtocol(TEST_PROTO);
        proto.setRel(TEST_REL);
        proto.setSupports(List.of(ArtifactUrlResolver.ApiType.DDI));
        proto.setRef("{protocol}://{hostnameRequest}:{port}/fws/{tenant}/{controllerId}/{artifactFileName}");
        properties.getProtocols().put("ftp", proto);

        assertThat(urlHandlerUnderTest.getUrls(DOWNLOAD_DESCRIPTOR, ArtifactUrlResolver.ApiType.DDI,
                new URI("https://" + testHost))).containsExactly(
                new ArtifactUrl(
                        TEST_PROTO.toUpperCase(), TEST_REL,
                        TEST_PROTO + "://" + testHost + ":5683/fws/" + TENANT + "/" + CONTROLLER_ID + "/" + FILENAME_ENCODED));
    }

    /**
     * Verifies that the protocol of the statically defined hostname is replaced with the protocol of the request.
     */
    @Test
    void urlGenerationWithProtocolFromRequest() throws URISyntaxException {
        final String testHost = "ddi.host.com";

        final UrlProtocol proto = new UrlProtocol();
        proto.setRef("{protocolRequest}://{hostname}:{port}/fws/{tenant}/{controllerId}/{artifactFileName}");
        properties.getProtocols().put("download-http", proto);

        assertThat(urlHandlerUnderTest.getUrls(DOWNLOAD_DESCRIPTOR, ArtifactUrlResolver.ApiType.DDI,
                new URI("https://" + testHost))).containsExactly(
                new ArtifactUrl(
                        "http".toUpperCase(), "download-http",
                        "https://localhost:8080/fws/" + TENANT + "/" + CONTROLLER_ID + "/" + FILENAME_ENCODED));
    }

    /**
     * Verifies that the port of the statically defined hostname is replaced with the port of the request.
     */
    @Test
    void urlGenerationWithPortFromRequest() throws URISyntaxException {
        final UrlProtocol proto = new UrlProtocol();
        proto.setRef(
                "{protocol}://{hostname}:{portRequest}/{tenant}/controller/v1/{controllerId}/softwaremodules/{softwareModuleId}/artifacts/{artifactFileName}");

        properties.getProtocols().put("download-http", proto);

        assertThat(urlHandlerUnderTest.getUrls(DOWNLOAD_DESCRIPTOR, ArtifactUrlResolver.ApiType.DDI,
                new URI("http://anotherHost.com:8083"))).containsExactly(
                new ArtifactUrl(
                        "http".toUpperCase(), "download-http",
                        "http://localhost:8083/" + TENANT + "/controller/v1/" +
                                CONTROLLER_ID + "/softwaremodules/" + SOFTWARE_MODULE_ID + "/artifacts/" + FILENAME_ENCODED));

        assertThat(urlHandlerUnderTest.getUrls(DOWNLOAD_DESCRIPTOR, ArtifactUrlResolver.ApiType.DMF)).containsExactly(
                new ArtifactUrl(
                        "http".toUpperCase(), "download-http",
                        "http://localhost:8080/" + TENANT + "/controller/v1/" +
                                CONTROLLER_ID + "/softwaremodules/" + SOFTWARE_MODULE_ID + "/artifacts/" + FILENAME_ENCODED));
    }

    /**
     * Verifies that if default protocol port in request is used then url is returned without port
     */
    @Test
    void urlGenerationWithPortFromRequestForHttps() throws URISyntaxException {
        final String protocol = "https";
        final UrlProtocol proto = new UrlProtocol();
        proto.setRef(
                "{protocolRequest}://{hostnameRequest}:{portRequest}/{tenant}/controller/v1/{controllerId}/softwaremodules/{softwareModuleId}/artifacts/{artifactFileName}");
        proto.setProtocol(protocol);
        properties.getProtocols().put("download-http", proto);

        final URI uri = new URI(protocol + "://anotherHost.com");
        assertThat(urlHandlerUnderTest.getUrls(DOWNLOAD_DESCRIPTOR, ArtifactUrlResolver.ApiType.DDI, uri)).containsExactly(
                new ArtifactUrl(
                        protocol.toUpperCase(), "download-http",
                        uri + "/" + TENANT + "/controller/v1/" + CONTROLLER_ID + "/softwaremodules/" + SOFTWARE_MODULE_ID + "/artifacts/" + FILENAME_ENCODED));
    }

    /**
     * Verifies that the domain of the statically defined hostname is replaced with the domain of the request.
     */
    @Test
    void urlGenerationWithDomainFromRequest() throws URISyntaxException {
        final UrlProtocol proto = new UrlProtocol();
        proto.setHostname("host.bumlux.net");
        proto.setRef(
                "{protocol}://{domainRequest}/{tenant}/controller/v1/{controllerId}/softwaremodules/{softwareModuleId}/artifacts/{artifactFileName}");

        properties.getProtocols().put("download-http", proto);

        assertThat(urlHandlerUnderTest.getUrls(DOWNLOAD_DESCRIPTOR, ArtifactUrlResolver.ApiType.DDI,
                new URI("http://anotherHost.com:8083"))).containsExactly(
                new ArtifactUrl(
                        "http".toUpperCase(), "download-http",
                        "http://host.com/" + TENANT + "/controller/v1/" +
                                CONTROLLER_ID + "/softwaremodules/" + SOFTWARE_MODULE_ID + "/artifacts/" + FILENAME_ENCODED));

        assertThat(urlHandlerUnderTest.getUrls(DOWNLOAD_DESCRIPTOR, ArtifactUrlResolver.ApiType.DMF)).containsExactly(
                new ArtifactUrl(
                        "http".toUpperCase(), "download-http",
                        "http://host.bumlux.net/" + TENANT + "/controller/v1/" +
                                CONTROLLER_ID + "/softwaremodules/" + SOFTWARE_MODULE_ID + "/artifacts/" + FILENAME_ENCODED));
    }
}