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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AuditLog {

    enum Level {
        INFO, WARN, ERROR
    }

    enum Type {
        CREATE, READ, UPDATE, DELETE, EXECUTE
    }

    Level level() default Level.INFO;

    Type type();

    String entity();

    String description() default "";

    String[] logParams() default {"*"};

    boolean logResponse() default false;
}