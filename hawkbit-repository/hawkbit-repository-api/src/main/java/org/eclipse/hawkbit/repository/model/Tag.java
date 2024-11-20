/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.model;

/**
 * {@link Tag} entry.
 */
public interface Tag extends NamedEntity {

    /**
     * Maximum length of colour in Management UI.
     */
    int COLOUR_MAX_SIZE = 16;

    /**
     * @return colour code of the tag used in Management UI.
     */
    String getColour();

}
