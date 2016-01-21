/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.model.TenantConfiguration;
import org.eclipse.hawkbit.rest.resource.model.system.CacheRest;
import org.eclipse.hawkbit.rest.resource.model.system.TenantConfigurationRest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@link SystemManagement} capabilities by REST.
 * 
 *
 *
 *
 */
@RestController
@RequestMapping(RestConstants.SYSTEM_ADMIN_MAPPING)
@Transactional(readOnly = true)
public class SystemManagementResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemManagementResource.class);

    @Autowired
    private SystemManagement systemManagement;

    @Autowired
    private CacheManager cacheManager;

    /**
     * Deletes the tenant data of a given tenant. USE WITH CARE!
     * 
     * @param tenant
     *            to delete
     * @return HttpStatus.OK
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/tenants/{tenant}")
    @Transactional
    public ResponseEntity<Void> deleteTenant(@PathVariable final String tenant) {
        systemManagement.deleteTenant(tenant);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Returns a list of all caches containing currently.
     * 
     * @return a list of caches for all tenants
     */
    @RequestMapping(method = RequestMethod.GET, value = "/caches")
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_SYSTEM_ADMIN)
    public ResponseEntity<Collection<CacheRest>> getCaches() {
        final Collection<String> cacheNames = cacheManager.getCacheNames();
        return ResponseEntity.ok(cacheNames.stream().map(cacheName -> cacheManager.getCache(cacheName))
                .map(cache -> cacheRest(cache)).collect(Collectors.toList()));
    }

    /**
     * Invalidates all caches for all tenants.
     * 
     * @return a list of cache names which has been invalidated
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/caches")
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_SYSTEM_ADMIN)
    public ResponseEntity<Collection<String>> invalidateCaches() {
        final Collection<String> cacheNames = cacheManager.getCacheNames();
        LOGGER.info("Invalidating caches {}", cacheNames);
        cacheNames.forEach(cacheName -> cacheManager.getCache(cacheName).clear());
        return ResponseEntity.ok(cacheNames);
    }

    /**
     * Adds or updates a configuration for a specific tenant to the tenant
     * configuration.
     * 
     * @param configuration
     *            the configuration value to add or update
     * @param key
     *            the key of the configuration to add or update
     * @return the response entity with status OK.
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/conf/{key}")
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_SYSTEM_ADMIN)
    public ResponseEntity<Void> addUpdateConfig(@RequestBody final TenantConfigurationRest configuration,
            @PathVariable final String key) {
        systemManagement.addOrUpdateConfiguration(new TenantConfiguration(key, configuration.getValue()));
        return ResponseEntity.ok().build();
    }

    private CacheRest cacheRest(final Cache cache) {
        final Object nativeCache = cache.getNativeCache();
        if (nativeCache instanceof com.google.common.cache.Cache) {
            return guavaCache(cache, nativeCache);
        } else {
            return new CacheRest(cache.getName(), Collections.emptyList());
        }
    }

    @SuppressWarnings("unchecked")
    private CacheRest guavaCache(final Cache cache, final Object nativeCache) {
        final com.google.common.cache.Cache<Object, Object> guavaCache = (com.google.common.cache.Cache<Object, Object>) nativeCache;
        final List<String> keys = guavaCache.asMap().keySet().stream().map(key -> key.toString())
                .collect(Collectors.toList());
        return new CacheRest(cache.getName(), keys);
    }
}
