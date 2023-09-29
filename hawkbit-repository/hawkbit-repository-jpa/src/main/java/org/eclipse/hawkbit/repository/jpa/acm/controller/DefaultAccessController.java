/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.acm.controller;

import java.util.List;

import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifact;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.springframework.data.jpa.domain.Specification;

/**
 * Default implementation of the {@link AccessController} permitting every kind
 * of request.
 * 
 * @param <T>
 *            the entity type to manage
 */
public class DefaultAccessController<T> implements AccessController<T> {

    /**
     * The default specification without limitation.
     * 
     * @return a new instance of {@link Specification} without limitations.
     */
    @Override
    public Specification<T> getAccessRules(final Operation operation) {
        return Specification.where(null);
    }

    /**
     * Nothing to append.
     * 
     * @param specification
     *            is the root specification which needs to be appended by the
     *            resource limitation
     * @return the unmodified specification
     */
    @Override
    public Specification<T> appendAccessRules(final Operation operation, final Specification<T> specification) {
        return specification;
    }

    @Override
    public void assertOperationAllowed(final Operation operation, final List<T> entities)
            throws InsufficientPermissionException {
        // Every request is allowed
    }

    public static TargetAccessController targetAccessController() {
        return new DefaultTargetAccessController();
    }

    public static TargetTypeAccessController targetTypeAccessController() {
        return new DefaultTargetTypeAccessController();
    }

    public static DistributionSetAccessController distributionSetAccessController() {
        return new DefaultDistributionSetAccessController();
    }

    public static DistributionSetTypeAccessController distributionSetTypeAccessController() {
        return new DefaultDistributionSetTypeAccessController();
    }

    public static SoftwareModuleAccessController softwareModuleAccessController() {
        return new DefaultSoftwareModuleAccessController();
    }

    public static SoftwareModuleTypeAccessController softwareModuleTypeAccessController() {
        return new DefaultSoftwareModuleTypeAccessController();
    }

    public static ArtifactAccessController artifactAccessController() {
        return new DefaultArtifactAccessController();
    }

    /**
     * Default implementation of the {@link TargetAccessController}
     */
    private static class DefaultTargetAccessController extends DefaultAccessController<JpaTarget>
            implements TargetAccessController {
    }

    /**
     * Default implementation of the {@link TargetTypeAccessController}
     */
    private static class DefaultTargetTypeAccessController extends DefaultAccessController<JpaTargetType>
            implements TargetTypeAccessController {
    }

    /**
     * Default implementation of the {@link DistributionSetAccessController}
     */
    private static class DefaultDistributionSetAccessController extends DefaultAccessController<JpaDistributionSet>
            implements DistributionSetAccessController {
    }

    /**
     * Default implementation of the {@link DistributionSetTypeAccessController}
     */
    private static class DefaultDistributionSetTypeAccessController
            extends DefaultAccessController<JpaDistributionSetType> implements DistributionSetTypeAccessController {
    }

    /**
     * Default implementation of the {@link SoftwareModuleAccessController}
     */
    private static class DefaultSoftwareModuleAccessController extends DefaultAccessController<JpaSoftwareModule>
            implements SoftwareModuleAccessController {
    }

    /**
     * Default implementation of the {@link SoftwareModuleTypeAccessController}
     */
    private static class DefaultSoftwareModuleTypeAccessController
            extends DefaultAccessController<JpaSoftwareModuleType> implements SoftwareModuleTypeAccessController {
    }

    /**
     * Default implementation of the {@link ArtifactAccessController}
     */
    private static class DefaultArtifactAccessController extends DefaultAccessController<JpaArtifact>
            implements ArtifactAccessController {
    }
}
