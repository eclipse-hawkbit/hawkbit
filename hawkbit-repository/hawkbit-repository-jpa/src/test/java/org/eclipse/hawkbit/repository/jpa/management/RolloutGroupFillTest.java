/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.hawkbit.repository.RolloutManagement.Create;
import org.eclipse.hawkbit.repository.RolloutManagement.GroupCreate;
import org.eclipse.hawkbit.repository.TargetManagement.Update;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.test.util.ConflictInjection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The rollout group fill (on the rollout creation) assigns the targets in id order, in pages of one transaction each, and
 * continues an interrupted fill from the assigned targets.
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: Rollout Management - group fill
 */
class RolloutGroupFillTest extends AbstractJpaIntegrationTest {

    @Autowired
    private PlatformTransactionManager txManager;

    /**
     * Verifies that after a failing fill the committed pages are kept and the next run continues the fill without duplicates
     * and with the same group contents as an uninterrupted fill: the save of the filled first group fails, the next run sets
     * the first group READY with the targets assigned already and fills the second group.
     */
    @Test
    void failedGroupSaveResumesFillInNextRun() {
        final String prefix = "fill-resume-";
        final List<Target> targets = byId(testdataFactory.createTargets(prefix, 0, 10));
        final Rollout rollout = createRollout(prefix, 2);

        // the first group save is the one of the filled first group (READY)
        try (ConflictInjection _ = ConflictInjection.afterRead(txManager, rolloutGroupRepository, "save", () -> {
            throw new IllegalStateException("injected failure of the save of the filled group");
        })) {
            rolloutHandler.handleAll();
        }

        final List<RolloutGroup> interrupted = groups(rollout);
        assertThat(interrupted.get(0).getStatus()).isEqualTo(RolloutGroupStatus.CREATING);
        assertThat(controllerIds(interrupted.get(0))).containsExactlyElementsOf(controllerIds(targets.subList(0, 5)));
        assertThat(interrupted.get(1).getStatus()).isEqualTo(RolloutGroupStatus.CREATING);
        assertThat(controllerIds(interrupted.get(1))).isEmpty();
        assertThat(rolloutManagement.get(rollout.getId()).getStatus()).isEqualTo(RolloutStatus.CREATING);

        rolloutHandler.handleAll();

        final List<RolloutGroup> groups = groups(rollout);
        assertThat(groups).allSatisfy(group -> {
            assertThat(group.getStatus()).isEqualTo(RolloutGroupStatus.READY);
            assertThat(group.getTotalTargets()).isEqualTo(5);
        });
        assertThat(controllerIds(groups.get(0))).containsExactlyElementsOf(controllerIds(targets.subList(0, 5)));
        assertThat(controllerIds(groups.get(1))).containsExactlyElementsOf(controllerIds(targets.subList(5, 10)));
        assertThat(rolloutManagement.get(rollout.getId()).getStatus()).isEqualTo(RolloutStatus.READY);
        assertThat(rolloutManagement.get(rollout.getId()).getTotalTargets()).isEqualTo(10);
    }

    /**
     * Verifies that a group with an own target filter takes the matching targets in id order, and the groups without own
     * target filter after it take the rest in id order - also the targets below the ids the filtered group took.
     */
    @Test
    void groupsWithAndWithoutOwnFilterTakeTargetsInIdOrder() {
        final String prefix = "fill-mixed-";
        final List<Target> as = new ArrayList<>();
        final List<Target> bs = new ArrayList<>();
        // interleaved, so the ids of the a- and b-targets alternate
        for (int i = 0; i < 6; i++) {
            as.add(testdataFactory.createTarget(prefix + "a" + i));
            bs.add(testdataFactory.createTarget(prefix + "b" + i));
        }
        final RolloutGroupConditions conditions = new RolloutGroupConditionBuilder().withDefaults().build();
        final Rollout created = rolloutManagement.create(
                rolloutCreate(prefix),
                List.of(
                        // 50 % of the 6 b-targets: b0 - b2
                        GroupCreate.builder().conditions(conditions).name("filtered").targetPercentage(50.0F)
                                .targetFilterQuery("controllerid==" + prefix + "b*").build(),
                        // 50 % (of the rest of the 100 % without filter) of the 9 left: 5 (4.5 rounded)
                        GroupCreate.builder().conditions(conditions).name("half").targetPercentage(50.0F).build(),
                        // the remaining 4
                        GroupCreate.builder().conditions(conditions).name("rest").targetPercentage(100.0F).build()),
                conditions);

        rolloutHandler.handleAll();

        final List<RolloutGroup> groups = groups(created);
        assertThat(controllerIds(groups.get(0))).containsExactlyElementsOf(controllerIds(bs.subList(0, 3)));
        assertThat(controllerIds(groups.get(1))).containsExactlyElementsOf(
                controllerIds(List.of(as.get(0), as.get(1), as.get(2), as.get(3), bs.get(3))));
        assertThat(controllerIds(groups.get(2))).containsExactlyElementsOf(
                controllerIds(List.of(as.get(4), bs.get(4), as.get(5), bs.get(5))));
        assertThat(groups).extracting(RolloutGroup::getTotalTargets).containsExactly(3, 5, 4);
        assertThat(rolloutManagement.get(created.getId()).getStatus()).isEqualTo(RolloutStatus.READY);
        assertThat(rolloutManagement.get(created.getId()).getTotalTargets()).isEqualTo(12);
    }

