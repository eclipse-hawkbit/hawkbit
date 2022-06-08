/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.hawkbit.cache.DownloadIdCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.util.AntPathMatcher;

/**
 * Extracts download or upload id from the request URI secruity token and set
 * the security context.
 * 
 *
 *
 */
public class HttpDownloadAuthenticationFilter extends AbstractPreAuthenticatedProcessingFilter {

    private static final Logger LOG = LoggerFactory.getLogger(HttpDownloadAuthenticationFilter.class);

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private final DownloadIdCache downloadIdCache;

    /**
     * Constructor.
     * 
     * @param downloadIdCache
     *            the cache
     */
    public HttpDownloadAuthenticationFilter(final DownloadIdCache downloadIdCache) {
        this.downloadIdCache = downloadIdCache;
    }

    private Object getDownloadByUri(final String requestURI) {
        if (!MATCHER.match("**/downloadId/**",requestURI)) {
            return null;
        }
        LOG.debug("retrieving id from URI request {}", requestURI);
        final String[] groups = requestURI.split("\\/");
        final String id = groups[groups.length - 1];
        if (id == null) {
            return null;
        }
        return downloadIdCache.get(id);
    }

    @Override
    protected Object getPreAuthenticatedPrincipal(final HttpServletRequest request) {
        return getDownloadByUri(request.getRequestURI());
    }

    @Override
    protected Object getPreAuthenticatedCredentials(final HttpServletRequest request) {
        return getDownloadByUri(request.getRequestURI());
    }
}
