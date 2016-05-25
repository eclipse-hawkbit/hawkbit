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
     * @return business key of this {@link SoftwareModuleType}.
     */
    String getKey();

    /**
     * @param key
     *            of this {@link SoftwareModuleType}.
     */
    void setKey(String key);

    /**
     * @return maximum assignments of an {@link SoftwareModule} of this type to
     *         a {@link DistributionSet}.
     */
    int getMaxAssignments();

    /**
     * @param maxAssignments
     *            of an {@link SoftwareModule} of this type to a
     *            {@link DistributionSet}.
     */
    void setMaxAssignments(int maxAssignments);

    /**
     * @return <code>true</code> if the type is deleted and only kept for
     *         history purposes.
     */
    boolean isDeleted();

    /**
     * @return get color code to by used in management UI views.
     */
    String getColour();

    /**
     * @param colour
     *            code to by used in management UI views.
     */
    void setColour(final String colour);

}