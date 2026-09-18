/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.util.List;
import java.util.stream.IntStream;

import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.repository.TargetManagement.Create;
import org.eclipse.hawkbit.repository.TargetManagement.Update;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests for the "distinct target groups per tenant" quota
 * ({@code hawkbit.server.security.dos.maxTargetGroups}).
 */
class TargetGroupQuotaTest extends AbstractJpaIntegrationTest {

    private static final String GERMANY = "Germany";
    private static final String FRANCE = "France";
    private static final String SPAIN = "Spain";
    private static final String ITALY = "Italy";
    private static final String PORTUGAL = "Portugal";
    private static final String BELGIUM = "Belgium";

    @Autowired
    private HawkbitSecurityProperties securityProperties;

    private int originalLimit;

    @BeforeEach
    void rememberLimit() {
        originalLimit = securityProperties.getDos().getMaxTargetGroups();
    }

    @AfterEach
    void restoreLimit() {
        securityProperties.getDos().setMaxTargetGroups(originalLimit);
    }

    /**
     * create(Create) rejects a new group once the tenant is at the limit.
     */
    @Test
    void createSingleRejectsNewGroupBeyondLimit() {
        seedGroups(GERMANY, FRANCE, SPAIN);
        limit(3);

        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> targetManagement.create(Create.builder().controllerId("over-1").group(ITALY).build()));
    }

    /**
     * bulk create rejects a new group once the tenant is at the limit.
     */
    @Test
    void createBatchRejectsNewGroupBeyondLimit() {
        seedGroups(GERMANY, FRANCE, SPAIN);
        limit(3);

        final List<Create> creates = List.of(
                Create.builder().controllerId("over-1").group(ITALY).build(),
                Create.builder().controllerId("over-2").group(ITALY).build());
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> targetManagement.create(creates));
    }

    /**
     * update rejects a new group once the tenant is at the limit.
     */
    @Test
    void updateSingleRejectsNewGroupBeyondLimit() {
        final Target plain = targetManagement.create(Create.builder().controllerId("plain").build());
        seedGroups(GERMANY, FRANCE, SPAIN);
        limit(3);

        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> targetManagement.update(Update.builder().id(plain.getId()).group(ITALY).build()));
    }

    /**
     * bulk update rejects a new group once the tenant is at the limit.
     */
    @Test
    void updateBatchRejectsNewGroupBeyondLimit() {
        final Target plain1 = targetManagement.create(Create.builder().controllerId("plain-1").build());
        final Target plain2 = targetManagement.create(Create.builder().controllerId("plain-2").build());
        seedGroups(GERMANY, FRANCE, SPAIN);
        limit(3);

        final List<Update> updates = List.of(
                Update.builder().id(plain1.getId()).group(ITALY).build(),
                Update.builder().id(plain2.getId()).group(ITALY).build());
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> targetManagement.update(updates));
    }

    /**
     * assignTargetsWithGroup rejects a new group once the tenant is at the limit.
     */
    @Test
    void assignTargetsWithGroupRejectsNewGroupBeyondLimit() {
        targetManagement.create(Create.builder().controllerId("plain").build());
        seedGroups(GERMANY, FRANCE, SPAIN);
        limit(3);

        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> targetManagement.assignTargetsWithGroup(ITALY, List.of("plain")));
    }

    /**
     * assignTargetGroupWithRsql rejects a new group once the tenant is at the limit (direct, non-negated path).
     */
    @Test
    void assignTargetGroupWithRsqlRejectsNewGroupBeyondLimit() {
        targetManagement.create(Create.builder().controllerId("plain").build());
        seedGroups(GERMANY, FRANCE, SPAIN);
        limit(3);

        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> targetManagement.assignTargetGroupWithRsql(ITALY, "controllerId==plain"));
    }

    /**
     * An existing group is assignable when the tenant sits exactly at the limit.
     */
    @Test
    void existingGroupIsAssignableWhenExactlyAtLimit() {
        targetManagement.create(Create.builder().controllerId("plain").build());
        seedGroups(GERMANY, FRANCE, SPAIN);
        limit(3);

        assertThatNoException().isThrownBy(() -> targetManagement.assignTargetsWithGroup(GERMANY, List.of("plain")));
        assertThat(groupsOfCurrentTenant()).containsExactlyInAnyOrder(GERMANY, FRANCE, SPAIN);
    }

    /**
     * An existing group is assignable when the tenant is already over the limit - the single-group fast path.
     */
    @Test
    void existingGroupIsAssignableWhenOverLimit() {
        targetManagement.create(Create.builder().controllerId("plain").build());
        seedGroups(GERMANY, FRANCE, SPAIN, ITALY, PORTUGAL);
        limit(3); // tenant now holds 5 groups against a limit of 3

        assertThatNoException()
                .isThrownBy(() -> targetManagement.create(Create.builder().controllerId("more").group(GERMANY).build()));
        assertThat(groupsOfCurrentTenant()).containsExactlyInAnyOrder(GERMANY, FRANCE, SPAIN, ITALY, PORTUGAL);
    }

    /**
     * Several existing groups are assignable in one batch when the tenant is over the limit. Exercises the removeAll branch rather
     * than the size==1 existsByGroup fast path.
     */
    @Test
    void multipleExistingGroupsAreAssignableWhenOverLimit() {
        seedGroups(GERMANY, FRANCE, SPAIN, ITALY, PORTUGAL);
        limit(3);

        final List<Create> creates = List.of(
                Create.builder().controllerId("more-1").group(GERMANY).build(),
                Create.builder().controllerId("more-2").group(FRANCE).build());
        assertThatNoException().isThrownBy(() -> targetManagement.create(creates));
        assertThat(groupsOfCurrentTenant()).containsExactlyInAnyOrder(GERMANY, FRANCE, SPAIN, ITALY, PORTUGAL);
    }

    /**
     * The unassign paths pass a null group and must not fail at the limit. Guards against a List.of(group) regression, which would
     * throw NullPointerException
     */
    @Test
    void nullGroupUnassignPathsAreAllowedAtLimit() {
        targetManagement.create(Create.builder().controllerId("unassign-1").group(GERMANY).build());
        targetManagement.create(Create.builder().controllerId("unassign-2").group(GERMANY).build());
        seedGroups(GERMANY, FRANCE, SPAIN);
        limit(3);

        assertThatNoException().isThrownBy(() -> targetManagement.assignTargetsWithGroup(null, List.of("unassign-1")));
        assertThatNoException().isThrownBy(() -> targetManagement.assignTargetGroupWithRsql(null, "controllerId==unassign-2"));
    }

    /**
     * A limit of zero means unlimited.
     */
    @Test
    void zeroLimitDisablesEnforcement() {
        seedGroups(GERMANY, FRANCE, SPAIN);
        limit(0);

        assertThatNoException()
                .isThrownBy(() -> targetManagement.create(Create.builder().controllerId("unlimited").group(ITALY).build()));
        assertThat(groupsOfCurrentTenant()).contains(ITALY);
    }

    /**
     * A negative limit means unlimited.
     */
    @Test
    void negativeLimitDisablesEnforcement() {
        seedGroups(GERMANY, FRANCE, SPAIN);
        limit(-1);

        assertThatNoException()
                .isThrownBy(() -> targetManagement.create(Create.builder().controllerId("unlimited").group(ITALY).build()));
        assertThat(groupsOfCurrentTenant()).contains(ITALY);
    }

    /**
     * An RSQL containing a negation routes through assignTargetGroupOnChunks and must be quota checked too.
     */
    @Test
    void chunkedRsqlPathRejectsNewGroupBeyondLimit() {
        //Assumptions.assumeTrue(Jpa.JPA_VENDOR == Jpa.JpaVendor.ECLIPSELINK, "chunked path only exists on EclipseLink");
        targetManagement.create(Create.builder().controllerId("plain").build());
        seedGroups(GERMANY, FRANCE, SPAIN);
        limit(3);

        // "!=" makes containsNegation true -> assignTargetGroupOnChunks
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> targetManagement.assignTargetGroupWithRsql(ITALY, "controllerId!=doesNotExist"));
    }

    /**
     * The chunked path still assigns an existing group when the tenant is over the limit.
     */
    @Test
    void chunkedRsqlPathAllowsExistingGroupWhenOverLimit() {
        //Assumptions.assumeTrue(Jpa.JPA_VENDOR == Jpa.JpaVendor.ECLIPSELINK, "chunked path only exists on EclipseLink");
        targetManagement.create(Create.builder().controllerId("plain").build());
        seedGroups(GERMANY, FRANCE, SPAIN, ITALY, PORTUGAL);
        limit(3);

        assertThatNoException()
                .isThrownBy(() -> targetManagement.assignTargetGroupWithRsql(GERMANY, "controllerId!=doesNotExist"));
        assertThat(targetManagement.getByControllerId("plain").getGroup()).isEqualTo(GERMANY);
    }

    /**
     * A batch create of many targets sharing one new group consumes exactly one group.
     */
    @Test
    void batchCreateSharingOneNewGroupConsumesOneGroup() {
        seedGroups(GERMANY, FRANCE);
        limit(3);

        final List<Create> creates = IntStream.range(0, 10)
                .<Create> mapToObj(i -> Create.builder().controllerId("bulk-" + i).group(SPAIN).build())
                .toList();
        assertThatNoException().isThrownBy(() -> targetManagement.create(creates));
        assertThat(groupsOfCurrentTenant()).containsExactlyInAnyOrder(GERMANY, FRANCE, SPAIN);
    }

    /**
     * A batch carrying k distinct new groups is asserted once and must be rejected when it would land at n + k, not merely n + 1.
     * With one existing group and a limit of three, three new groups would land at four.
     */
    @Test
    void batchCreateWithDistinctNewGroupsIsAssertedOnce() {
        seedGroups(GERMANY);
        limit(3);

        final List<Create> creates = List.of(
                Create.builder().controllerId("k-1").group(FRANCE).build(),
                Create.builder().controllerId("k-2").group(SPAIN).build(),
                Create.builder().controllerId("k-3").group(ITALY).build());
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> targetManagement.create(creates));
        assertThat(groupsOfCurrentTenant()).containsExactly(GERMANY);
    }

    /**
     * The same batch shape is allowed when n + k lands exactly on the limit.
     */
    @Test
    void batchCreateWithDistinctNewGroupsIsAllowedExactlyAtLimit() {
        seedGroups(GERMANY);
        limit(3);

        final List<Create> creates = List.of(
                Create.builder().controllerId("k-1").group(FRANCE).build(),
                Create.builder().controllerId("k-2").group(SPAIN).build());
        assertThatNoException().isThrownBy(() -> targetManagement.create(creates));
        assertThat(groupsOfCurrentTenant()).containsExactlyInAnyOrder(GERMANY, FRANCE, SPAIN);
    }

    /**
     * Groups belonging to another tenant do not count towards this tenant's cardinality.
     */
    @Test
    void otherTenantGroupsDoNotCountTowardsThisTenant() {
        limit(0);
        // uppercase tenant name so that this test isolates tenant scoping and is not confounded by the casing defect
        SecurityContextSwitch.runAs(
                SecurityContextSwitch.withTenantAndUserAndAllPermissions("OTHERTENANT", "otheruser"),
                () -> {
                    targetManagement.create(Create.builder().controllerId("other-1").group(ITALY).build());
                    targetManagement.create(Create.builder().controllerId("other-2").group(PORTUGAL).build());
                    targetManagement.create(Create.builder().controllerId("other-3").group(BELGIUM).build());
                });

        seedGroups(GERMANY, FRANCE);
        limit(3);

        // this tenant holds 2 groups, so a third is allowed - it would be rejected if the other tenant's 3 groups were counted
        assertThatNoException()
                .isThrownBy(() -> targetManagement.create(Create.builder().controllerId("mine").group(SPAIN).build()));
        assertThat(groupsOfCurrentTenant()).containsExactlyInAnyOrder(GERMANY, FRANCE, SPAIN);
    }


    /**
     * Rejection surfaces as AssignmentQuotaExceededException, carrying the SP_QUOTA_EXCEEDED error.
     */
    @Test
    void rejectionThrowsAssignmentQuotaExceededException() {
        seedGroups(GERMANY, FRANCE, SPAIN);
        limit(3);

        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> targetManagement.create(Create.builder().controllerId("over").group(ITALY).build()))
                .satisfies(e -> assertThat(e.getError().getKey()).isEqualTo("hawkbit.server.error.quota.tooManyEntries"));
    }

    /**
     * Updating a nonexistent target with a new group while at the limit therefore reports NOT FOUND rather than quota-exceeded.
     */
    @Test
    void updateOfNonexistentTargetAtLimitReportsQuotaExceededRatherThanNotFound() {
        seedGroups(GERMANY, FRANCE, SPAIN);
        limit(3);

        assertThatExceptionOfType(EntityNotFoundException.class)
                .isThrownBy(() -> targetManagement.update(Update.builder().id(123456789L).group(ITALY).build()));
    }

    /**
     * The same ordering guard for the batch overload. update(Collection) resolves its ids through findAllById(ids, true), so a batch
     * containing a nonexistent id while at the limit must report NOT FOUND rather than quota-exceeded.
     */
    @Test
    void updateBatchOfNonexistentTargetAtLimitReportsNotFoundRatherThanQuotaExceeded() {
        final Target plain = targetManagement.create(Create.builder().controllerId("plain").build());
        seedGroups(GERMANY, FRANCE, SPAIN);
        limit(3);

        final List<Update> updates = List.of(
                Update.builder().id(plain.getId()).group(ITALY).build(),
                Update.builder().id(123456789L).group(ITALY).build());
        assertThatExceptionOfType(EntityNotFoundException.class)
                .isThrownBy(() -> targetManagement.update(updates));
    }

    private void limit(final int limit) {
        securityProperties.getDos().setMaxTargetGroups(limit);
    }

    /**
     * Seeds one target per group with enforcement disabled, so that over-limit states are reachable. Leaves the limit at 0 - callers
     * set the limit under test afterwards.
     */
    private void seedGroups(final String... groups) {
        limit(0);
        for (final String group : groups) {
            targetManagement.create(Create.builder().controllerId("seed-" + group).group(group).build());
        }
    }

    /**
     * findGroups is native and needs the stored (upper-cased) tenant, see design 6.1.
     */
    private List<String> groupsOfCurrentTenant() {
        return targetManagement.findGroups(AccessContext.tenant().toUpperCase());
    }
}
