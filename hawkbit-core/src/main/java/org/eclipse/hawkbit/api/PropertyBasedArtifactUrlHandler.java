/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.api;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.api.ArtifactUrlHandlerProperties.UrlProtocol;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Implementation for ArtifactUrlHandler for creating urls to download resource
 * based on patterns configured by {@link ArtifactUrlHandlerProperties}.
 *
 * This mechanism can be used to generate links to arbitrary file hosting
 * infrastructure. However, the hawkBit update server supports hosting files as
 * well in the following {@link UrlProtocol#getRef()} patterns:
 *
 * Default:
 * {protocol}://{hostname}:{port}{contextPath}/{tenant}/controller/v1/{controllerId}/
 * softwaremodules/{softwareModuleId}/artifacts/{artifactFileName}
 *
 * Default (MD5SUM files):
 * {protocol}://{hostname}:{port}{contextPath}/{tenant}/controller/v1/{controllerId}/
 * softwaremodules/{softwareModuleId}/artifacts/{artifactFileName}.MD5SUM
 */
public class PropertyBasedArtifactUrlHandler implements ArtifactUrlHandler {

    private static final String PROTOCOL_PLACEHOLDER = "protocol";
    private static final String HOSTNAME_PLACEHOLDER = "hostname";
    private static final String IP_PLACEHOLDER = "ip";
    private static final String PORT_PLACEHOLDER = "port";
    private static final String CONTEXT_PATH = "contextPath";
    private static final String CONTROLLER_ID_PLACEHOLDER = "controllerId";
    private static final String TARGET_ID_BASE10_PLACEHOLDER = "targetId";
    private static final String TARGET_ID_BASE62_PLACEHOLDER = "targetIdBase62";
    private static final String HOSTNAME_REQUEST_PLACEHOLDER = "hostnameRequest";
    private static final String PORT_REQUEST_PLACEHOLDER = "portRequest";
    private static final String PROTOCOL_REQUEST_PLACEHOLDER = "protocolRequest";
    private static final String HOSTNAME_WITH_DOMAIN_REQUEST_PLACEHOLDER = "domainRequest";
    private static final String ARTIFACT_FILENAME_PLACEHOLDER = "artifactFileName";
    private static final String ARTIFACT_SHA1_PLACEHOLDER = "artifactSHA1";
    private static final String ARTIFACT_ID_BASE10_PLACEHOLDER = "artifactId";
    private static final String ARTIFACT_ID_BASE62_PLACEHOLDER = "artifactIdBase62";
    private static final String TENANT_PLACEHOLDER = "tenant";
    private static final String TENANT_ID_BASE10_PLACEHOLDER = "tenantId";
    private static final String TENANT_ID_BASE62_PLACEHOLDER = "tenantIdBase62";
    private static final String SOFTWARE_MODULE_ID_BASE10_PLACEHOLDER = "softwareModuleId";
    final static String DEFAULT_URL_PROTOCOL_REF = "{" + PROTOCOL_PLACEHOLDER + "}://{" + HOSTNAME_PLACEHOLDER + "}:{" + PORT_PLACEHOLDER + "}{" + CONTEXT_PATH + "}/{" + TENANT_PLACEHOLDER + "}/controller/v1/{" + CONTROLLER_ID_PLACEHOLDER + "}/softwaremodules/{" + SOFTWARE_MODULE_ID_BASE10_PLACEHOLDER + "}/artifacts/{" + ARTIFACT_FILENAME_PLACEHOLDER + "}";
    private static final String SOFTWARE_MODULE_ID_BASE62_PLACEHOLDER = "softwareModuleIdBase62";
    private final ArtifactUrlHandlerProperties urlHandlerProperties;
    private final String contextPath;

    /**
     * @param urlHandlerProperties for URL generation configuration
     */
    public PropertyBasedArtifactUrlHandler(final ArtifactUrlHandlerProperties urlHandlerProperties, final String contextPath) {
        this.urlHandlerProperties = urlHandlerProperties;
        this.contextPath = contextPath == null || "/".equals(contextPath) ? "" : contextPath; // normalize
    }

    @Override
    public List<ArtifactUrl> getUrls(final URLPlaceholder placeholder, final ApiType api) {
        return getUrls(placeholder, api, null);
    }

    @Override
    public List<ArtifactUrl> getUrls(final URLPlaceholder placeholder, final ApiType api, final URI requestUri) {
        return urlHandlerProperties.getProtocols().values().stream()
                .filter(urlProtocol -> urlProtocol.getSupports().contains(api) && urlProtocol.isEnabled())
                .map(urlProtocol -> new ArtifactUrl(urlProtocol.getProtocol().toUpperCase(), urlProtocol.getRel(),
                        generateUrl(urlProtocol, placeholder, requestUri)))
                .collect(Collectors.toList());

    }

    private static String getRequestPort(final UrlProtocol protocol, final URI requestUri) {
        if (requestUri == null) {
            return getPort(protocol);
        }
        // if port undefined then default protocol port is used
        return requestUri.getPort() > 0 ? String.valueOf(requestUri.getPort()) : "";
    }

    private static String getRequestHost(final UrlProtocol protocol, final URI requestUri) {
        if (requestUri == null) {
            return protocol.getHostname();
        }

        return Optional.ofNullable(requestUri.getHost()).orElse(protocol.getHostname());
    }

    private static String getRequestProtocol(final UrlProtocol protocol, final URI requestUri) {
        if (requestUri == null) {
            return protocol.getProtocol();
        }

        return Optional.ofNullable(requestUri.getScheme()).orElse(protocol.getProtocol());
    }

    private static String getPort(final UrlProtocol protocol) {
        return ObjectUtils.isEmpty(protocol.getPort()) ? null : String.valueOf(protocol.getPort());
    }

    private static String computeHostWithRequestDomain(final UrlProtocol protocol, final URI requestUri) {

        if (requestUri == null) {
            return protocol.getHostname();
        }

        if (!protocol.getHostname().contains(".")) {
            return protocol.getHostname();
        }

        final String host = StringUtils.delimitedListToStringArray(protocol.getHostname(), ".")[0].trim();

        final List<String> domainElements = Arrays
                .asList(StringUtils.delimitedListToStringArray(requestUri.getHost(), "."));
        final String domain = StringUtils.collectionToDelimitedString(domainElements.subList(1, domainElements.size()),
                ".");

        if (ObjectUtils.isEmpty(domain)) {
            return protocol.getHostname();
        }

        return host + "." + domain;
    }

    private String generateUrl(final UrlProtocol protocol, final URLPlaceholder placeholder,
            final URI requestUri) {
        final Set<Entry<String, String>> entrySet = getReplaceMap(protocol, placeholder, requestUri).entrySet();

        String urlPattern = protocol.getRef();

        for (final Entry<String, String> entry : entrySet) {
            if (List.of(PORT_PLACEHOLDER, PORT_REQUEST_PLACEHOLDER).contains(entry.getKey())) {
                urlPattern = urlPattern.replace(":{" + entry.getKey() + "}",
                        ObjectUtils.isEmpty(entry.getValue()) ? "" : (":" + entry.getValue()));
            } else {
                if (entry.getValue() != null) {
                    urlPattern = urlPattern.replace("{" + entry.getKey() + "}", entry.getValue());
                }
            }
        }

        return urlPattern;
    }

    private Map<String, String> getReplaceMap(final UrlProtocol protocol, final URLPlaceholder placeholder,
            final URI requestUri) {
        final Map<String, String> replaceMap = new HashMap<>();
        replaceMap.put(IP_PLACEHOLDER, protocol.getIp());

        replaceMap.put(HOSTNAME_PLACEHOLDER, protocol.getHostname());

        replaceMap.put(HOSTNAME_REQUEST_PLACEHOLDER, getRequestHost(protocol, requestUri));
        replaceMap.put(PORT_REQUEST_PLACEHOLDER, getRequestPort(protocol, requestUri));
        replaceMap.put(HOSTNAME_WITH_DOMAIN_REQUEST_PLACEHOLDER, computeHostWithRequestDomain(protocol, requestUri));
        replaceMap.put(PROTOCOL_REQUEST_PLACEHOLDER, getRequestProtocol(protocol, requestUri));

        replaceMap.put(CONTEXT_PATH, contextPath);

        replaceMap.put(ARTIFACT_FILENAME_PLACEHOLDER,
                URLEncoder.encode(placeholder.getSoftwareData().getFilename(), StandardCharsets.UTF_8));

        replaceMap.put(ARTIFACT_SHA1_PLACEHOLDER, placeholder.getSoftwareData().getSha1Hash());
        replaceMap.put(PROTOCOL_PLACEHOLDER, protocol.getProtocol());
        replaceMap.put(PORT_PLACEHOLDER, getPort(protocol));
        replaceMap.put(TENANT_PLACEHOLDER, placeholder.getTenant());
        replaceMap.put(TENANT_ID_BASE10_PLACEHOLDER, String.valueOf(placeholder.getTenantId()));
        replaceMap.put(TENANT_ID_BASE62_PLACEHOLDER, Base62Util.fromBase10(placeholder.getTenantId()));
        replaceMap.put(CONTROLLER_ID_PLACEHOLDER, placeholder.getControllerId());
        replaceMap.put(TARGET_ID_BASE10_PLACEHOLDER, String.valueOf(placeholder.getTargetId()));
        if (placeholder.getTargetId() != null) {
            replaceMap.put(TARGET_ID_BASE62_PLACEHOLDER, Base62Util.fromBase10(placeholder.getTargetId()));
        }
        replaceMap.put(ARTIFACT_ID_BASE62_PLACEHOLDER,
                Base62Util.fromBase10(placeholder.getSoftwareData().getArtifactId()));
        replaceMap.put(ARTIFACT_ID_BASE10_PLACEHOLDER, String.valueOf(placeholder.getSoftwareData().getArtifactId()));
        replaceMap.put(SOFTWARE_MODULE_ID_BASE10_PLACEHOLDER,
                String.valueOf(placeholder.getSoftwareData().getSoftwareModuleId()));
        replaceMap.put(SOFTWARE_MODULE_ID_BASE62_PLACEHOLDER,
                Base62Util.fromBase10(placeholder.getSoftwareData().getSoftwareModuleId()));
        return replaceMap;
    }

}
