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

import com.google.common.base.Strings;
import com.google.common.net.UrlEscapers;

/**
 * Implementation for ArtifactUrlHandler for creating urls to download resource
 * based on patterns configured by {@link ArtifactUrlHandlerProperties}.
 * 
 * This mechanism can be used to generate links to arbitrary file hosting
 * infrastructure. However, the hawkBit update server supports hosting files as
 * well in the following {@link UrlProtocol#getRef()} patterns:
 * 
 * Default:
 * {protocol}://{hostname}:{port}/{tenant}/controller/v1/{controllerId}/
 * softwaremodules/{softwareModuleId}/artifacts/{artifactFileName}
 * 
 * Default (MD5SUM files):
 * {protocol}://{hostname}:{port}/{tenant}/controller/v1/{controllerId}/
 * softwaremodules/{softwareModuleId}/artifacts/{artifactFileName}.MD5SUM
 * 
 */
public class PropertyBasedArtifactUrlHandler implements ArtifactUrlHandler {

    private static final String PROTOCOL_PLACEHOLDER = "protocol";
    private static final String CONTROLLER_ID_PLACEHOLDER = "controllerId";
    private static final String TARGET_ID_BASE10_PLACEHOLDER = "targetId";
    private static final String TARGET_ID_BASE62_PLACEHOLDER = "targetIdBase62";
    private static final String IP_PLACEHOLDER = "ip";
    private static final String PORT_PLACEHOLDER = "port";
    private static final String HOSTNAME_PLACEHOLDER = "hostname";
    private static final String ARTIFACT_FILENAME_PLACEHOLDER = "artifactFileName";
    private static final String ARTIFACT_SHA1_PLACEHOLDER = "artifactSHA1";
    private static final String ARTIFACT_ID_BASE10_PLACEHOLDER = "artifactId";
    private static final String ARTIFACT_ID_BASE62_PLACEHOLDER = "artifactIdBase62";
    private static final String TENANT_PLACEHOLDER = "tenant";
    private static final String TENANT_ID_BASE10_PLACEHOLDER = "tenantId";
    private static final String TENANT_ID_BASE62_PLACEHOLDER = "tenantIdBase62";
    private static final String SOFTWARE_MODULE_ID_BASE10_PLACDEHOLDER = "softwareModuleId";
    private static final String SOFTWARE_MODULE_ID_BASE62_PLACDEHOLDER = "softwareModuleIdBase62";

    private final ArtifactUrlHandlerProperties urlHandlerProperties;

    /**
     * @param urlHandlerProperties
     *            for URL generation configuration
     */
    public PropertyBasedArtifactUrlHandler(final ArtifactUrlHandlerProperties urlHandlerProperties) {
        this.urlHandlerProperties = urlHandlerProperties;
    }

    @Override
    public List<ArtifactUrl> getUrls(final URLPlaceholder placeholder, final APIType api) {

        return urlHandlerProperties.getProtocols().stream().filter(entry -> entry.getSupports().contains(api))
                .map(entry -> new ArtifactUrl(entry.getProtocol(), entry.getRel(), generateUrl(entry, placeholder)))
                .collect(Collectors.toList());

    }

    private String generateUrl(final UrlProtocol protocol, final URLPlaceholder placeholder) {
        final Set<Entry<String, String>> entrySet = getReplaceMap(protocol, placeholder).entrySet();

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

    private Map<String, String> getReplaceMap(final UrlProtocol protocol, final URLPlaceholder placeholder) {
        final Map<String, String> replaceMap = new HashMap<>();
        replaceMap.put(IP_PLACEHOLDER, protocol.getIp());
        replaceMap.put(HOSTNAME_PLACEHOLDER, protocol.getHostname());
        replaceMap.put(ARTIFACT_FILENAME_PLACEHOLDER,
                UrlEscapers.urlFragmentEscaper().escape(placeholder.getFilename()));
        replaceMap.put(ARTIFACT_SHA1_PLACEHOLDER, placeholder.getSha1Hash());
        replaceMap.put(PROTOCOL_PLACEHOLDER, protocol.getProtocol());
        replaceMap.put(PORT_PLACEHOLDER, protocol.getPort() == null ? null : String.valueOf(protocol.getPort()));
        replaceMap.put(TENANT_PLACEHOLDER, placeholder.getTenant());
        replaceMap.put(TENANT_ID_BASE10_PLACEHOLDER, String.valueOf(placeholder.getTenantId()));
        replaceMap.put(TENANT_ID_BASE62_PLACEHOLDER, Base62Util.fromBase10(placeholder.getTenantId()));
        replaceMap.put(CONTROLLER_ID_PLACEHOLDER, placeholder.getControllerId());
        replaceMap.put(TARGET_ID_BASE10_PLACEHOLDER, String.valueOf(placeholder.getTargetId()));
        replaceMap.put(TARGET_ID_BASE62_PLACEHOLDER, Base62Util.fromBase10(placeholder.getTargetId()));
        replaceMap.put(ARTIFACT_ID_BASE62_PLACEHOLDER, Base62Util.fromBase10(placeholder.getArtifactId()));
        replaceMap.put(ARTIFACT_ID_BASE10_PLACEHOLDER, String.valueOf(placeholder.getArtifactId()));
        replaceMap.put(SOFTWARE_MODULE_ID_BASE10_PLACDEHOLDER, String.valueOf(placeholder.getSoftwareModuleId()));
        replaceMap.put(SOFTWARE_MODULE_ID_BASE62_PLACDEHOLDER,
                Base62Util.fromBase10(placeholder.getSoftwareModuleId()));
        return replaceMap;
    }

}
