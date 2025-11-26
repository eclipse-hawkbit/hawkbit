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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AuditLogger {

    private static final AuditContextProvider AUDIT_CONTEXT_PROVIDER = AuditContextProvider.getInstance();

    public static void info(final String entity, final String message) {
        logMessage(entity, message, AuditLog.Level.INFO);
    }

    public static void info(final String tenant, final String username, final String entity, final String message) {
        logMessage(tenant, username, entity, message, AuditLog.Level.INFO);
    }

    public static void error(final String entity, final String message) {
        logMessage(entity, message, AuditLog.Level.ERROR);
    }

    public static void error(final String tenant, final String username, final String entity, final String message) {
        logMessage(tenant, username, entity, message, AuditLog.Level.ERROR);
    }

    public static void warn(final String entity, final String message) {
        logMessage(entity, message, AuditLog.Level.WARN);
    }

    public static void warn(final String tenant, final String username, final String entity, final String message) {
        logMessage(tenant, username, entity, message, AuditLog.Level.WARN);
    }

    private static void logMessage(final String entity, final String message, final AuditLog.Level level) {
        logMessage(AUDIT_CONTEXT_PROVIDER.getAuditContext().tenant(), AUDIT_CONTEXT_PROVIDER.getAuditContext().username(),
                entity, message, level);
    }

    private static void logMessage(
            final String tenant, final String username, final String entity, final String message, final AuditLog.Level level) {
        final String logMessage = String.format("[%s] User: %s, AccessContext: %s - %s", entity, username, tenant, message);
        final Logger auditLogger = LoggerFactory.getLogger("AUDIT" + (entity != null ? ("." + entity.toUpperCase()) : ""));
        switch (level) {
            case INFO:
                auditLogger.info(logMessage);
                break;
            case WARN:
                auditLogger.warn(logMessage);
                break;
            case ERROR:
                auditLogger.error(logMessage);
                break;
        }
    }
}