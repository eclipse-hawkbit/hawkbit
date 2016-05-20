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

public interface DistributionSetType extends NamedEntity {

    /**
     * @return the deleted
     */
    boolean isDeleted();

    /**
     * @param deleted
     *            the deleted to set
     */
    void setDeleted(boolean deleted);

    Set<SoftwareModuleType> getMandatoryModuleTypes();

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
     * @param softwareModuleType
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

    String getKey();

    void setKey(String key);

    /**
     * @param distributionSet
     *            to check for completeness
     * @return <code>true</code> if the all mandatory software module types are
     *         in the system.
     */
    boolean checkComplete(DistributionSet distributionSet);

    String getColour();

    void setColour(final String colour);

}