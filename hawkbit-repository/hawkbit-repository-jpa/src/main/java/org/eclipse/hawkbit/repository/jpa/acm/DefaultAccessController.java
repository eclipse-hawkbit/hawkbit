/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.acm;

import static org.eclipse.hawkbit.security.SecurityContextTenantAware.SYSTEM_USER;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.ContextAware;
import org.eclipse.hawkbit.repository.RsqlQueryField;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.jpa.ql.EntityMatcher;
import org.eclipse.hawkbit.repository.jpa.rsql.RsqlUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.ObjectUtils;

@Slf4j
public class DefaultAccessController<A extends Enum<A> & RsqlQueryField, T> implements AccessController<T> {

    private final Class<A> rsqlQueryFieldType;
    private final Map<Operation, List<String>> permissions = new EnumMap<>(Operation.class);

    private ContextAware contextAware;

    public DefaultAccessController(final Class<A> rsqlQueryFieldType, final String... permissionTypes) {
        if (ObjectUtils.isEmpty(permissionTypes)) {
            throw new IllegalArgumentException("Permission types must not be empty");
        }

        this.rsqlQueryFieldType = rsqlQueryFieldType;
        for (final Operation operation : Operation.values()) {
            for (final String permissionType : permissionTypes) {
                permissions.computeIfAbsent(operation, k -> new ArrayList<>()).add(operation.name() + "_" + permissionType.toUpperCase());
            }
        }
    }

    @Autowired
    void setContextAware(final ContextAware contextAware) {
        this.contextAware = contextAware;
    }

    @Override
    public Optional<Specification<T>> getAccessRules(final Operation operation) {
        if (contextAware.getCurrentTenant() != null && SYSTEM_USER.equals(contextAware.getCurrentUsername())) {
            // as tenant, no restrictions
            return Optional.empty();
        }

        return Optional.ofNullable(getScopes(operation)) // if get scopes returns null, no scopes return no spec - all entities are accessible
                .map(scopes -> // to RSQL
                        scopes.size() == 1
                                ? scopes.get(0) // single scope
                                : "(" + String.join(") or (", scopes) + ")") // join multiple scopes with 'or' - union
                .map(scope -> RsqlUtility.getInstance().buildRsqlSpecification(scope, rsqlQueryFieldType));
    }

    @Override
    public void assertOperationAllowed(final Operation operation, final T entity) throws InsufficientPermissionException {
        if (contextAware.getCurrentTenant() != null && SYSTEM_USER.equals(contextAware.getCurrentUsername())) {
            // as tenant, no restrictions
            return;
        }

        final List<String> scopes = getScopes(operation);
        if (scopes != null) {
            for (final String scope : scopes) {
                if (EntityMatcher.forRsql(scope).match(entity)) {
                    return; // at least one scope matches, operation is allowed
                }
            }
            throw new InsufficientPermissionException(String.format("Operation '%s' is not allowed", operation));
        } // else if scopes is null, no scopes are defined, so all entities are accessible
    }

    // returns null if ALL entities are accessible, otherwise returns a list of scopes
    // throws InsufficientPermissionException if no matching authority found (should not happen - should be already checked with @PreAuthorize)
    @SuppressWarnings("java:S1168") // java:S1168 - returns null with purpose to indicate no scopes, privately used with attention
    private List<String> getScopes(final Operation operation) {
        final List<String> operationPermissions = permissions.get(operation);
        final List<String> scopes = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(Permission::from)
                .filter(permission -> operationPermissions.contains(permission.name()))
                .map(Permission::scope)
                .distinct() // remove duplicates
                .toList();
        if (scopes.isEmpty()) {
            // no matching permission scope found for the operation
            // the required for the method permissions should have already been checked with @PreAuthorize
            // however it could happen that there is no entity permission, e.g.:
            // * in controller management, that checks ROLE_CONTROLLER and on its behalf calls pure repository methods as privileged
            // * in case the entity permission(s) are implied - e.g. there is READ_REPOSITORY which implies READ_DISTRIBUTION_SET
            log.debug(
                    "[{}] No matching authority found for operation {} (expects {}), they shall have already been checked with @PreAuthorize)",
                    rsqlQueryFieldType, operation, operationPermissions);
            return null;
        } else if (scopes.contains(null)) {
            return null; // not scoped at all
        } else {
            return scopes;
        }
    }

    private record Permission(String name, String scope) {

        private static final Pattern PATTERN = Pattern.compile("^(?<name>[^/]+)(/(?<scope>.+))?$");

        static Permission from(final String authority) {
            final Matcher matcher = PATTERN.matcher(authority);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid authority format: " + authority);
            }
            return from(matcher);
        }

        static Permission from(final Matcher matcher) {
            return new Permission(matcher.group("name"), matcher.group("scope"));
        }
    }
}