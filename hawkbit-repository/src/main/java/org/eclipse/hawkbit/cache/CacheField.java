/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.cache;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.eclipse.hawkbit.eventbus.CacheFieldEntityListener;
import org.springframework.cache.CacheManager;
import org.springframework.data.annotation.Transient;

/**
 * Marks an field within a JPA entity as transient and this field should be
 * loaded from a configured {@link CacheManager} by using the JPA entity
 * listeners.
 *
 *
 *
 * @see CacheFieldEntityListener
 */
@Target({ FIELD })
@Retention(RUNTIME)
@Transient
public @interface CacheField {

    /**
     * @return the cache key name for this cacheable field which is used to
     *         store the value.
     */
    String key();

}
