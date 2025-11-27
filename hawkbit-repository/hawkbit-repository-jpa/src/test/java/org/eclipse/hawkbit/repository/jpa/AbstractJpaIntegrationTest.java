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

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity_;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.repository.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.repository.ActionStatusRepository;
import org.eclipse.hawkbit.repository.jpa.repository.ArtifactRepository;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetTagRepository;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetTypeRepository;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutRepository;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutTargetGroupRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleTypeRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetTagRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetTypeRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TenantMetaDataRepository;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionStatusCreate;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.test.TestConfiguration;
import org.eclipse.hawkbit.repository.test.util.AbstractIntegrationTest;
import org.eclipse.hawkbit.repository.test.util.RolloutTestApprovalStrategy;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@ContextConfiguration(classes = { JpaRepositoryConfiguration.class, TestConfiguration.class })
@TestPropertySource(locations = "classpath:/jpa-test.properties")
@SuppressWarnings("java:S6813") // constructor injects are not possible for test classes
public abstract class AbstractJpaIntegrationTest extends AbstractIntegrationTest {

    protected static final String INVALID_TEXT_HTML = "</noscript><br><script>";
    protected static final String NOT_EXIST_ID = "12345678990";
    protected static final long NOT_EXIST_IDL = Long.parseLong(NOT_EXIST_ID);

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
    protected ActionStatusRepository actionStatusRepository;
    @Autowired
    protected ArtifactRepository artifactRepository;
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

    protected static void verifyThrownExceptionBy(final ThrowingCallable tc, final String objectType) {
        Assertions.assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(tc)
                .withMessageContaining(NOT_EXIST_ID).withMessageContaining(objectType);
    }

    protected static <T> List<T> toList(final Iterable<? extends T> it) {
        return StreamSupport.stream(it.spliterator(), false).map(e -> (T) e).toList();
    }

    protected static <T> T[] toArray(final Iterable<? extends T> it, final Class<T> type) {
        final List<T> list = toList(it);
        final T[] array = (T[]) Array.newInstance(type, list.size());
        for (int i = 0; i < array.length; i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    // just increase the opt lock revision if the instance in order to match it against locked db instance - not really locking
    protected static void implicitLock(final DistributionSet set) {
        ((JpaDistributionSet) set).setOptLockRevision(set.getOptLockRevision() + 1);
    }

    // just increase the opt lock revision if the instance in order to match it against locked db instance - not really locking
    protected static void implicitLock(final SoftwareModule module) {
        ((JpaSoftwareModule) module).setOptLockRevision(module.getOptLockRevision() + 1);
    }

    protected Database getDatabase() {
        return jpaProperties.getDatabase();
    }

    @Transactional(readOnly = true)
    protected List<Action> findActionsByRolloutAndStatus(final Rollout rollout, final Action.Status actionStatus) {
        return toList(actionRepository.findByRolloutIdAndStatus(PAGE, rollout.getId(), actionStatus));
    }

    protected List<Target> assignTag(final Collection<Target> targets, final TargetTag tag) {
        return targetManagement.assignTag(
                targets.stream().map(Target::getControllerId).toList(), tag.getId());
    }

    protected List<Target> unassignTag(final Collection<Target> targets, final TargetTag tag) {
        return targetManagement.unassignTag(
                targets.stream().map(Target::getControllerId).toList(), tag.getId());
    }

    protected List<? extends DistributionSet> assignTag(final Collection<? extends DistributionSet> sets, final DistributionSetTag tag) {
        return distributionSetManagement.assignTag(sets.stream().map(DistributionSet::getId).toList(), tag.getId());
    }

    protected List<? extends DistributionSet> assignTags(final Collection<? extends DistributionSet> sets, final DistributionSetTag... tags) {
        List<? extends DistributionSet> result = null;
        for (DistributionSetTag tag : tags) {
            result = assignTag(sets, tag);
        }
        return result;
    }

    protected List<? extends DistributionSet> unassignTag(
            final Collection<DistributionSet> sets, final DistributionSetTag tag) {
        return distributionSetManagement.unassignTag(sets.stream().map(DistributionSet::getId).toList(), tag.getId());
    }

    protected void initiateTypeAssignment(final Collection<Target> targets, final TargetType type) {
        targets.stream().map(Target::getControllerId).forEach(id -> targetManagement.assignType(id, type.getId()));
    }

    protected void assertRollout(
            final Rollout rollout, final boolean dynamic, final Rollout.RolloutStatus status, final int groupCreated, final long totalTargets) {
        final Rollout refreshed = refresh(rollout);
        assertThat(refreshed.isDynamic()).as("Is dynamic").isEqualTo(dynamic);
        assertThat(refreshed.getStatus()).as("Status").isEqualTo(status);
        assertThat(refreshed.getRolloutGroupsCreated()).as("Groups created").isEqualTo(groupCreated);
        assertThat(refreshed.getTotalTargets()).as("Total targets").isEqualTo(totalTargets);
    }

    protected void assertGroup(final RolloutGroup group, final boolean dynamic, final RolloutGroup.RolloutGroupStatus status,
            final long totalTargets) {
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
        controllerManagement.addUpdateActionStatus(
                ActionStatusCreate.builder().actionId(action.getId()).status(Action.Status.FINISHED).build());
    }

    protected Set<TargetTag> getTargetTags(final String controllerId) {
        return targetManagement.getTags(controllerId);
    }

    protected JpaRolloutGroup refresh(final RolloutGroup group) {
        return rolloutGroupRepository.findById(group.getId()).get();
    }

    protected static Specification<JpaAction> byDistributionSetId(final Long distributionSetId) {
        return (root, query, cb) -> cb.equal(root.get(JpaAction_.distributionSet).get(AbstractJpaBaseEntity_.id), distributionSetId);
    }

    private JpaRollout refresh(final Rollout rollout) {
        return rolloutRepository.findById(rollout.getId()).get();
    }
}