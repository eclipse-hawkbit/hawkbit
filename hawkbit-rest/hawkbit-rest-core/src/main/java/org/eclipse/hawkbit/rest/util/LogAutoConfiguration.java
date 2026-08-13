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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import lombok.Data;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
@EnableConfigurationProperties(LogAutoConfiguration.Properties.class)
public class LogAutoConfiguration {

    private final Properties properties;
    private final Map<Method, LogDecision> deprecatedStatus = new ConcurrentHashMap<>();

    LogAutoConfiguration(final Properties properties) {
        this.properties = properties;
    }

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
            switch (deprecatedStatus.computeIfAbsent(method, this::logDecision)) {
                case YES:
                    LogUtility.logRequest("Call of " + method);
                    break;
                case YES_DEPRECATED:
                    LogUtility.logRequestDeprecated("Call of deprecated " + method + " method");
                    break;
                case NO:
                    break; // skip
            }
            return invocation.proceed();
        });
    }

    // option to define declarative some packages to log
    @Bean
    @ConditionalOnExpression(
            "#{!T(org.springframework.util.ObjectUtils).isEmpty('${" + Properties.PREFIX + ".packages:}') || !T(org.springframework.util.ObjectUtils).isEmpty('${" + Properties.PREFIX + ".packages[0]:}')}")
    RestPackages deprecatedLogPackages() {
        return new RestPackages(properties.getPackages());
    }

    // A method is deprecated when, anywhere in its type hierarchy:
    // * it is a REST mapping method and
    // * it (or its declaring type) is @Deprecated
    // AnnotatedElementUtils resolves meta-annotations, so @GetMapping/@PostMapping/... count as @RequestMapping.
    @SuppressWarnings("java:S3776") // cyclomatic complexity is high, but the logic is clear and readable
    private LogDecision logDecision(final Method method) {
        boolean rest = false;
        boolean log = false;
        boolean deprecated = false;

        for (final Class<?> type : typeAndSuperTypes(method.getDeclaringClass())) { // traverse this (first and supper) for decision
            final Method declared = byPatternIn(method, type);
            if (declared != null) { // if there is a method declared - check annotations there
                rest = rest || AnnotatedElementUtils.hasAnnotation(declared, RequestMapping.class);
                deprecated = deprecated || hasAnnotation(declared, Deprecated.class);
                if (rest && deprecated) {
                    return LogDecision.YES_DEPRECATED; // no matter if @Log annotated, early return
                }

                log = log || hasAnnotation(declared, Log.class);
            }
            // check class annotations, RequestMapping is also a type annotation but doesn't make methods magically rest endpoints
            deprecated = deprecated || hasAnnotation(type, Deprecated.class);
            if (rest && deprecated) {
                return LogDecision.YES_DEPRECATED; // no matter if @Log annotated, early return
            }
            log = log || hasAnnotation(type, Log.class);
        }

        return rest && log ? LogDecision.YES : LogDecision.NO;
    }

    private boolean hasAnnotation(final Method declared, final Class<? extends Annotation> annotation) {
        return AnnotatedElementUtils.hasAnnotation(declared, annotation)
                || properties.getDeclarativeAnnotations().getOrDefault(annotation.getName(), Set.of()).contains(
                declared.getDeclaringClass().getName() + "." + declared.getName() + Arrays.stream(declared.getParameterTypes())
                        .map(Type::getTypeName)
                        .collect(Collectors.joining(",", "(", ")")));
    }

    private boolean hasAnnotation(final Class<?> type, final Class<? extends Annotation> annotation) {
        return type.isAnnotationPresent(annotation)
                || properties.getDeclarativeAnnotations().getOrDefault(annotation.getName(), Set.of()).contains(type.getName());
    }

    private static Method byPatternIn(final Method method, final Class<?> type) {
        try {
            return type.getDeclaredMethod(method.getName(), method.getParameterTypes());
        } catch (final NoSuchMethodException ignored) {
            // this supertype does not declare the method - skip
            return null;
        }
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

    @Data
    @ConfigurationProperties(Properties.PREFIX)
    static class Properties {

        private static final String PREFIX = "hawkbit.rest.log";

        // used to declaratively ADD rest packages
        String[] packages;
        // used to configure declarative (out of the source annotation just for logging purposes), e.g.:
        // hawkbit.rest.log.declarative-annotations.org.eclipse.hawkbit.rest.util.Log[0]=org.eclipse.hawkbit.mgmt.rest.resource.MgmtTargetResource
        // hawkbit.rest.log.declarative-annotations.org.eclipse.hawkbit.rest.util.Log[1]=org.eclipse.hawkbit.mgmt.rest.resource.MgmtActionResource.getActions(java.lang.String,int,int,java.lang.String,java.lang.String)
        // ...
        // note that method signatures contains commas! so declaring as comma separated list in .properties won't work
        Map<String, Set<String>> declarativeAnnotations = new LinkedHashMap<>();
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

    private enum LogDecision {
        NO,
        YES,
        YES_DEPRECATED
    }
}