/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.acm;

import org.eclipse.hawkbit.repository.acm.context.ContextRunner;
import org.eclipse.hawkbit.repository.jpa.acm.context.DefaultContextRunner;
import org.eclipse.hawkbit.repository.jpa.acm.controller.ArtifactAccessController;
import org.eclipse.hawkbit.repository.jpa.acm.controller.DefaultAccessController;
import org.eclipse.hawkbit.repository.jpa.acm.controller.DistributionSetAccessController;
import org.eclipse.hawkbit.repository.jpa.acm.controller.DistributionSetTypeAccessController;
import org.eclipse.hawkbit.repository.jpa.acm.controller.SoftwareModuleAccessController;
import org.eclipse.hawkbit.repository.jpa.acm.controller.SoftwareModuleTypeAccessController;
import org.eclipse.hawkbit.repository.jpa.acm.controller.TargetAccessController;
import org.eclipse.hawkbit.repository.jpa.acm.controller.TargetTypeAccessController;
import org.eclipse.hawkbit.repository.jpa.utils.DeploymentHelper;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.tenancy.TenantAware;

import java.util.Objects;
import java.util.function.Consumer;

public class AccessControlService {

    private final TenantAware tenantAware;
    private final ContextRunner contextRunner;
    private final TargetAccessController targetAccessController;
    private final TargetTypeAccessController targetTypeAccessController;
    private final DistributionSetAccessController distributionSetAccessController;
    private final DistributionSetTypeAccessController distributionSetTypeAccessController;
    private final SoftwareModuleAccessController softwareModuleAccessController;
    private final SoftwareModuleTypeAccessController softwareModuleTypeAccessController;
    private final ArtifactAccessController artifactAccessController;

    public AccessControlService(final TenantAware tenantAware, final ContextRunner contextRunner,
            final TargetAccessController targetAccessController,
            final TargetTypeAccessController targetTypeAccessController,
            final DistributionSetAccessController distributionSetAccessController,
            final DistributionSetTypeAccessController distributionSetTypeAccessController,
            final SoftwareModuleAccessController softwareModuleAccessController,
            final SoftwareModuleTypeAccessController softwareModuleTypeAccessController,
            final ArtifactAccessController artifactAccessController) {
        this.tenantAware = tenantAware;
        this.contextRunner = contextRunner;
        this.targetAccessController = targetAccessController;
        this.targetTypeAccessController = targetTypeAccessController;
        this.distributionSetAccessController = distributionSetAccessController;
        this.distributionSetTypeAccessController = distributionSetTypeAccessController;
        this.softwareModuleAccessController = softwareModuleAccessController;
        this.softwareModuleTypeAccessController = softwareModuleTypeAccessController;
        this.artifactAccessController = artifactAccessController;
    }

    public static AccessControlService withDefaultOptions(final TenantAware tenantAware) {
        final DefaultContextRunner defaultContextRunner = new DefaultContextRunner();
        final TargetAccessController targetAccessController = DefaultAccessController.targetAccessController();
        final TargetTypeAccessController targetTypeAccessController = DefaultAccessController
                .targetTypeAccessController();
        final DistributionSetAccessController distributionSetAccessController = DefaultAccessController
                .distributionSetAccessController();
        final DistributionSetTypeAccessController distributionSetTypeAccessController = DefaultAccessController
                .distributionSetTypeAccessController();
        final SoftwareModuleAccessController softwareModuleAccessController = DefaultAccessController
                .softwareModuleAccessController();
        final SoftwareModuleTypeAccessController softwareModuleTypeAccessController = DefaultAccessController
                .softwareModuleTypeAccessController();
        final ArtifactAccessController artifactAccessController = DefaultAccessController.artifactAccessController();
        return new AccessControlService(tenantAware, defaultContextRunner, targetAccessController,
                targetTypeAccessController, distributionSetAccessController, distributionSetTypeAccessController,
                softwareModuleAccessController, softwareModuleTypeAccessController, artifactAccessController);

    }

    public ContextRunner getContextRunner() {
        return contextRunner;
    }

    public TargetAccessController getTargetAccessController() {
        return targetAccessController;
    }

    public TargetTypeAccessController getTargetTypeAccessController() {
        return targetTypeAccessController;
    }

    public DistributionSetAccessController getDistributionSetAccessController() {
        return distributionSetAccessController;
    }

    public DistributionSetTypeAccessController getDistributionSetTypeAccessController() {
        return distributionSetTypeAccessController;
    }

    public SoftwareModuleAccessController getSoftwareModuleAccessController() {
        return softwareModuleAccessController;
    }

    public SoftwareModuleTypeAccessController getSoftwareModuleTypeAccessController() {
        return softwareModuleTypeAccessController;
    }

    public ArtifactAccessController getArtifactAccessController() {
        return artifactAccessController;
    }

    public void runningAutoAssignContext(final TargetFilterQuery targetFilterQuery, final String initiator,
            final Consumer<TargetFilterQuery> targetFilterQueryConsumer) {
        runInUserContext(initiator, () -> {
            targetFilterQuery.getAccessControlContext().ifPresentOrElse(context -> {
                contextRunner.runInContext(context, () -> {
                    targetFilterQueryConsumer.accept(targetFilterQuery);
                });
            }, () -> targetFilterQueryConsumer.accept(targetFilterQuery));
        });
    }

    public void runningRolloutContext(final Rollout rollout, final Consumer<Rollout> rolloutConsumer) {
        runInUserContext(rollout.getCreatedBy(), () -> {
            rollout.getAccessControlContext().ifPresentOrElse(context -> {
                contextRunner.runInContext(context, () -> {
                    rolloutConsumer.accept(rollout);
                });
            }, () -> rolloutConsumer.accept(rollout));
        });
    }

    private void runInUserContext(final String user, final Runnable handler) {
        DeploymentHelper.runInNonSystemContext(handler, () -> Objects.requireNonNull(user), tenantAware);
    }

}
