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
 */
public interface DistributionSet extends NamedVersionedEntity {

    /**
     * @return type of the {@link DistributionSet}.
     */
    DistributionSetType getType();

    /**
     * @return unmodifiableSet of {@link SoftwareModule}.
     */
    Set<SoftwareModule> getModules();

    /**
     * @return <code>true</code> if all defined
     *         {@link DistributionSetType#getMandatoryModuleTypes()} of
     *         {@link #getType()} are present in this {@link DistributionSet}.
     */
    boolean isComplete();

    /**
     * @return <code>true</code> if this {@link DistributionSet} is locked. If so it's 'functional'
     *         properties (e.g. software modules) could not be modified anymore.
     */
    boolean isLocked();

    /**
     * @return <code>true</code> if this {@link DistributionSet} is deleted and only kept for history
     *         purposes.
     */
    boolean isDeleted();

    /**
     * @return <code>false</code> if this {@link DistributionSet} is
     *         invalidated.
     */
    boolean isValid();

    /**
     * @return <code>true</code> if {@link DistributionSet} contains a mandatory
     *         migration step, i.e. unfinished {@link Action}s will kept active
     *         and not automatically canceled if overridden by a newer update.
     */
    boolean isRequiredMigrationStep();

    /**
     * Searches through modules for the given type.
     *
     * @param type to search for
     * @return SoftwareModule of given type
     */
    default Optional<SoftwareModule> findFirstModuleByType(final SoftwareModuleType type) {
        return getModules().stream().filter(module -> module.getType().equals(type)).findAny();
    }
}