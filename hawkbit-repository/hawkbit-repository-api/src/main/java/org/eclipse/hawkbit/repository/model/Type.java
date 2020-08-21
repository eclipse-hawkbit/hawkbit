/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

/**
 * {@link Type} is an abstract definition for {@link DistributionSetType}s and
 * {@link SoftwareModuleType}s
 */
public interface Type extends NamedEntity {
    /**
     * Maximum length of key.
     */
    int KEY_MAX_SIZE = 64;

    /**
     * Maximum length of color in Management UI.
     */
    int COLOUR_MAX_SIZE = 16;

    /**
     * @return business key.
     */
    String getKey();

    /**
     * @return <code>true</code> if the type is deleted and only kept for
     *         history purposes.
     */
    boolean isDeleted();

    /**
     * @return get color code to be used in management UI views.
     */
    String getColour();

}
