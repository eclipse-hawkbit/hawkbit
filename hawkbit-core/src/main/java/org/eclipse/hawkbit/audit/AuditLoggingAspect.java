/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.audit;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class AuditLoggingAspect {

    private static final String MASKED = "***";
    private static final String PATH_SEPARATOR = "/";
    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();
    // conditions are annotation constants - parse each once
    private static final Map<String, Expression> EXPRESSIONS = new ConcurrentHashMap<>();

    /**
     * Provides around advice for methods annotated with {@code @AuditLog}.
     * <p>
     * By default, all method parameters are logged. To restrict logging to specific parameters,
     * specify them via the {@code logParams} attribute (e.g., {@code  = {"param1", "param2"}}).
     * To disable parameter logging, use an empty array (i.e., {@code logParams = {}}).
     * </p>
     * <p>
     * Secrets in parameters must be masked via {@code masks} (see {@link AuditLog.Mask}) or not be included in the
     * object's {@code toString()} (e.g. with Lombok {@code @ToString}, annotate the secret fields with {@code @ToString.Exclude}).
     * </p>
     * <p>
     * This advice logs the request details and, if {@code logResponse} is set to {@code true},
     * logs the method response as well.
     * </p>
     */
    @Around("@annotation(auditLog)")
    public Object handleAuditLogging(final ProceedingJoinPoint joinPoint, final AuditLog auditLog) throws Throwable {
        try {
            final Object result = joinPoint.proceed();
            try { // log success
                final ResultMessage resultMessage = getResultMessage(result, auditLog);
                final String paramsToLog = getParamsToLog(joinPoint, auditLog, mask(joinPoint, auditLog).args());
                logAudit(joinPoint, auditLog, resultMessage.message(), paramsToLog, resultMessage.level());
            } catch (final Throwable logError) {
                // should never fail!
                log.error("Failed to log success", logError);
            }
            return result;
        } catch (final Throwable t) {
            try {
                final Masked masked = mask(joinPoint, auditLog);
                final String paramsToLog = getParamsToLog(joinPoint, auditLog, masked.args());
                // the exception message may contain (masked) argument values - e.g. a validation message quoting the value
                logAudit(joinPoint, auditLog, masked.obfuscate(t.getMessage()), paramsToLog, AuditLog.Level.ERROR);
            } catch (final Throwable logError) {
                // should never fail!
                log.error("Failed to log error", logError);
            }
            throw t;
        }
    }

    /**
     * Logs both the request details and the response.
     */
    private void logAudit(
            final JoinPoint joinPoint,
            final AuditLog auditLog, final String resultMessage, final String paramsToLog, final AuditLog.Level logLevel) {
        final String methodName = joinPoint.getSignature().getName();

        final String logMessage = String.format("Type: %s, Method: %s - Description: %s - Parameters: %s - Response: %s",
                auditLog.type(), methodName, auditLog.description(), paramsToLog, resultMessage);

        switch (logLevel) {
            case INFO:
                AuditLogger.info(auditLog.entity(), logMessage);
                break;
            case WARN:
                AuditLogger.warn(auditLog.entity(), logMessage);
                break;
            case ERROR:
                AuditLogger.error(auditLog.entity(), logMessage);
                break;
        }
    }

    private String getParamsToLog(final JoinPoint joinPoint, final AuditLog auditLog, final Object[] args) {
        final String[] logParams = auditLog.logParams();

        if (isLogAll(logParams)) {
            return Arrays.deepToString(args);
        } else {
            final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            final String[] paramNames = methodSignature.getParameterNames();
            // not Collectors.toMap - it doesn't accept null values (null arguments)
            final Map<String, Object> paramMap = new HashMap<>();
            IntStream.range(0, paramNames.length).forEach(i -> paramMap.put(paramNames[i], args[i]));

            return Arrays.stream(logParams)
                    .filter(paramMap::containsKey)
                    .map(name -> name + "=" + paramMap.get(name))
                    .collect(Collectors.joining(", "));
        }
    }

    /**
     * Returns the arguments with the {@link AuditLog#masks()} applied, together with the masked values. The real arguments are never
     * modified - masked containers are copies. On misconfiguration (unknown parameter, invalid condition) all arguments are masked -
     * rather lose audit details than leak a secret.
     */
    private static Masked mask(final JoinPoint joinPoint, final AuditLog auditLog) {
        final Object[] args = joinPoint.getArgs();
        if (auditLog.masks().length == 0) {
            return new Masked(args, Set.of());
        }

        final MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        final List<String> paramNames = Arrays.asList(signature.getParameterNames());
        // parameter names and the index aliases #a<index> / #p<index> MethodBasedEvaluationContext resolves too
        final Set<String> knownParams = new HashSet<>(paramNames);
        IntStream.range(0, args.length).forEach(i -> {
            knownParams.add("a" + i);
            knownParams.add("p" + i);
        });
        final EvaluationContext context = new MethodBasedEvaluationContext(null, signature.getMethod(), args, PARAMETER_NAME_DISCOVERER) {

            // SpEL resolves unknown variables to null - a misspelled parameter would silently make the condition false and
            // log the secret unmasked. Fail instead, so all parameters get masked (#this / #root don't go through here)
            @Override
            public Object lookupVariable(final String name) {
                if (!knownParams.contains(name)) {
                    throw new IllegalArgumentException("Unknown parameter #" + name + " in mask condition");
                }
                return super.lookupVariable(name);
            }
        };
        final Object[] masked = args.clone();
        final Set<String> secrets = new HashSet<>();
        try {
            for (final AuditLog.Mask mask : auditLog.masks()) {
                if (Boolean.TRUE.equals(EXPRESSIONS.computeIfAbsent(mask.when(), PARSER::parseExpression).getValue(context, Boolean.class))) {
                    for (final String path : mask.mask()) {
                        final String[] segments = path.split(PATH_SEPARATOR);
                        final int paramIndex = paramNames.indexOf(segments[0]);
                        if (paramIndex < 0) {
                            throw new IllegalArgumentException("Unknown parameter in mask path " + path);
                        }
                        masked[paramIndex] = mask(masked[paramIndex], segments, 1, secrets);
                    }
                }
            }
            return new Masked(masked, secrets);
        } catch (final RuntimeException e) {
            // all masked - so all argument values are secrets (the exception message, e.g. of a condition, may contain some of them)
            Arrays.stream(args).forEach(arg -> collectSecrets(arg, secrets));
            Arrays.fill(masked, MASKED);
            final Masked all = new Masked(masked, secrets);
            log.warn("Invalid @AuditLog masks of {}, mask all parameters: {}", signature.toShortString(), all.obfuscate(e.getMessage()));
            return all;
        }
    }

    // returns the value with the path (from segment index on) masked, collects the masked values in secrets
    private static Object mask(final Object value, final String[] segments, final int index, final Set<String> secrets) {
        if (index == segments.length || !(value instanceof Map<?, ?> map)) {
            // path end or can't go deeper - mask whole value
            collectSecrets(value, secrets);
            return MASKED;
        }
        // keys are matched by their string form - the path can address non-String keys (e.g. enums, numbers) too
        final Object key = map.containsKey(segments[index])
                ? segments[index]
                : map.keySet().stream().filter(k -> segments[index].equals(String.valueOf(k))).findFirst().orElse(null);
        if (key == null) {
            return value;
        }
        final Map<Object, Object> copy = new LinkedHashMap<>(map);
        copy.put(key, mask(map.get(key), segments, index + 1, secrets));
        return copy;
    }

    // collects the string forms of the value and, for containers, of their elements - the forms a secret may appear in a message
    private static void collectSecrets(final Object value, final Set<String> secrets) {
        collectSecrets(value, secrets, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    // visited (by identity) - a container (indirectly) containing itself would recurse endlessly
    private static void collectSecrets(final Object value, final Set<String> secrets, final Set<Object> visited) {
        if (value == null || !visited.add(value)) {
            return;
        }
        if (value instanceof Map<?, ?> map) {
            map.values().forEach(v -> collectSecrets(v, secrets, visited));
        } else if (value instanceof Collection<?> collection) {
            collection.forEach(v -> collectSecrets(v, secrets, visited));
        } else if (value instanceof Object[] array) {
            Arrays.stream(array).forEach(v -> collectSecrets(v, secrets, visited));
        }
        final String secret = value instanceof Object[] array ? Arrays.deepToString(array) : String.valueOf(value);
        if (!secret.isEmpty()) { // empty would "redact" between every character
            secrets.add(secret);
        }
    }

    private ResultMessage getResultMessage(final Object result, final AuditLog auditLog) {
        final ResultMessage.ResultMessageBuilder resultMessageBuilder = ResultMessage.builder();
        if (result instanceof ResponseEntity<?> responseEntity) {
            int statusCode = responseEntity.getStatusCode().value();
            if (statusCode >= 200 && statusCode < 300) {
                resultMessageBuilder.level(AuditLog.Level.INFO);
                if (auditLog.logResponse()) {
                    resultMessageBuilder.message(result.toString());
                } else {
                    resultMessageBuilder.message("OK - " + statusCode);
                }
            } else {
                resultMessageBuilder.level(AuditLog.Level.WARN);
                if (auditLog.logResponse()) {
                    resultMessageBuilder.message(result.toString());
                } else {
                    resultMessageBuilder.message("FAILED - " + statusCode);
                }
            }
            return resultMessageBuilder.build();
        }
        resultMessageBuilder.message(result != null ? result.toString() : "null");
        resultMessageBuilder.level(auditLog.level());
        return resultMessageBuilder.build();
    }

    private boolean isLogAll(final String[] logParams) {
        return Arrays.asList(logParams).contains("*");
    }

    /**
     * Arguments to log (with masks applied) and the masked values. The single place that redacts secrets from free texts (e.g.
     * exception messages) which may quote argument values.
     */
    private record Masked(Object[] args, Set<String> secrets) {

        private String obfuscate(final String text) {
            if (text == null || secrets.isEmpty()) {
                return text;
            }
            String redacted = text;
            // the longest first - a secret containing another one is redacted entirely
            for (final String secret : secrets.stream().sorted(Comparator.comparingInt(String::length).reversed()).toList()) {
                redacted = redacted.replace(secret, MASKED);
            }
            return redacted;
        }
    }

    @Builder
    private record ResultMessage(String message, AuditLog.Level level) {}
}
