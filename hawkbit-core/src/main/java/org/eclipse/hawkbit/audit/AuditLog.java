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

    String[] logParams() default { "*" };

    boolean logResponse() default false;

    /**
     * Mask rules applied to the parameters before they are logged. Rules are independent - each one whose {@link Mask#when()} matches
     * applies its {@link Mask#mask()} paths. Nothing is masked unless a rule says so.
     */
    Mask[] masks() default {};

    /**
     * Masks (parts of) parameters, e.g.:
     * <ul>
     *     <li>{@code @Mask(when = "#keyName == 'secret.key'", mask = "value")} - masks parameter {@code value} if parameter {@code keyName}
     *     is {@code secret.key}</li>
     *     <li>{@code @Mask(mask = "map/key1/key2")} - masks the value of {@code key2} in the map value of {@code key1} in the map
     *     parameter {@code map}</li>
     * </ul>
     */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Mask {

        /**
         * SpEL condition, evaluated against the method parameters (referenced as {@code #paramName}, or by index as
         * {@code #a0} / {@code #p0}). The rule applies only if it evaluates to {@code true}.
         */
        String when() default "true";

        /**
         * Paths to mask: {@code <param name>[/<map key>]*}. A path that ends at a parameter / map value masks it entirely. If a path
         * goes below a value that is not a {@link java.util.Map} the whole value is masked (it can't be partially masked). Missing map
         * keys are ignored.
         */
        String[] mask();
    }
}
