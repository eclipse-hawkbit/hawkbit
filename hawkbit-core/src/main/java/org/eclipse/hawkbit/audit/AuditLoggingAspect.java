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

import lombok.Builder;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Aspect
@Component
public class AuditLoggingAspect {

    /**
     * Around advice that applies to methods annotated with @AuditLog.
     * It logs the request and, if logResponse is true, the response as well.
     */
    @Around("@annotation(auditLog)")
    public Object handleAuditLogging(final ProceedingJoinPoint joinPoint, final AuditLog auditLog) throws Throwable {
        String paramsToLog = getParamsToLog(joinPoint, auditLog);
        try {
            Object result = joinPoint.proceed();
            ResultMessage resultMessage = getResultMessage(result, auditLog);
            logAudit(joinPoint, auditLog, resultMessage.message(), paramsToLog, resultMessage.level());
            return result;
        } catch (Throwable t) {
            logAudit(joinPoint, auditLog, t.getMessage(), paramsToLog, AuditLog.Level.ERROR);
            throw t;
        }
    }

    /**
     * Logs both the request details and the response.
     */
    private void logAudit(final JoinPoint joinPoint, final AuditLog auditLog, final String resultMessage, final String paramsToLog, final AuditLog.Level logLevel) {
        String methodName = joinPoint.getSignature().getName();

        String logMessage = String.format(
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
        Object[] args = joinPoint.getArgs();
        String[] includeParams = auditLog.includeParams();

        if (includeParams.length == 0) {
            return Arrays.deepToString(args);
        } else {
            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = methodSignature.getParameterNames();
            Map<String, Object> paramMap = IntStream.range(0, paramNames.length)
                .boxed()
                .collect(Collectors.toMap(i -> paramNames[i], i -> args[i]));

            return Arrays.stream(includeParams)
                .filter(paramMap::containsKey)
                .map(name -> name + "=" + paramMap.get(name))
                .collect(Collectors.joining(", "));
        }
    }

    private ResultMessage getResultMessage(final Object result, final AuditLog auditLog) {
        ResultMessage.ResultMessageBuilder resultMessageBuilder = ResultMessage.builder();
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
    private record ResultMessage(String message, AuditLog.Level level) {
    }
}