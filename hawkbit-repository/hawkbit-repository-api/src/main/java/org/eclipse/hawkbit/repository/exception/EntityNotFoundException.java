/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.exception;

import java.util.Collection;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.MetaData;

/**
 * the {@link EntityNotFoundException} is thrown when a entity queried but not
 * found.
 */
public class EntityNotFoundException extends AbstractServerRtException {

    private static final long serialVersionUID = 1L;
    private static final SpServerError THIS_ERROR = SpServerError.SP_REPO_ENTITY_NOT_EXISTS;

    /**
     * Default constructor.
     */
    public EntityNotFoundException() {
        super(THIS_ERROR);
    }

    /**
     * Parameterized constructor.
     * 
     * @param cause
     *            of the exception
     */
    public EntityNotFoundException(final Throwable cause) {
        super(THIS_ERROR, cause);
    }

    /**
     * Parameterized constructor.
     * 
     * @param message
     *            of the exception
     * @param cause
     *            of the exception
     */
    public EntityNotFoundException(final String message, final Throwable cause) {
        super(message, THIS_ERROR, cause);
    }

    /**
     * Parameterized constructor.
     * 
     * @param message
     *            of the exception
     */
    protected EntityNotFoundException(final String message) {
        super(message, THIS_ERROR);
    }

    /**
     * Parameterized constructor for {@link BaseEntity} not found.
     * 
     * @param type
     *            of the entity that was not found
     * 
     * @param enityId
     *            of the {@link BaseEntity}
     */
    public EntityNotFoundException(final Class<? extends BaseEntity> type, final Object enityId) {
        this(type.getSimpleName() + " with given identifier {" + enityId + "} does not exist.");
    }

    /**
     * Parameterized constructor for {@link MetaData} not found.
     * 
     * @param type
     *            of the entity that was not found
     * @param enityId
     *            of the {@link BaseEntity} the {@link MetaData} was for
     * @param key
     *            for the {@link MetaData} entry
     */
    public EntityNotFoundException(final Class<? extends MetaData> type, final Long enityId, final String key) {
        this(type.getSimpleName() + " for given entity {" + enityId + "} and with key {" + key + "} does not exist.");
    }

    /**
     * Parameterized constructor for a list of {@link BaseEntity}s not found.
     * 
     * @param type
     *            of the entity that was not found
     * 
     * @param expected
     *            collection of the {@link BaseEntity#getId()}s
     * @param found
     *            collection of the {@link BaseEntity#getId()}s
     */
    public EntityNotFoundException(final Class<? extends BaseEntity> type, final Collection<?> expected,
            final Collection<?> found) {
        this(type.getSimpleName() + "s with given identifiers {" + expected.stream().filter(id -> !found.contains(id))
                .map(String::valueOf).collect(Collectors.joining(",")) + "} do not exist.");
    }

}
