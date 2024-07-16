/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.repository.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.repository.ActionStatusRepository;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetTagRepository;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetTypeRepository;
import org.eclipse.hawkbit.repository.jpa.repository.LocalArtifactRepository;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutRepository;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutTargetGroupRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleMetadataRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleTypeRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetTagRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetTypeRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TenantMetaDataRepository;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetTagAssignmentResult;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetTagAssignmentResult;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TargetTypeAssignmentResult;
import org.eclipse.hawkbit.repository.test.TestConfiguration;
import org.eclipse.hawkbit.repository.test.util.AbstractIntegrationTest;
import org.eclipse.hawkbit.repository.test.util.RolloutTestApprovalStrategy;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = {
        RepositoryApplicationConfiguration.class, TestConfiguration.class })
@Import(TestChannelBinderConfiguration.class)
@TestPropertySource(locations = "classpath:/jpa-test.properties")
public abstract class AbstractJpaIntegrationTest extends AbstractIntegrationTest {

    protected static final String INVALID_TEXT_HTML = "</noscript><br><script>";
    protected static final String NOT_EXIST_ID = "12345678990";
    protected static final long NOT_EXIST_IDL = Long.parseLong(NOT_EXIST_ID);

    @PersistenceContext
    protected EntityManager entityManager;

    @Autowired
    protected TargetRepository targetRepository;

    @Autowired
    protected ActionRepository actionRepository;

    @Autowired
    protected DistributionSetRepository distributionSetRepository;

    @Autowired
    protected SoftwareModuleRepository softwareModuleRepository;

    @Autowired
    protected TenantMetaDataRepository tenantMetaDataRepository;

    @Autowired
    protected DistributionSetTypeRepository distributionSetTypeRepository;

    @Autowired
    protected SoftwareModuleTypeRepository softwareModuleTypeRepository;

    @Autowired
    protected TargetTagRepository targetTagRepository;

    @Autowired
    protected TargetTypeRepository targetTypeRepository;

    @Autowired
    protected DistributionSetTagRepository distributionSetTagRepository;

    @Autowired
    protected SoftwareModuleMetadataRepository softwareModuleMetadataRepository;

    @Autowired
    protected ActionStatusRepository actionStatusRepository;

    @Autowired
    protected LocalArtifactRepository artifactRepository;

    @Autowired
    protected RolloutGroupRepository rolloutGroupRepository;

    @Autowired
    protected RolloutTargetGroupRepository rolloutTargetGroupRepository;

    @Autowired
    protected RolloutRepository rolloutRepository;

    @Autowired
    protected TenantConfigurationProperties tenantConfigurationProperties;

    @Autowired
    protected RolloutTestApprovalStrategy approvalStrategy;

    @Autowired
    private JpaProperties jpaProperties;

    protected Database getDatabase() {
        return jpaProperties.getDatabase();
    }

    @Transactional(readOnly = true)
    protected List<Action> findActionsByRolloutAndStatus(final Rollout rollout, final Action.Status actionStatus) {
        return toList(actionRepository.findByRolloutIdAndStatus(PAGE, rollout.getId(), actionStatus));
    }

    protected static void verifyThrownExceptionBy(final ThrowingCallable tc, final String objectType) {
        Assertions.assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(tc)
                .withMessageContaining(NOT_EXIST_ID).withMessageContaining(objectType);
    }

    protected TargetTagAssignmentResult toggleTagAssignment(final Collection<Target> targets, final TargetTag tag) {
        return targetManagement.toggleTagAssignment(
                targets.stream().map(Target::getControllerId).collect(Collectors.toList()), tag.getName());
    }

    public DistributionSetTagAssignmentResult toggleTagAssignment(final Collection<DistributionSet> sets,
            final DistributionSetTag tag) {
        return distributionSetManagement.toggleTagAssignment(
                sets.stream().map(DistributionSet::getId).collect(Collectors.toList()), tag.getName());
    }

    protected TargetTypeAssignmentResult initiateTypeAssignment(final Collection<Target> targets, final TargetType type) {
        return targetManagement.assignType(
                targets.stream().map(Target::getControllerId).collect(Collectors.toList()), type.getId());
    }

    protected void assertRollout(final Rollout rollout, final boolean dynamic, final Rollout.RolloutStatus status, final int groupCreated, final long totalTargets) {
        final Rollout refreshed = refresh(rollout);
        assertThat(refreshed.isDynamic()).as("Is dynamic").isEqualTo(dynamic);
        assertThat(refreshed.getStatus()).as("Status").isEqualTo(status);
        assertThat(refreshed.getRolloutGroupsCreated()).as("Groups created").isEqualTo(groupCreated);
        assertThat(refreshed.getTotalTargets()).as("Total targets").isEqualTo(totalTargets);
    }

    protected void assertGroup(final RolloutGroup group, final boolean dynamic, final RolloutGroup.RolloutGroupStatus status, final long totalTargets) {
        final RolloutGroup refreshed = refresh(group);
        assertThat(refreshed.isDynamic()).as("Is dynamic").isEqualTo(dynamic);
        assertThat(refreshed.getStatus()).as("Status").isEqualTo(status);
        assertThat(refreshed.getTotalTargets()).as("Total targets").isEqualTo(totalTargets);
    }

    protected Page<JpaAction> assertAndGetRunning(final Rollout rollout, final int count) {
        final Page<JpaAction> running = actionRepository.findByRolloutIdAndStatus(PAGE, rollout.getId(), Action.Status.RUNNING);
        assertThat(running.getTotalElements()).as("Action count").isEqualTo(count);
        return running;
    }

    protected void assertScheduled(final Rollout rollout, final int count) {
        final Page<JpaAction> running = actionRepository.findByRolloutIdAndStatus(PAGE, rollout.getId(), Action.Status.SCHEDULED);
        assertThat(running.getTotalElements()).as("Action count").isEqualTo(count);
    }

    protected void finishAction(final Action action) {
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(action.getId()).status(Action.Status.FINISHED));
    }

    protected Set<TargetTag> getTargetTags(final String controllerId) {
        return targetRepository
                .findOne(TargetSpecifications.hasControllerId(controllerId))
                .map(JpaTarget.class::cast)
                .map(JpaTarget::getTags)
                .orElseThrow(() -> new EntityNotFoundException(Target.class, controllerId));
    }

    private JpaRollout refresh(final Rollout rollout) {
        return rolloutRepository.findById(rollout.getId()).get();
    }

    protected JpaRolloutGroup refresh(final RolloutGroup group) {
        return rolloutGroupRepository.findById(group.getId()).get();
    }

    protected static <T> List<T> toList(final Iterable<? extends T> it) {
        return StreamSupport.stream(it.spliterator(), false).map(e -> (T)e).toList();
    }

    protected static <T> T[] toArray(final Iterable<? extends T> it, final Class<T> type) {
        final List<T> list = toList(it);
        final T[] array = (T[])Array.newInstance(type, list.size());
        for (int i = 0; i < array.length; i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    protected static void implicitLock(final DistributionSet set) {
        ((JpaDistributionSet) set).setOptLockRevision(set.getOptLockRevision() + 1);
    }

    protected static void implicitLock(final SoftwareModule module) {
        ((JpaSoftwareModule) module).setOptLockRevision(module.getOptLockRevision() + 1);
    }
}
