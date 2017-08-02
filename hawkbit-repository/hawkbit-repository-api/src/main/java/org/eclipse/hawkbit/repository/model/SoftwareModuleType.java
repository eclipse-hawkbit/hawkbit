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
 * {@link SoftwareModuleType} is an abstract definition used in
 * {@link DistributionSetType}s and includes additional {@link SoftwareModule}
 * specific information.
 *
 */
public interface SoftwareModuleType extends NamedEntity {
    /**
     * Maximum length of key.
     */
    int KEY_MAX_SIZE = 64;

    /**
     * Maximum length of colour in Management UI.
     */
    int COLOUR_MAX_SIZE = 16;

    /**
     * @return business key of this {@link SoftwareModuleType}.
     */
    String getKey();

    /**
     * @return maximum assignments of an {@link SoftwareModule} of this type to
     *         a {@link DistributionSet}.
     */
    int getMaxAssignments();

    /**
     * @return <code>true</code> if the type is deleted and only kept for
     *         history purposes.
     */
    boolean isDeleted();

    /**
     * @return get color code to by used in management UI views.
     */
    String getColour();

}
