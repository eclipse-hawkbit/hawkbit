/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.exception;

import java.io.Serial;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.repository.model.BaseEntity;

/**
 * the {@link EntityNotFoundException} is thrown when a entity queried but not found.
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Getter
public class EntityNotFoundException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String KEY = "key";
    public static final String ENTITY_ID = "entityId";
    public static final String TYPE = "type";

    private static final SpServerError THIS_ERROR = SpServerError.SP_REPO_ENTITY_NOT_EXISTS;
    private static final int ENTITY_STRING_MAX_LENGTH = 100;

    public EntityNotFoundException() {
        super(THIS_ERROR);
    }

    public EntityNotFoundException(final Throwable cause) {
        super(THIS_ERROR, cause);
    }

    public EntityNotFoundException(final String message, final Throwable cause) {
        super(THIS_ERROR, message, cause);
    }

    protected EntityNotFoundException(final String message) {
        super(THIS_ERROR, message);
    }

    public EntityNotFoundException(final String type, final Object entityId) {
        super(THIS_ERROR, type + " with given identifier {" + entityId + "} does not exist.", Map.of(TYPE, type, ENTITY_ID, entityId));
    }

    public EntityNotFoundException(final Class<? extends BaseEntity> type, final Object entityId) {
        this(type.getSimpleName(), entityId);
    }

    public EntityNotFoundException(final Class<? extends BaseEntity> type, final Collection<?> expected, final Collection<?> found) {
        super(
                THIS_ERROR, String.format("%ss with given identifiers {%s} do not exist.",
                        type.getSimpleName(),
                        toEntityString(expected.stream()
                                .filter(id -> !found.contains(id))
                                .map(String::valueOf)
                                .collect(Collectors.joining(",")))),
                Map.of(
                        TYPE, type.getSimpleName(),
                        ENTITY_ID, expected.stream().filter(id -> !found.contains(id)).map(String::valueOf).toList()));
    }

    private static String toEntityString(final Object obj) {
        final String str = String.valueOf(obj);
        return str.length() > ENTITY_STRING_MAX_LENGTH ? str.substring(0, ENTITY_STRING_MAX_LENGTH - 3).concat("...") : str;
    }
}