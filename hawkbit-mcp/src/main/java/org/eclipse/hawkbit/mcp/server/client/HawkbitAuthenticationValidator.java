/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mcp.server.client;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.mcp.server.config.HawkbitMcpProperties;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTenantManagementRestApi;
import org.eclipse.hawkbit.sdk.HawkbitClient;
import org.eclipse.hawkbit.sdk.Tenant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Validates authentication credentials against hawkBit REST API using the SDK.
 * This validator is conditionally created when {@code hawkbit.mcp.validation.enabled=true}.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "hawkbit.mcp.validation.enabled", havingValue = "true", matchIfMissing = true)
public class HawkbitAuthenticationValidator implements AuthenticationValidator {

    private final HawkbitClient hawkbitClient;
    private final Tenant dummyTenant;
    private final Cache<String, Boolean> validationCache;

    public HawkbitAuthenticationValidator(final HawkbitClient hawkbitClient,
                                          final Tenant dummyTenant,
                                          final HawkbitMcpProperties properties) {
        this.hawkbitClient = hawkbitClient;
        this.dummyTenant = dummyTenant;

        this.validationCache = Caffeine.newBuilder()
                .expireAfterWrite(properties.getValidation().getCacheTtl())
                .maximumSize(properties.getValidation().getCacheMaxSize())
                .build();

        log.info("Authentication validation enabled with cache TTL={}, maxSize={}",
                properties.getValidation().getCacheTtl(),
                properties.getValidation().getCacheMaxSize());
    }

    /**
     * Validates the given authorization header against hawkBit.
     *
     * @param authHeader the Authorization header value
     * @return validation result
     */
    @Override
    public ValidationResult validate(final String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return ValidationResult.MISSING_CREDENTIALS;
        }

        String cacheKey = hashAuthHeader(authHeader);
        Boolean cachedResult = validationCache.getIfPresent(cacheKey);

        if (cachedResult != null) {
            log.debug("Authentication validation cache hit: valid={}", cachedResult);
            return cachedResult ? ValidationResult.VALID : ValidationResult.INVALID_CREDENTIALS;
        }

        return validateWithHawkbit(cacheKey);
    }

    private ValidationResult validateWithHawkbit(final String cacheKey) {
        log.debug("Validating authentication against hawkBit using SDK");

        try {
            MgmtTenantManagementRestApi tenantApi = hawkbitClient.mgmtService(
                    MgmtTenantManagementRestApi.class, dummyTenant);

            ResponseEntity<?> response = tenantApi.getTenantConfiguration();
            int statusCode = response.getStatusCode().value();

            if (statusCode >= 200 && statusCode < 300) {
                log.debug("Authentication valid (status={})", statusCode);
                validationCache.put(cacheKey, true);
                return ValidationResult.VALID;
            } else {
                log.warn("Unexpected status from hawkBit during auth validation: {}", statusCode);
                return ValidationResult.HAWKBIT_ERROR;
            }
        } catch (FeignException.Unauthorized e) {
            log.debug("Authentication invalid (status=401)");
            validationCache.put(cacheKey, false);
            return ValidationResult.INVALID_CREDENTIALS;
        } catch (FeignException.Forbidden e) {
            // 403 = Valid credentials but lacks READ_TENANT_CONFIGURATION permission
            // User is authenticated in hawkBit but doesn't have this specific permission
            log.debug("Authentication valid but lacks permission (status=403)");
            validationCache.put(cacheKey, true);
            return ValidationResult.VALID;
        } catch (FeignException e) {
            log.warn("Error validating authentication against hawkBit: {} - {}",
                    e.getClass().getSimpleName(), e.getMessage());
            return ValidationResult.HAWKBIT_ERROR;
        } catch (Exception e) {
            // Unexpected errors, don't cache, fail closed
            log.warn("Unexpected error validating authentication against hawkBit: {}", e.getMessage());
            return ValidationResult.HAWKBIT_ERROR;
        }
    }

    private String hashAuthHeader(final String authHeader) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(authHeader.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available
            throw new McpAuthenticationException("SHA-256 not available." + e.getMessage());
        }
    }
}
