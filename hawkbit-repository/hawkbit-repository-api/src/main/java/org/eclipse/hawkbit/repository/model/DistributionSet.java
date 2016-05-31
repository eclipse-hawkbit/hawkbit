/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.util.List;
import java.util.Set;

import org.eclipse.hawkbit.repository.DistributionSetManagement;

/**
 * A {@link DistributionSet} defines a meta package that combines a set of
 * {@link SoftwareModule}s which have to be or are provisioned to a
 * {@link Target}.
 * 
 * <p>
 * A {@link Target} has exactly one target {@link DistributionSet} assigned.
 * </p>
 *
 */
public interface DistributionSet extends NamedVersionedEntity {

    /**
     * @return {@link Set} of assigned {@link DistributionSetTag}s.
     */
    Set<DistributionSetTag> getTags();

    /**
     * @return <code>true</code> if the set is deleted and only kept for history
     *         purposes.
     */
    boolean isDeleted();

    /**
     * @return immutable {@link List} of {@link DistributionSetMetadata}
     *         elements. See {@link DistributionSetManagement} to alter.
     */
    List<DistributionSetMetadata> getMetadata();

    /**
     * @return <code>true</code> if {@link DistributionSet} contains a mandatory
     *         migration step, i.e. unfinished {@link Action}s will kept active
     *         and not automatically canceled if overridden by a newer update.
     */
    boolean isRequiredMigrationStep();

    /**
     * @param deleted
     *            to <code>true</code> if {@link DistributionSet} is no longer
     *            be usage but kept for history purposes.
     * @return updated {@link DistributionSet}
     */
    DistributionSet setDeleted(boolean deleted);

    /**
     * @param isRequiredMigrationStep
     *            to <code>true</code> if {@link DistributionSet} contains a
     *            mandatory migration step, i.e. unfinished {@link Action}s will
     *            kept active and not automatically canceled if overridden by a
     *            newer update.
     * 
     * @return updated {@link DistributionSet}
     */
    DistributionSet setRequiredMigrationStep(boolean isRequiredMigrationStep);

    /**
     * @return the assignedTargets
     */
    List<Target> getAssignedTargets();

    /**
     * @return the installedTargets
     */
    List<TargetInfo> getInstalledTargets();

    /**
     *
     * @return unmodifiableSet of {@link SoftwareModule}.
     */
    Set<SoftwareModule> getModules();

    /**
     * @param softwareModule
     * @return <code>true</code> if the module was added and <code>false</code>
     *         if it already existed in the set
     *
     */
    boolean addModule(SoftwareModule softwareModule);

    /**
     * Removed given {@link SoftwareModule} from this DS instance.
     *
     * @param softwareModule
     *            to remove
     * @return <code>true</code> if element was found and removed
     */
    boolean removeModule(SoftwareModule softwareModule);

    /**
     * Searches through modules for the given type.
     *
     * @param type
     *            to search for
     * @return SoftwareModule of given type or <code>null</code> if not in the
     *         list.
     */
    SoftwareModule findFirstModuleByType(SoftwareModuleType type);

    /**
     * @return type of the {@link DistributionSet}.
     */
    DistributionSetType getType();

    /**
     * @param type
     *            of the {@link DistributionSet}.
     */
    void setType(DistributionSetType type);

    /**
     * @return <code>true</code> if all defined
     *         {@link DistributionSetType#getMandatoryModuleTypes()} of
     *         {@link #getType()} are present in this {@link DistributionSet}.
     */
    boolean isComplete();

}
