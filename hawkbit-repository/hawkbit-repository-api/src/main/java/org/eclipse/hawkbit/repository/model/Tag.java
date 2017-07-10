/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

/**
 * {@link Tag} entry.
 *
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
