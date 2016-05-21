/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.util.Set;

import org.eclipse.hawkbit.repository.jpa.model.DistributionSetTypeElement;

/**
 * A {@link DistributionSetType} is an abstract definition for
 * {@link DistributionSet} that defines what {@link SoftwareModule}s can be
 * added (optional) to {@link DistributionSet} of that type or have to added
 * (mandatory) in order to be considered complete. Only complete DS can be
 * assigned to a {@link Target}.
 *
 */
public interface DistributionSetType extends NamedEntity {

    /**
     * @return <code>true</code> if the type is deleted and only kept for
     *         history purposes.
     */
    boolean isDeleted();

    /**
     * @return set of {@link SoftwareModuleType}s that need to be in a
     *         {@link DistributionSet} of this type to be
     *         {@link DistributionSet#isComplete()}.
     */
    Set<SoftwareModuleType> getMandatoryModuleTypes();

    /**
     * @return set of optional {@link SoftwareModuleType}s that can be in a
     *         {@link DistributionSet} of this type.
     */
    Set<SoftwareModuleType> getOptionalModuleTypes();

    /**
     * Checks if the given {@link SoftwareModuleType} is in this
     * {@link DistributionSetType}.
     *
     * @param softwareModuleType
     *            search for
     * @return <code>true</code> if found
     */
    boolean containsModuleType(SoftwareModuleType softwareModuleType);

    /**
     * Checks if the given {@link SoftwareModuleType} is in this
     * {@link DistributionSetType} and defined as
     * {@link DistributionSetTypeElement#isMandatory()}.
     *
     * @param softwareModuleType
     *            search for
     * @return <code>true</code> if found
     */
    boolean containsMandatoryModuleType(SoftwareModuleType softwareModuleType);

    /**
     * Checks if the given {@link SoftwareModuleType} is in this
     * {@link DistributionSetType} and defined as
     * {@link DistributionSetTypeElement#isMandatory()}.
     *
     * @param softwareModuleTypeId
     *            search for by {@link SoftwareModuleType#getId()}
     * @return <code>true</code> if found
     */
    boolean containsMandatoryModuleType(Long softwareModuleTypeId);

    /**
     * Checks if the given {@link SoftwareModuleType} is in this
     * {@link DistributionSetType} and NOT defined as
     * {@link DistributionSetTypeElement#isMandatory()}.
     *
     * @param softwareModuleType
     *            search for
     * @return <code>true</code> if found
     */
    boolean containsOptionalModuleType(SoftwareModuleType softwareModuleType);

    /**
     * Checks if the given {@link SoftwareModuleType} is in this
     * {@link DistributionSetType} and NOT defined as
     * {@link DistributionSetTypeElement#isMandatory()}.
     *
     * @param softwareModuleTypeId
     *            search by {@link SoftwareModuleType#getId()}
     * @return <code>true</code> if found
     */
    boolean containsOptionalModuleType(Long softwareModuleTypeId);

    /**
     * Compares the modules of this {@link DistributionSetType} and the given
     * one.
     *
     * @param dsType
     *            to compare with
     * @return <code>true</code> if the lists are identical.
     */
    boolean areModuleEntriesIdentical(DistributionSetType dsType);

    /**
     * Adds {@link SoftwareModuleType} that is optional for the
     * {@link DistributionSet}.
     *
     * @param smType
     *            to add
     * @return updated instance
     */
    DistributionSetType addOptionalModuleType(SoftwareModuleType smType);

    /**
     * Adds {@link SoftwareModuleType} that is mandatory for the
     * {@link DistributionSet}.
     *
     * @param smType
     *            to add
     * @return updated instance
     */
    DistributionSetType addMandatoryModuleType(SoftwareModuleType smType);

    /**
     * Removes {@link SoftwareModuleType} from the list.
     *
     * @param smTypeId
     *            to remove
     * @return updated instance
     */
    DistributionSetType removeModuleType(Long smTypeId);

    /**
     * @return business key of this {@link DistributionSetType}.
     */
    String getKey();

    /**
     * @param key
     *            of this {@link DistributionSetType}.
     */
    void setKey(String key);

    /**
     * @param distributionSet
     *            to check for completeness
     * @return <code>true</code> if the all mandatory software module types are
     *         in the system.
     */
    boolean checkComplete(DistributionSet distributionSet);

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