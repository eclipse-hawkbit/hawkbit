/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.hawkbit.api.ArtifactUrlHandlerProperties.ProtocolProperties;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

/**
 * Implementation for ArtifactUrlHandler for creating urls to download resource
 * based on pattern.
 */
@Component
@EnableConfigurationProperties(ArtifactUrlHandlerProperties.class)
public class PropertyBasedArtifactUrlHandler implements ArtifactUrlHandler {

    private static final String PROTOCOL_PLACEHOLDER = "protocol";
    private static final String TARGET_ID_PLACEHOLDER = "targetId";
    private static final String IP_PLACEHOLDER = "ip";
    private static final String PORT_PLACEHOLDER = "port";
    private static final String HOSTNAME_PLACEHOLDER = "hostname";
    private static final String ARTIFACT_FILENAME_PLACEHOLDER = "artifactFileName";
    private static final String ARTIFACT_SHA1_PLACEHOLDER = "artifactSHA1";
    private static final String TENANT_PLACEHOLDER = "tenant";
    private static final String SOFTWARE_MODULE_ID_PLACDEHOLDER = "softwareModuleId";

    @Autowired
    private ArtifactUrlHandlerProperties urlHandlerProperties;

    @Autowired
    private TenantAware tenantAware;

    @Override
    public String getUrl(final String targetId, final Long softwareModuleId, final String filename,
            final String sha1Hash, final UrlProtocol protocol) {

        final String protocolString = protocol.name().toLowerCase();
        final ProtocolProperties properties = urlHandlerProperties.getProperties(protocolString);
        if (properties == null || properties.getPattern() == null) {
            return null;
        }

        String urlPattern = properties.getPattern();
        final Set<Entry<String, String>> entrySet = getReplaceMap(targetId, softwareModuleId, filename, sha1Hash,
                protocolString, properties).entrySet();
        for (final Entry<String, String> entry : entrySet) {
            if (entry.getKey().equals(PORT_PLACEHOLDER)) {
                urlPattern = urlPattern.replace(":{" + entry.getKey() + "}",
                        Strings.isNullOrEmpty(entry.getValue()) ? "" : ":" + entry.getValue());
            } else {
                urlPattern = urlPattern.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return urlPattern;
    }

    private Map<String, String> getReplaceMap(final String targetId, final Long softwareModuleId, final String filename,
            final String sha1Hash, final String protocol, final ProtocolProperties properties) {
        final Map<String, String> replaceMap = new HashMap<>();
        replaceMap.put(IP_PLACEHOLDER, properties.getIp());
        replaceMap.put(HOSTNAME_PLACEHOLDER, properties.getHostname());
        replaceMap.put(ARTIFACT_FILENAME_PLACEHOLDER, filename);
        replaceMap.put(ARTIFACT_SHA1_PLACEHOLDER, sha1Hash);
        replaceMap.put(PROTOCOL_PLACEHOLDER, protocol);
        replaceMap.put(PORT_PLACEHOLDER, properties.getPort());
        replaceMap.put(TENANT_PLACEHOLDER, tenantAware.getCurrentTenant());
        replaceMap.put(TARGET_ID_PLACEHOLDER, targetId);
        replaceMap.put(SOFTWARE_MODULE_ID_PLACDEHOLDER, String.valueOf(softwareModuleId));
        return replaceMap;
    }

}
