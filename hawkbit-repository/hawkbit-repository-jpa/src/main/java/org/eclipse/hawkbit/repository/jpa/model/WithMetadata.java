/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.util.Map;

/**
 * Interface for entities that support metadata.
 * @param <MV> metadata value type
 * @param <MVI> metadata value implementation type
 */
@SuppressWarnings("java:S119") // java:S119 - better self explainable
public interface WithMetadata<MV, MVI extends MV> {

    Map<String, MVI> getMetadata();

    // return if the entity is valid for update metadata
    default boolean isValid() {
        return true;
    }
}