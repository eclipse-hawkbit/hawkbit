/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.auth.PermissionTracker;
import org.eclipse.hawkbit.rest.RequiredPermissions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Verifies that the {@link RequiredPermissions} of the Management API endpoints match the permissions actually granted by the permission
 * checks during the requests. Enabled via {@value #ENABLED_PROPERTY} (off by default - meant for tests / diagnostics).
 * <p/>
 * For every request the permission tracking ({@link PermissionTracker}) is started, and after a successful request every granted
 * permission must be covered (equal or implied via the role hierarchy) by a declared one - {@link RequiredPermissions#value()},
 * {@link RequiredPermissions#anyOf()} or {@link RequiredPermissions#optional()}. Otherwise, it is a violation that is logged and
 * collected (see {@link #drainViolations()}).
 * <p/>
 * Declared {@link RequiredPermissions#value()} permissions and {@link RequiredPermissions#anyOf()} groups that never covered a granted
 * permission can't be detected per request (some checks are done only on some paths). They are collected for all requests and reported
 * (see {@link #unusedDeclarations()}), and logged on shutdown.
 */
@Slf4j
@Component
@ConditionalOnProperty(value = MgmtRequiredPermissionsVerifier.ENABLED_PROPERTY, havingValue = "true")
public class MgmtRequiredPermissionsVerifier implements HandlerInterceptor, WebMvcConfigurer {

    public static final String ENABLED_PROPERTY = PermissionTracker.ENABLED_PROPERTY;

    private final RoleHierarchy roleHierarchy;
    private final Map<String, Set<String>> reachableCache = new ConcurrentHashMap<>();
    // a set - so a recurring mismatch is kept once (e.g. if enabled for a running server where the violations are not drained)
    private final Set<String> violations = new LinkedHashSet<>();
    private final Map<Method, Usage> usages = new ConcurrentHashMap<>();

    public MgmtRequiredPermissionsVerifier(final RoleHierarchy roleHierarchy) {
        this.roleHierarchy = roleHierarchy;
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(this);
    }

    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) {
        if (isMgmtEndpoint(handler)) {
            PermissionTracker.start();
        }
        return true;
    }

    @Override
    public void afterCompletion(
            final HttpServletRequest request, final HttpServletResponse response, final Object handler, final Exception ex) {
        if (isMgmtEndpoint(handler)) {
            final HandlerMethod handlerMethod = (HandlerMethod) handler;
            final Set<String> granted = PermissionTracker.stop();
            // only successful requests passed all the checks, failed could be interrupted at any point
            if (ex == null && response.getStatus() < 400) {
                verify(handlerMethod.getMethod(), granted);
            }
        }
    }

    /**
     * @return the distinct violations (granted, but not declared permissions) collected since the last call, and clears them
     */
    public List<String> drainViolations() {
        synchronized (violations) {
            final List<String> drained = List.copyOf(violations);
            violations.clear();
            return drained;
        }
    }

    /**
     * @return the declared {@link RequiredPermissions#value()} permissions and {@link RequiredPermissions#anyOf()} groups which haven't
     *         covered any granted permission in any of the (successful) requests to the endpoint so far
     */
    public List<String> unusedDeclarations() {
        return unusedDeclarations(List.of(this));
    }

    /**
     * Same as {@link #unusedDeclarations()} but over the requests of all given verifiers, e.g. of different application contexts
     * (as with tests with different configurations). A declaration is unused only if it is unused in all of them.
     *
     * @param verifiers the verifiers to aggregate
     * @return the declarations which haven't covered any granted permission in any of the (successful) requests of the verifiers
     */
    public static List<String> unusedDeclarations(final Collection<MgmtRequiredPermissionsVerifier> verifiers) {
        final Map<Method, Usage> merged = new HashMap<>();
        verifiers.forEach(verifier -> verifier.usages.forEach((method, usage) -> merged.merge(method, usage, Usage::merge)));
        return merged.entrySet().stream()
                .flatMap(entry -> entry.getValue().unused().stream().map(unused -> endpoint(entry.getKey()) + ": " + unused))
                .sorted()
                .toList();
    }

    @PreDestroy
    void logUnusedDeclarations() {
        final List<String> unused = unusedDeclarations();
        if (!unused.isEmpty()) {
            log.warn("Declared required permissions that were never checked:\n  {}", String.join("\n  ", unused));
        }
    }

    private void verify(final Method method, final Set<String> granted) {
        final RequiredPermissions requiredPermissions = AnnotatedElementUtils.findMergedAnnotation(method, RequiredPermissions.class);
        if (requiredPermissions == null) {
            addViolation(endpoint(method) + ": no @RequiredPermissions, granted " + granted);
            return;
        }

        final Usage usage = usages.computeIfAbsent(method, k -> new Usage(requiredPermissions));
        final Set<String> notDeclared = new TreeSet<>();
        for (final String permission : granted) {
            boolean covered = false;
            for (final String declared : requiredPermissions.value()) {
                if (implies(declared, permission)) {
                    usage.usedValues.add(declared);
                    covered = true;
                }
            }
            final RequiredPermissions.AnyOf[] anyOf = requiredPermissions.anyOf();
            for (int i = 0; i < anyOf.length; i++) {
                for (final String declared : anyOf[i].value()) {
                    if (implies(declared, permission)) {
                        usage.usedAnyOf.add(i);
                        covered = true;
                    }
                }
            }
            for (final String declared : requiredPermissions.optional()) {
                covered |= implies(declared, permission);
            }
            if (!covered) {
                notDeclared.add(permission);
            }
        }
        if (!notDeclared.isEmpty()) {
            addViolation(endpoint(method) + ": granted but not declared " + notDeclared);
        }
    }

    private void addViolation(final String violation) {
        log.warn("Required permissions mismatch - {}", violation);
        synchronized (violations) {
            violations.add(violation);
        }
    }

    // true if the declared permission is or implies (via the role hierarchy) the granted one
    private boolean implies(final String declared, final String granted) {
        return declared.equals(granted) || reachableCache.computeIfAbsent(declared, k -> roleHierarchy
                .getReachableGrantedAuthorities(List.of(new SimpleGrantedAuthority(k))).stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet())).contains(granted);
    }

    // e.g. in the monolith the other APIs (DDI) are served by the same dispatcher - only the Management API is verified
    private static boolean isMgmtEndpoint(final Object handler) {
        return handler instanceof HandlerMethod handlerMethod &&
                handlerMethod.getBeanType().getPackageName().startsWith(MgmtRequiredPermissionsVerifier.class.getPackageName());
    }

    private static String endpoint(final Method method) {
        return method.getDeclaringClass().getSimpleName() + "#" + method.getName();
    }

    private static final class Usage {

        private final RequiredPermissions requiredPermissions;
        private final Set<String> usedValues = ConcurrentHashMap.newKeySet();
        private final Set<Integer> usedAnyOf = ConcurrentHashMap.newKeySet();

        private Usage(final RequiredPermissions requiredPermissions) {
            this.requiredPermissions = requiredPermissions;
        }

        // returns a new usage, union of the both
        private Usage merge(final Usage other) {
            final Usage merged = new Usage(requiredPermissions);
            merged.usedValues.addAll(usedValues);
            merged.usedValues.addAll(other.usedValues);
            merged.usedAnyOf.addAll(usedAnyOf);
            merged.usedAnyOf.addAll(other.usedAnyOf);
            return merged;
        }

        private List<String> unused() {
            final List<String> unused = new ArrayList<>();
            Arrays.stream(requiredPermissions.value())
                    .filter(declared -> !usedValues.contains(declared))
                    .forEach(declared -> unused.add("never checked " + declared));
            IntStream.range(0, requiredPermissions.anyOf().length)
                    .filter(i -> !usedAnyOf.contains(i))
                    .forEach(i -> unused.add("never checked any of " + Arrays.toString(requiredPermissions.anyOf()[i].value())));
            return unused;
        }
    }
}
