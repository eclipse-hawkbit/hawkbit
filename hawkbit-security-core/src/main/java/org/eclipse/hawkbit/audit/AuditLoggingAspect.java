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
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class AuditLoggingAspect {

    /**
     * Around advice that applies to methods annotated with @AuditLog.
     * It logs the request and, if logResponse is true, the response as well.
     */
    @Around("@annotation(auditLog)")
    public Object handleAuditLogging(final ProceedingJoinPoint joinPoint, final AuditLog auditLog) throws Throwable {
        try {
            final Object result = joinPoint.proceed();
            try { // log success
                final ResultMessage resultMessage = getResultMessage(result, auditLog);
                final String paramsToLog = getParamsToLog(joinPoint, auditLog);
                logAudit(joinPoint, auditLog, resultMessage.message(), paramsToLog, resultMessage.level());
            } catch (final Throwable logError) {
                // should never fail!
                log.debug("Failed to log success", logError);
            }
            return result;
        } catch (final Throwable t) {
            try {
                final String paramsToLog = getParamsToLog(joinPoint, auditLog);
                logAudit(joinPoint, auditLog, t.getMessage(), paramsToLog, AuditLog.Level.ERROR);
            } catch (final Throwable logError) {
                // should never fail!
                log.debug("Failed to log error", logError);
            }
            throw t;
        }
    }

    /**
     * Logs both the request details and the response.
     */
    private void logAudit(
            final JoinPoint joinPoint, final AuditLog auditLog, final String resultMessage, final String paramsToLog,
            final AuditLog.Level logLevel) {
        final String methodName = joinPoint.getSignature().getName();

        final String logMessage = String.format(
                "Method: %s - Message: %s - Parameters: %s - Response: %s",
                methodName, auditLog.message(), paramsToLog, resultMessage
        );

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

    private String getParamsToLog(final JoinPoint joinPoint, final AuditLog auditLog) {
        final Object[] args = joinPoint.getArgs();
        final String[] includeParams = auditLog.logParams();

        if (includeParams.length == 0) {
            return Arrays.deepToString(args);
        } else {
            final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            final String[] paramNames = methodSignature.getParameterNames();
            final Map<String, Object> paramMap = IntStream.range(0, paramNames.length)
                    .boxed()
                    .collect(Collectors.toMap(i -> paramNames[i], i -> args[i]));

            return Arrays.stream(includeParams)
                    .filter(paramMap::containsKey)
                    .map(name -> name + "=" + paramMap.get(name))
                    .collect(Collectors.joining(", "));
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

    @Builder
    private record ResultMessage(String message, AuditLog.Level level) {}
}