    /**
     * Verifies that a target with an id below the fill cursor, which matches the rollout's filter only after the group before
     * has been filled, is assigned too: when the candidates after the cursor run out before the group is complete, they are
     * read once more from the start. The fill of the second group fails in the first run (the targets of its pages are
     * kept), then the lowest target gets matching, so the resumed fill of the second group finds it only below the cursor.
     */
    @Test
    void targetMatchingLaterBelowTheCursorIsAssigned() {
        final String prefix = "fill-below-cursor-";
        // the lowest target doesn't match the filter (by name) yet
        final Target low = testdataFactory.createTarget(prefix + "0", "other-" + prefix + "0");
        final List<Target> matching = new ArrayList<>();
        for (int i = 1; i < 10; i++) {
            matching.add(testdataFactory.createTarget(prefix + i, prefix + i));
        }
        final DistributionSet ds = testdataFactory.createDistributionSetLocked(prefix + "rollout");
        final Rollout rollout = rolloutManagement.create(
                Create.builder().name(prefix).description(prefix).targetFilterQuery("name==" + prefix + "*").distributionSet(ds).build(),
                2, false, new RolloutGroupConditionBuilder().withDefaults().build());

        // 9 matching: 5 for the first group, 4 for the second, whose save (the second save) fails
        final AtomicInteger saves = new AtomicInteger();
        try (ConflictInjection _ = ConflictInjection.afterEachRead(txManager, rolloutGroupRepository, "save", () -> {
            if (saves.incrementAndGet() == 2) {
                throw new IllegalStateException("injected failure of the save of the second filled group");
            }
        })) {
            rolloutHandler.handleAll();
        }
        assertThat(controllerIds(groups(rollout).get(1))).containsExactlyElementsOf(controllerIds(byId(matching.subList(5, 9))));

        targetManagement.update(Update.builder().id(low.getId()).name(prefix + "0").build());
        rolloutHandler.handleAll();

        final List<RolloutGroup> groups = groups(rollout);
        assertThat(controllerIds(groups.get(0))).containsExactlyElementsOf(controllerIds(byId(matching.subList(0, 5))));
        final List<Target> second = new ArrayList<>(List.of(low));
        second.addAll(matching.subList(5, 9));
        assertThat(controllerIds(groups.get(1))).containsExactlyElementsOf(controllerIds(byId(second)));
        assertThat(groups).extracting(RolloutGroup::getTotalTargets).containsExactly(5, 5);
        assertThat(rolloutManagement.get(rollout.getId()).getTotalTargets()).isEqualTo(10);
    }

    // a rollout in CREATING, the groups not filled yet (the rollout handler is not run)
    private Rollout createRollout(final String prefix, final int groups) {
        return rolloutManagement.create(
                rolloutCreate(prefix), groups, false, new RolloutGroupConditionBuilder().withDefaults().build());
    }

    private Create rolloutCreate(final String prefix) {
        final DistributionSet ds = testdataFactory.createDistributionSetLocked(prefix + "rollout");
        return Create.builder().name(prefix).description(prefix).targetFilterQuery("controllerid==" + prefix + "*").distributionSet(ds).build();
    }

    private List<RolloutGroup> groups(final Rollout rollout) {
        return rolloutGroupManagement.findByRollout(rollout.getId(), PageRequest.of(0, 100, Sort.by("id"))).getContent();
    }

    // the controller ids of the group targets (rollout target group rows), in id order - read directly, as the management
    // reads the targets of the group's actions unless the rollout is READY
    private List<String> controllerIds(final RolloutGroup group) {
        return controllerIds(byId(targetRepository.findAll(TargetSpecifications.isInRolloutGroup(group.getId())).stream()
                .map(Target.class::cast)
                .toList()));
    }

    // the targets in id order - the order the fill assigns them in (the batch creation doesn't create them in the order of the
    // controller ids)
    private static List<Target> byId(final List<? extends Target> targets) {
        return targets.stream().sorted(Comparator.comparing(Target::getId)).map(Target.class::cast).toList();
    }

    private static List<String> controllerIds(final List<Target> targets) {
        return targets.stream().map(Target::getControllerId).toList();
    }
}
