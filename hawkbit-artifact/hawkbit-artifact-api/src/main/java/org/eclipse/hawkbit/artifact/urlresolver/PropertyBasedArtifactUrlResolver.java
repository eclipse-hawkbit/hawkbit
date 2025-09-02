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

import org.eclipse.hawkbit.artifact.urlresolver.PropertyBasedArtifactUrlResolverProperties.UrlProtocol;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Implementation for ArtifactUrlHandler for creating urls to download resource based on patterns configured by
 * {@link PropertyBasedArtifactUrlResolverProperties}.
 * <p/>
 * This mechanism can be used to generate links to arbitrary file hosting infrastructure. However, the hawkBit update server
 * supports hosting files as well in the following {@link UrlProtocol#getRef()} patterns:
 * <p/>
 * Default: </br>
 * {protocol}://{hostname}:{port}{contextPath}/{tenant}/controller/v1/{controllerId}
 * /softwaremodules/{softwareModuleId}/artifacts/{artifactFileName}
 * <p/>
 * Default (MD5SUM files):
 * {protocol}://{hostname}:{port}{contextPath}/{tenant}/controller/v1/{controllerId}/
 * softwaremodules/{softwareModuleId}/artifacts/{artifactFileName}.MD5SUM
 */
public class PropertyBasedArtifactUrlResolver implements ArtifactUrlResolver {

    private static final String PROTOCOL_PLACEHOLDER = "protocol";
    private static final String PROTOCOL_REQUEST_PLACEHOLDER = "protocolRequest";
    private static final String HOSTNAME_PLACEHOLDER = "hostname";
    private static final String HOSTNAME_REQUEST_PLACEHOLDER = "hostnameRequest";
    private static final String HOSTNAME_WITH_DOMAIN_REQUEST_PLACEHOLDER = "domainRequest";
    private static final String IP_PLACEHOLDER = "ip";
    private static final String PORT_PLACEHOLDER = "port";
    private static final String PORT_REQUEST_PLACEHOLDER = "portRequest";
    private static final String TENANT_PLACEHOLDER = "tenant";
    private static final String CONTEXT_PATH = "contextPath";
    private static final String CONTROLLER_ID_PLACEHOLDER = "controllerId";
    private static final String SOFTWARE_MODULE_ID_PLACEHOLDER = "softwareModuleId";
    private static final String ARTIFACT_FILENAME_PLACEHOLDER = "artifactFileName";
    private static final String ARTIFACT_SHA1_PLACEHOLDER = "artifactSHA1";
    // by default, we download via the controller / DDI API download endpoint
    static final String DEFAULT_URL_PROTOCOL_REF = "{" + PROTOCOL_REQUEST_PLACEHOLDER + "}://{" + HOSTNAME_REQUEST_PLACEHOLDER + "}:{" + PORT_REQUEST_PLACEHOLDER + "}{" + CONTEXT_PATH + "}/{" + TENANT_PLACEHOLDER + "}/controller/v1/{" + CONTROLLER_ID_PLACEHOLDER + "}/softwaremodules/{" + SOFTWARE_MODULE_ID_PLACEHOLDER + "}/artifacts/{" + ARTIFACT_FILENAME_PLACEHOLDER + "}";

    private final PropertyBasedArtifactUrlResolverProperties urlHandlerProperties;
    private final String contextPath;

    @SuppressWarnings("java:S3358") // better readable this way
    public PropertyBasedArtifactUrlResolver(final PropertyBasedArtifactUrlResolverProperties urlHandlerProperties, final String contextPath) {
        this.urlHandlerProperties = urlHandlerProperties;
        this.contextPath = ObjectUtils.isEmpty(contextPath) || "/".equals(contextPath)
                ? ""
                : (contextPath.charAt(0) == '/' ? contextPath : '/' + contextPath); // normalize context path
    }

    @Override
    public List<ArtifactUrl> getUrls(final DownloadDescriptor downloadDescriptor, final ApiType api) {
        return getUrls(downloadDescriptor, api, null);
    }

    @Override
    public List<ArtifactUrl> getUrls(final DownloadDescriptor downloadDescriptor, final ApiType api, final URI requestUri) {
        return urlHandlerProperties.getProtocols().values().stream()
                .filter(urlProtocol -> urlProtocol.isEnabled() && urlProtocol.getSupports().contains(api))
                .map(urlProtocol -> new ArtifactUrl(
                        urlProtocol.getProtocol().toUpperCase(), urlProtocol.getRel(),
                        generateUrl(urlProtocol, downloadDescriptor, requestUri)))
                .toList();
    }

    private String generateUrl(final UrlProtocol protocol, final DownloadDescriptor placeholder, final URI requestUri) {
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

    private Map<String, String> getReplaceMap(final UrlProtocol protocol, final DownloadDescriptor placeholder, final URI requestUri) {
        final Map<String, String> replaceMap = new HashMap<>();

        replaceMap.put(PROTOCOL_PLACEHOLDER, protocol.getProtocol());
        replaceMap.put(PROTOCOL_REQUEST_PLACEHOLDER, Optional.ofNullable(requestUri).map(URI::getScheme).orElseGet(protocol::getProtocol));

        replaceMap.put(HOSTNAME_PLACEHOLDER, protocol.getHostname());
        replaceMap.put(IP_PLACEHOLDER, protocol.getIp());
        replaceMap.put(HOSTNAME_REQUEST_PLACEHOLDER, Optional.ofNullable(requestUri).map(URI::getHost).orElseGet(protocol::getHostname));
        replaceMap.put(HOSTNAME_WITH_DOMAIN_REQUEST_PLACEHOLDER, computeHostWithRequestDomain(protocol, requestUri));

        replaceMap.put(PORT_PLACEHOLDER, getPort(protocol));
        replaceMap.put(
                PORT_REQUEST_PLACEHOLDER,
                Optional.ofNullable(requestUri)
                        .map(URI::getPort)
                        .map(port -> port > 0 ? String.valueOf(port) : "")
                        .orElseGet(() -> getPort(protocol)));

        replaceMap.put(CONTEXT_PATH, contextPath);

        replaceMap.put(TENANT_PLACEHOLDER, placeholder.tenant());
        replaceMap.put(CONTROLLER_ID_PLACEHOLDER, placeholder.controllerId());
        replaceMap.put(SOFTWARE_MODULE_ID_PLACEHOLDER, String.valueOf(placeholder.softwareModuleId()));
        replaceMap.put(ARTIFACT_FILENAME_PLACEHOLDER, URLEncoder.encode(placeholder.filename(), StandardCharsets.UTF_8));
        replaceMap.put(ARTIFACT_SHA1_PLACEHOLDER, placeholder.sha1());
        return replaceMap;
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

        final List<String> domainElements = Arrays.asList(StringUtils.delimitedListToStringArray(requestUri.getHost(), "."));
        if (domainElements.isEmpty()) {
            return protocol.getHostname();
        }

        final String domain = StringUtils.collectionToDelimitedString(domainElements.subList(1, domainElements.size()), ".");
        return StringUtils.delimitedListToStringArray(protocol.getHostname(), ".")[0].trim() + "." + domain;
    }
}