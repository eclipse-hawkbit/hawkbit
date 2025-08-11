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
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.runAs;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.withUser;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.StreamSupport;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity_;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.repository.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.repository.ActionStatusRepository;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetTagRepository;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetTypeRepository;
import org.eclipse.hawkbit.repository.jpa.repository.LocalArtifactRepository;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutRepository;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutTargetGroupRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleTypeRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetTagRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetTypeRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TenantMetaDataRepository;
import org.eclipse.hawkbit.repository.jpa.specifications.DistributionSetSpecification;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionStatusCreate;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

@Slf4j
@ContextConfiguration(classes = { JpaRepositoryConfiguration.class, TestConfiguration.class })
@TestPropertySource(locations = "classpath:/jpa-test.properties")
@SuppressWarnings("java:S6813") // constructor injects are not possible for test classes
public abstract class AbstractJpaIntegrationTest extends AbstractIntegrationTest {

    protected static final String INVALID_TEXT_HTML = "</noscript><br><script>";
    protected static final String NOT_EXIST_ID = "12345678990";
    protected static final long NOT_EXIST_IDL = Long.parseLong(NOT_EXIST_ID);

    private static final List<String> REPOSITORY_AND_TARGET_PERMISSIONS = List.of(SpPermission.READ_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY, SpPermission.DELETE_REPOSITORY, SpPermission.READ_TARGET, SpPermission.CREATE_TARGET,
            SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET);

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

    protected List<? extends DistributionSet> assignTag(final Collection<? extends DistributionSet> sets,
            final DistributionSetTag tag) {
        return distributionSetManagement.assignTag(sets.stream().map(DistributionSet::getId).toList(), tag.getId());
    }

    protected List<? extends DistributionSet> unassignTag(
            final Collection<DistributionSet> sets, final DistributionSetTag tag) {
        return distributionSetManagement.unassignTag(sets.stream().map(DistributionSet::getId).toList(), tag.getId());
    }

    protected void initiateTypeAssignment(final Collection<Target> targets, final TargetType type) {
        targets.stream().map(Target::getControllerId).forEach(id -> targetManagement.assignType(id, type.getId()));
    }

