/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest.util;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Wires the deprecated-REST-API logging.
 * <p>
 * The advised packages are contributed by modules via {@link RestPackages} beans - each module may declare its own,
 * and their union is advised.
 */
@AutoConfiguration
public class LogAutoConfiguration {

    private final Map<Method, LogDecision> deprecatedStatus = new ConcurrentHashMap<>();

    @Bean
    @Conditional(LogEnabledCondition.class) // only if log is enabled
    @ConditionalOnBean(RestPackages.class)
        // and if some module contributed packages
    Advisor loggingAdvisor(final ObjectProvider<RestPackages> contributions) {
        // union of all module contributions - appended and de-duplicated, resolved lazily so contributor ordering does not matter
        final List<String> packages = contributions.stream()
                .flatMap(c -> c.patterns().stream())
                .filter(pkg -> pkg != null && !pkg.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (LogUtility.LOGGER.isDebugEnabled()) {
            LogUtility.LOGGER.info("REST logging advisor enabled for packages: {}", packages);
        } else if (LogUtility.DEPRECATED_LOGGER.isDebugEnabled()) {
            LogUtility.DEPRECATED_LOGGER.info("REST logging advisor (deprecated only mode) enabled for packages: {}", packages);
        }

        final AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression(packages.stream()
                .map(pkg -> "execution(* " + pkg + ".*.*(..))")
                .collect(Collectors.joining(" || ")));
        return new DefaultPointcutAdvisor(pointcut, (MethodInterceptor) invocation -> {
            final Method method = invocation.getMethod();
            switch (deprecatedStatus.computeIfAbsent(method, LogAutoConfiguration::logDecision)) {
                case YES:
                    LogUtility.logRequest("Called " + method);
                    break;
                case YES_DEPRECATED:
                    LogUtility.logRequestDeprecated("Usage of deprecated " + method + " method");
                    break;
            }
            return invocation.proceed();
        });
    }

    // option to define declarative some packages to log
    @Bean
    @ConditionalOnProperty("rest.packages")
    RestPackages deprecatedLogPackages(@Value("${rest.packages}") final String[] packages) {
        return new RestPackages(packages);
    }

    // A method is deprecated when, anywhere in its type hierarchy:
    // * it is a REST mapping method and
    // * it (or its declaring type) is @Deprecated
    // AnnotatedElementUtils resolves meta-annotations, so @GetMapping/@PostMapping/... count as @RequestMapping.
    private static LogDecision logDecision(final Method method) {
        boolean rest = false;
        boolean log = false;
        boolean deprecated = false;

        final Set<Class<?>> types = typeAndSuperTypes(method.getDeclaringClass());
        for (final Method candidate : hierarchyMethods(method, types)) {
            if (!rest) {
                rest = AnnotatedElementUtils.hasAnnotation(candidate, RequestMapping.class);
            }
            if (!deprecated) {
                deprecated = AnnotatedElementUtils.hasAnnotation(candidate, Deprecated.class)
                        || candidate.getDeclaringClass().isAnnotationPresent(Deprecated.class);
            }
            if (rest && deprecated) {
                return LogDecision.YES_DEPRECATED; // no matter if @Log annotated
            }

            if (!log) {
                log = AnnotatedElementUtils.hasAnnotation(candidate, Log.class)
                        || candidate.getDeclaringClass().isAnnotationPresent(Log.class);
            }
        } // after the method rest is finally resolved

        if (rest) { // check if there is a @Deprecated or @Log annotated class without this method declaration
            for (final Class<?> type : types) {
                if (type.isAnnotationPresent(Deprecated.class)) {
                    return LogDecision.YES_DEPRECATED;  // no matter if @Log annotated
                }
                if (!log) {
                    log = AnnotatedElementUtils.hasAnnotation(type, Log.class);
                }
            }

            if (log) {
                return LogDecision.YES;
            }
        }

        return LogDecision.NO;
    }

    // The method itself plus the matching method declared by every super class / interface.
    private static Set<Method> hierarchyMethods(final Method method, final Set<Class<?>> declaringTypeAndSuperTypes) {
        final Set<Method> methods = new LinkedHashSet<>();
        for (final Class<?> type : declaringTypeAndSuperTypes) {
            try {
                methods.add(type.getDeclaredMethod(method.getName(), method.getParameterTypes()));
            } catch (final NoSuchMethodException ignored) {
                // this supertype does not declare the method - skip
            }
        }
        return methods;
    }

    private static Set<Class<?>> typeAndSuperTypes(final Class<?> type) {
        final Set<Class<?>> all = new LinkedHashSet<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            all.add(c);
            addInterfaces(c, all);
        }
        return all;
    }

    private static void addInterfaces(final Class<?> type, final Set<Class<?>> targetSet) {
        for (final Class<?> i : type.getInterfaces()) {
            if (targetSet.add(i)) {
                addInterfaces(i, targetSet);
            }
        }
    }

    /**
     * A module's contribution of type patterns to advise. Declare one bean per module - all are collected and their
     * union is advised (appended, not overridden).
     * Each entry is an AspectJ type pattern, e.g. {@code org.eclipse.hawkbit.mgmt.rest.resource.} (package and sub-packages)
     * cor {@code org.eclipse.hawkbit.mgmt.rest.resource} (that package only).
     */
    public record RestPackages(List<String> patterns) {

        public RestPackages(final String... patterns) {
            this(List.of(patterns));
        }
    }

    private enum LogDecision {
        NO,
        YES,
        YES_DEPRECATED
    }

    static class LogEnabledCondition implements Condition {

        @Override
        public boolean matches(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
            final boolean matches = LogUtility.LOGGER.isDebugEnabled() || LogUtility.DEPRECATED_LOGGER.isDebugEnabled();
            if (!matches) {
                LogUtility.LOGGER.info("REST logging is disabled");
            }
            return matches;
        }
    }
}