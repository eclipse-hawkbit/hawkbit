/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