    protected void assertRollout(final Rollout rollout, final boolean dynamic, final Rollout.RolloutStatus status, final int groupCreated,
            final long totalTargets) {
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

    /**
     * Asserts that the given callable throws an InsufficientPermissionException.
     * If callable succeeds without any exception or exception other than InsufficientPermissionException, it will be considered as an assert failure.
     *
     * @param callable the callable to call
     */
    protected void assertPermissions(final Callable<?> callable, List<String> requiredPermissions) {
        assertPermissions(callable, requiredPermissions, null);
    }

    /**
     * Asserts that the given callable throws an InsufficientPermissionException.
     *
     * @param callable the callable to call
     * @param requiredPermissions required permissions for the callable
     * @param insufficientPermissions can be null, if null, it will be resolved automatically. But in some cases (e.g. @PreAuthorized Permissions with OR, it is safer to pass directly the insufficient permissions)
     */
    @SneakyThrows
    protected void assertPermissions(final Callable<?> callable, final List<String> requiredPermissions,
            final List<String> insufficientPermissions) {
        // if READ_PERMISSION is required and required permissions are multiple, give only READ_PERMISSION to eliminate internal read_permission check failure that would confuse the actual test
        final List<String> resolvedInsufficientPermissions = insufficientPermissions != null
                ? insufficientPermissions
                : requiredPermissions.contains(SpPermission.READ_REPOSITORY) && requiredPermissions.size() > 1
                        ? List.of(SpPermission.READ_REPOSITORY)
                        : REPOSITORY_AND_TARGET_PERMISSIONS.stream().filter(p -> !requiredPermissions.contains(p)).toList();
        // check if the user has the correct permissions
        runAs(withUser("user_with_permissions", requiredPermissions.toArray(new String[0])), () -> {
            assertPermissionWorks(callable);
            log.info("assertPermissionWorks Passed");
        });

        // check if the user has the insufficient permissions
        runAs(withUser("user_without_permissions", resolvedInsufficientPermissions.toArray(new String[0])), () -> {
            assertInsufficientPermission(callable);
            log.info("assertInsufficientPermission Passed");
        });
    }

    /**
     * Asserts that the given callable throws an InsufficientPermissionException.
     * If callable succeeds without any exception or exception other than InsufficientPermissionException, it will be considered as an assert failure.
     *
     * @param callable the callable to call
     */
    private void assertInsufficientPermission(final Callable<?> callable) {
        try {
            callable.call();
            throw new AssertionError(
                    "Expected Exception 'InsufficientPermissionException' to be thrown, but request passed with no exception.");
        } catch (Exception ex) {
            assertThat(ex).isInstanceOf(InsufficientPermissionException.class);
        }
    }

    /**
     * Asserts that the given callable succeeds.
     * </p>
     * Note: This method will assume that EntityNotFoundException is OK, as security tests use dummy (non-existing) IDs.
     * It matters to either callable succeeds without any exception or at most EntityNotFoundException.
     * All other cases will be considered as an error.
     *
     * @param callable the callable to call
     */
    private void assertPermissionWorks(final Callable<?> callable) {
        try {
            callable.call();
        } catch (final Throwable th) {
            if (th instanceof EntityNotFoundException) {
                log.info("Expected (at most) EntityNotFoundException catch: {}", th.getMessage());
            } else {
                throw new AssertionError("Expected no Exception (other then EntityNotFound) to be thrown, but got: " + th.getMessage(), th);
            }
        }
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

    protected Slice<JpaDistributionSet> findDsByDistributionSetFilter(final DistributionSetFilter distributionSetFilter, final Pageable pageable) {
        final List<Specification<JpaDistributionSet>> specList = buildDistributionSetSpecifications(distributionSetFilter);
        return JpaManagementHelper.findAllWithoutCountBySpec(distributionSetRepository, specList, pageable);
    }

    private static List<Specification<JpaDistributionSet>> buildDistributionSetSpecifications(
            final DistributionSetFilter distributionSetFilter) {
        final List<Specification<JpaDistributionSet>> specList = new ArrayList<>(10);

        if (distributionSetFilter.getIsComplete() != null) {
            specList.add(DistributionSetSpecification.isCompleted(distributionSetFilter.getIsComplete()));
        }
        if (distributionSetFilter.getIsDeleted() != null) {
            specList.add(DistributionSetSpecification.isDeleted(distributionSetFilter.getIsDeleted()));
        }
        if (distributionSetFilter.getIsValid() != null) {
            specList.add(DistributionSetSpecification.isValid(distributionSetFilter.getIsValid()));
        }
        if (distributionSetFilter.getTypeId() != null) {
            specList.add(DistributionSetSpecification.byType(distributionSetFilter.getTypeId()));
        }
        if (!ObjectUtils.isEmpty(distributionSetFilter.getSearchText())) {
            final String[] dsFilterNameAndVersionEntries = JpaManagementHelper
                    .getFilterNameAndVersionEntries(distributionSetFilter.getSearchText().trim());
            specList.add(DistributionSetSpecification.likeNameAndVersion(dsFilterNameAndVersionEntries[0], dsFilterNameAndVersionEntries[1]));
        }
        if (hasTagsFilterActive(distributionSetFilter)) {
            specList.add(DistributionSetSpecification.hasTags(
                    distributionSetFilter.getTagNames(), distributionSetFilter.getSelectDSWithNoTag()));
        }
        return specList;
    }

    private static boolean hasTagsFilterActive(final DistributionSetFilter distributionSetFilter) {
        final boolean isNoTagActive = Boolean.TRUE.equals(distributionSetFilter.getSelectDSWithNoTag());
        final boolean isAtLeastOneTagActive = !CollectionUtils.isEmpty(distributionSetFilter.getTagNames());
        return isNoTagActive || isAtLeastOneTagActive;
    }
}