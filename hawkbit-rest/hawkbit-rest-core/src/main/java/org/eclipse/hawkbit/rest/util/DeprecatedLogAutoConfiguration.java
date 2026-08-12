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
 * The advised packages are contributed by modules via {@link DeprecatedLogPackages} beans - each module declares its own,
 * and their union is advised.<br/>
 * Only wired when the {@code DEPRECATED_USAGE} logger is at DEBUG and at least one {@link DeprecatedLogPackages} bean is present.
 */
@AutoConfiguration
@Conditional(DeprecatedLogAutoConfiguration.LoggingEnabledCondition.class) // only if log is enabled
public class DeprecatedLogAutoConfiguration {

    private final Map<Method, Boolean> deprecatedStatus = new ConcurrentHashMap<>();

    @Bean
    @ConditionalOnBean(DeprecatedLogAutoConfiguration.DeprecatedLogPackages.class) // and if some module contributed packages
    Advisor deprecationLoggingAdvisor(final ObjectProvider<DeprecatedLogPackages> contributions) {
        // union of all module contributions - appended and de-duplicated, resolved lazily so contributor ordering does not matter
        final List<String> packages = contributions.stream()
                .flatMap(c -> c.patterns().stream())
                .filter(pkg -> pkg != null && !pkg.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        LogUtility.LOGGER.info("Deprecated logging advisor enabled for packages: {}", packages);

        final AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression(packages.stream()
                .map(pkg -> "execution(* " + pkg + ".*.*(..))")
                .collect(Collectors.joining(" || ")));
        return new DefaultPointcutAdvisor(pointcut, (MethodInterceptor) invocation -> {
            final Method method = invocation.getMethod();
            if (deprecatedStatus.computeIfAbsent(method, DeprecatedLogAutoConfiguration::resolveDeprecated)) {
                LogUtility.logDeprecated("Usage of " + method + ": result that is up to modification.");
            }
            return invocation.proceed();
        });
    }

    // option to define declarative some packages to log
    @Bean
    @ConditionalOnProperty("deprecated.usage.logging.packages")
    DeprecatedLogPackages deprecatedLogPackages(@Value("${deprecated.usage.logging.packages}") final String[] packages) {
        return new DeprecatedLogPackages(packages);
    }

    // A method is deprecated when, anywhere in its type hierarchy:
    // * it is a REST mapping method and
    // * it (or its declaring type) is @Deprecated
    // AnnotatedElementUtils resolves meta-annotations, so @GetMapping/@PostMapping/... count as @RequestMapping.
    private static boolean resolveDeprecated(final Method method) {
        boolean rest = false;
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
                return true;
            }
        }
        if (rest && !deprecated) { // check if there is a @Deprecated anontated class with not method declaration
            for (final Class<?> type : types) {
                if (type.isAnnotationPresent(Deprecated.class)) {
                    return true;
                }
            }
        }
        return false;
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
    public record DeprecatedLogPackages(List<String> patterns) {

        public DeprecatedLogPackages(final String... patterns) {
            this(List.of(patterns));
        }
    }

    /**
     * Only wire the advisor when deprecated-usage logging is actually enabled, so no interception is added otherwise.
     * Evaluated once at context startup - a log level toggled at runtime afterwards is not picked up.
     */
    static class LoggingEnabledCondition implements Condition {

        @Override
        public boolean matches(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
            return LogUtility.LOGGER.isDebugEnabled();
        }
    }
}