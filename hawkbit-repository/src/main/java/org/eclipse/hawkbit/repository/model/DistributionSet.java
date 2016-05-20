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

public interface DistributionSet extends NamedVersionedEntity {

    Set<DistributionSetTag> getTags();

    boolean isDeleted();

    /**
     * @return immutable list of meta data elements.
     */
    List<DistributionSetMetadata> getMetadata();

    List<Action> getActions();

    boolean isRequiredMigrationStep();

    DistributionSet setDeleted(boolean deleted);

    DistributionSet setRequiredMigrationStep(boolean isRequiredMigrationStep);

    DistributionSet setTags(Set<DistributionSetTag> tags);

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

    DistributionSetIdName getDistributionSetIdName();

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

    DistributionSetType getType();

    void setType(DistributionSetType type);

    boolean isComplete();

}