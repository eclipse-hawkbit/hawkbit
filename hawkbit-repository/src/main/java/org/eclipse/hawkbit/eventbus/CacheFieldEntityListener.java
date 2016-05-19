/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.eventbus;

import java.io.Serializable;
import java.lang.reflect.Field;

import javax.persistence.PostLoad;
import javax.persistence.PostRemove;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.eclipse.hawkbit.cache.CacheField;
import org.eclipse.hawkbit.cache.CacheKeys;
import org.eclipse.hawkbit.repository.model.helper.CacheManagerHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.hateoas.Identifiable;

/**
 * An JPA entity listener which enriches the JPA entity fields which are
 * annotated with {@link CacheField} with values from the {@link CacheManager}.
 * Only JPA entities which are implementing {@link Identifiable} can be handled
 * by this entity listener cause the cache keys are calculated with the ID of
 * the entity.
 *
 */
public class CacheFieldEntityListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheFieldEntityListener.class);

    /**
     * enriches the JPA entities after loading the entity from the SQL database.
     * 
     * @param target
     *            the target which has been loaded from the database
     */
    @PostLoad
    public void postLoad(final Object target) {
        if (!isIdentifiable(target)) {
            return;
        }

        final CacheFieldCallback cacheFieldCallback = (type, field, cacheKey, entityId) -> {
            final CacheManager cacheManager = CacheManagerHolder.getInstance().getCacheManager();
            final Cache cache = cacheManager.getCache(type.getName());
            final ValueWrapper valueWrapper = cache
                    .get(CacheKeys.entitySpecificCacheKey(entityId.toString(), cacheKey));
            if (valueWrapper != null && valueWrapper.get() != null) {
                FieldUtils.writeField(field, target, valueWrapper.get(), true);
            }
        };

        findCacheFields((Identifiable<?>) target, cacheFieldCallback);
    }

    /**
     * deletes the associated cache fields from the cache when deleted the
     * entity from the database.
     * 
     * @param target
     *            the entity which has been deleted
     */
    @PostRemove
    public void postDelete(final Object target) {
        if (!isIdentifiable(target)) {
            return;
        }
        final CacheFieldCallback cacheFieldCallback = (type, field, cacheKey, entityId) -> {
            final CacheManager cacheManager = CacheManagerHolder.getInstance().getCacheManager();
            final Cache cache = cacheManager.getCache(type.getName());
            cache.evict(CacheKeys.entitySpecificCacheKey(entityId.toString(), cacheKey));
        };

        findCacheFields((Identifiable<?>) target, cacheFieldCallback);
    }

    private boolean isIdentifiable(final Object target) {
        return target instanceof Identifiable;
    }

    private void findCacheFields(final Identifiable<?> target, final CacheFieldCallback callback) {
        final String id = target.getId().toString();
        final Class<?> type = target.getClass();

        final Field[] declaredFields = type.getDeclaredFields();
        for (final Field field : declaredFields) {
            if (field.getAnnotation(CacheField.class) != null) {
                try {
                    final CacheField annotation = field.getAnnotation(CacheField.class);
                    callback.fromCache(type, field, annotation.key(), id);
                } catch (final IllegalAccessException e) {
                    LOGGER.error("cannot access the field {} for the entity {}, ignoring the cacheable field", field,
                            type, e);
                }
            }
        }
    }

    @FunctionalInterface
    private interface CacheFieldCallback {
        /**
         * callback methods which is called when a field is annotated with
         * {@link CacheField}.
         * 
         * @param type
         *            the type of the target.
         * @param field
         *            the field which is annotaed with {@link CacheField}
         * @param cacheKey
         *            the configured cache key in the annotation
         *            {@link CacheField#key()}
         * @param entityId
         *            the ID of the entity
         * @throws IllegalAccessException
         *             in case the field cannot be accessed
         */
        void fromCache(Class<?> type, final Field field, final String cacheKey, Serializable entityId)
                throws IllegalAccessException;
    }

}
