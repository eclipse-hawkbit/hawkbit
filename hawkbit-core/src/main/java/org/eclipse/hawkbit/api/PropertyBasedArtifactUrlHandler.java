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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.api.ArtifactUrlHandlerProperties.UrlProtocol;
import org.eclipse.hawkbit.tenancy.TenantAware;

import com.google.common.base.Strings;

//TODO kaizimmerm: autoconfigure
/**
 * Implementation for ArtifactUrlHandler for creating urls to download resource
 * based on pattern.
 */
public class PropertyBasedArtifactUrlHandler implements ArtifactUrlHandler {

    private static final String PROTOCOL_PLACEHOLDER = "protocol";
    private static final String CONTROLLER_ID_PLACEHOLDER = "controllerId";
    private static final String IP_PLACEHOLDER = "ip";
    private static final String PORT_PLACEHOLDER = "port";
    private static final String HOSTNAME_PLACEHOLDER = "hostname";
    private static final String ARTIFACT_FILENAME_PLACEHOLDER = "artifactFileName";
    private static final String ARTIFACT_SHA1_PLACEHOLDER = "artifactSHA1";
    private static final String ARTIFACT_ID_BASE62_PLACEHOLDER = "artifactIdBase62";
    private static final String ARTIFACT_ID_BASE10_PLACEHOLDER = "artifactId";
    private static final String TENANT_PLACEHOLDER = "tenant";
    private static final String SOFTWARE_MODULE_ID_PLACDEHOLDER = "softwareModuleId";

    private final ArtifactUrlHandlerProperties urlHandlerProperties;
    private final TenantAware tenantAware;

    public PropertyBasedArtifactUrlHandler(final ArtifactUrlHandlerProperties urlHandlerProperties,
            final TenantAware tenantAware) {
        this.urlHandlerProperties = urlHandlerProperties;
        this.tenantAware = tenantAware;
    }

    @Override
    public List<ArtifactUrl> getUrls(final String controllerId, final Long softwareModuleId, final String filename,
            final String sha1Hash, final Long artifactid, final APIType api) {

        return urlHandlerProperties.getProtocols().entrySet().stream()
                .filter(entry -> entry.getValue().getSupports().contains(api))
                .map(e -> new ArtifactUrl(e.getValue().getName(), e.getValue().getRel(),
                        generateUrl(e.getValue(), controllerId, softwareModuleId, filename, sha1Hash, artifactid)))
                .collect(Collectors.toList());

    }

    private String generateUrl(final UrlProtocol protocol, final String controllerId, final Long softwareModuleId,
            final String filename, final String sha1Hash, final Long artifactid) {
        final Set<Entry<String, String>> entrySet = getReplaceMap(protocol, controllerId, softwareModuleId, filename,
                sha1Hash, artifactid).entrySet();

        String urlPattern = protocol.getRef();

        for (final Entry<String, String> entry : entrySet) {
            if (entry.getKey().equals(PORT_PLACEHOLDER)) {
                urlPattern = urlPattern.replace(":{" + entry.getKey() + "}",
                        Strings.isNullOrEmpty(entry.getValue()) ? "" : (":" + entry.getValue()));
            } else {
                urlPattern = urlPattern.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return urlPattern;
    }

    private Map<String, String> getReplaceMap(final UrlProtocol protocol, final String controllerId,
            final Long softwareModuleId, final String filename, final String sha1Hash, final Long artifactId) {
        final Map<String, String> replaceMap = new HashMap<>();
        replaceMap.put(IP_PLACEHOLDER, protocol.getIp());
        replaceMap.put(HOSTNAME_PLACEHOLDER, protocol.getHostname());
        replaceMap.put(ARTIFACT_FILENAME_PLACEHOLDER, filename);
        replaceMap.put(ARTIFACT_SHA1_PLACEHOLDER, sha1Hash);
        replaceMap.put(PROTOCOL_PLACEHOLDER, protocol.getName());
        replaceMap.put(PORT_PLACEHOLDER, String.valueOf(protocol.getPort()));
        replaceMap.put(TENANT_PLACEHOLDER, tenantAware.getCurrentTenant());
        replaceMap.put(CONTROLLER_ID_PLACEHOLDER, controllerId);
        replaceMap.put(ARTIFACT_ID_BASE62_PLACEHOLDER, Base62Util.fromBase10(artifactId));
        replaceMap.put(ARTIFACT_ID_BASE10_PLACEHOLDER, String.valueOf(artifactId));
        replaceMap.put(SOFTWARE_MODULE_ID_PLACDEHOLDER, String.valueOf(softwareModuleId));
        return replaceMap;
    }

}
