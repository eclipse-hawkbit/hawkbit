/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.autoconfigure.ddi;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.Filter;

import org.eclipse.hawkbit.rest.security.DosFilter;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Feature: Unit Tests - Security<br/>
 * Story: DDI DoS filters registration
 */
class DdiDosFilterConfigurationTest {

    private static final String DDI_PATH = "/DEFAULT/controller/v1/device1";
    private static final String DOWNLOAD_PATH = DDI_PATH + "/softwaremodules/1/artifacts/file.bin";
    private static final String CLIENT_IP = "203.0.113.7";

    /**
     * The DDI DoS filter doesn't count the downloads - they are limited by the DDI download DoS filter only, so the download limits are
     * effective even if above the DDI ones.
     */
    @Test
    void downloadsCountedByDownloadFilterOnly() throws Exception {
        final HawkbitSecurityProperties securityProperties = new HawkbitSecurityProperties();
        securityProperties.getDos().getFilter().getDdi().setMaxRead(1);
        securityProperties.getDos().getFilter().getDdiDownload().setMaxRead(3);
        final DosFilter ddi = ddiConfiguration(securityProperties).dosFilterDDI(securityProperties).getFilter();
        final DosFilter download = downloadConfiguration(securityProperties).dosFilterDDIDL(securityProperties).getFilter();

        for (int i = 0; i < 3; i++) {
            // both registered for the download path (as servlet filters) - the DDI one doesn't count it
            assertThat(status(ddi, request(DOWNLOAD_PATH, CLIENT_IP))).isEqualTo(200);
            assertThat(status(download, request(DOWNLOAD_PATH, CLIENT_IP))).isEqualTo(200);
        }
        assertThat(status(download, request(DOWNLOAD_PATH, CLIENT_IP))).isEqualTo(429);
        // the DDI limit is not affected by the downloads
        assertThat(status(ddi, request(DDI_PATH, CLIENT_IP))).isEqualTo(200);
        assertThat(status(ddi, request(DDI_PATH, CLIENT_IP))).isEqualTo(429);
    }

    private static ControllerSecurityConfiguration ddiConfiguration(final HawkbitSecurityProperties securityProperties) {
        return new ControllerSecurityConfiguration(null, null, securityProperties);
    }

    private static ControllerDownloadSecurityConfiguration downloadConfiguration(final HawkbitSecurityProperties securityProperties) {
        return new ControllerDownloadSecurityConfiguration(null, null, securityProperties);
    }

    private static MockHttpServletRequest request(final String uri, final String ip) {
        return request("GET", uri, ip);
    }

    private static MockHttpServletRequest request(final String method, final String uri, final String ip) {
        final MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(ip);
        return request;
    }

    private static int status(final Filter filter, final MockHttpServletRequest request) throws Exception {
        final MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response.getStatus();
    }
}
