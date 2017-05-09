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
import java.util.Optional;
import java.util.Set;

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
     * @return <code>true</code> if the set is deleted and only kept for history
     *         purposes.
     */
    boolean isDeleted();

    /**
     * @return <code>true</code> if {@link DistributionSet} contains a mandatory
     *         migration step, i.e. unfinished {@link Action}s will kept active
     *         and not automatically canceled if overridden by a newer update.
     */
    boolean isRequiredMigrationStep();

    /**
     * @return the auto assign target filters
     */
    List<TargetFilterQuery> getAutoAssignFilters();

    /**
     *
     * @return unmodifiableSet of {@link SoftwareModule}.
     */
    Set<SoftwareModule> getModules();

    /**
     * Searches through modules for the given type.
     *
     * @param type
     *            to search for
     * @return SoftwareModule of given type
     */
    default Optional<SoftwareModule> findFirstModuleByType(final SoftwareModuleType type) {
        return getModules().stream().filter(module -> module.getType().equals(type)).findAny();
    }

    /**
     * @return type of the {@link DistributionSet}.
     */
    DistributionSetType getType();

    /**
     * @return <code>true</code> if all defined
     *         {@link DistributionSetType#getMandatoryModuleTypes()} of
     *         {@link #getType()} are present in this {@link DistributionSet}.
     */
    boolean isComplete();

}
