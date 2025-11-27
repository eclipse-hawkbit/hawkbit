/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ql.rsql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.repository.TargetManagement.Create;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.qfields.ActionFields;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Slice;
import org.springframework.orm.jpa.vendor.Database;

/**
 * Feature: Component Tests - Repository<br/>
 * Story: RSQL filter actions
 */
class RsqlActionFieldsTest extends AbstractJpaIntegrationTest {

    private JpaTarget target;
    private JpaAction action;

    @BeforeEach
    void setupBeforeTest() {
        final DistributionSet dsA = testdataFactory.createDistributionSet("daA");
        target = (JpaTarget) targetManagement.create(Create.builder().controllerId("targetId123").description("targetId123").build());

        action = newJpaAction(dsA, false, null);
        for (int i = 0; i < 10; i++) {
            newJpaAction(dsA, i % 2 == 0, i % 2 == 0 ? "extRef" : "extRef2");
        }
    }

    /**
     * Test filter action by id
     */
    @Test
    void testFilterByParameterId() {
        assertRSQLQuery(ActionFields.ID.name() + "==" + action.getId(), 1);
        assertRSQLQuery(ActionFields.ID.name() + "!=" + action.getId(), 10);
        assertRSQLQuery(ActionFields.ID.name() + "==" + -1, 0);
        assertRSQLQuery(ActionFields.ID.name() + "!=" + -1, 11);

        // Not supported for numbers
        if (Database.POSTGRESQL.equals(getDatabase())) {
            return;
        }

        assertRSQLQuery(ActionFields.ID.name() + "=in=(" + action.getId() + ",10000000)", 1);
        assertRSQLQuery(ActionFields.ID.name() + "=out=(" + action.getId() + ",10000000)", 10);
    }

    /**
     * Test action by status
     */
    @Test
    void testFilterByParameterActive() {
        assertRSQLQuery(ActionFields.ACTIVE.name() + "==" + true, 5);
        assertRSQLQuery(ActionFields.ACTIVE.name() + "!=" + true, 6);
        assertRSQLQuery(ActionFields.ACTIVE.name() + "=in=(" + true + ")", 5);
        assertRSQLQuery(ActionFields.ACTIVE.name() + "=out=(" + true + ")", 6);

        final String rsql = ActionFields.ACTIVE.name() + "==true2";
        assertThatExceptionOfType(RSQLParameterSyntaxException.class)
                .as("RSQLParameterUnsupportedFieldException because active cannot be compared with 'true2'")
                .isThrownBy(() -> assertRSQLQuery(rsql, 5));
    }

    /**
     * Test action by status
     */
    @Test
    void testFilterByParameterStatus() {
        assertRSQLQuery(ActionFields.STATUS.name() + "==" + Status.RUNNING, 5);
        assertRSQLQuery(ActionFields.STATUS.name() + "!=" + Status.RUNNING, 6);
        assertRSQLQuery(ActionFields.STATUS.name() + "=in=(" + Status.RUNNING + ")", 5);
        assertRSQLQuery(ActionFields.STATUS.name() + "=out=(" + Status.RUNNING + ")", 6);

        final String rsql = ActionFields.STATUS.name() + "==not_a_status";
        assertThatExceptionOfType(RSQLParameterUnsupportedFieldException.class)
                .as("RSQLParameterUnsupportedFieldException because status cannot be compared with 'not_a_status'")
                .isThrownBy(() -> assertRSQLQuery(rsql, 5));
    }

    /**
     * Test action by status
     */
    @Test
    void testFilterByParameterExtRef() {
        assertRSQLQuery(ActionFields.EXTERNALREF.name() + "==extRef", 5);
        assertRSQLQuery(ActionFields.EXTERNALREF.name() + "!=extRef", 6);
        assertRSQLQuery(ActionFields.EXTERNALREF.name() + "==extRef*", 10);
    }

    private JpaAction newJpaAction(final DistributionSet dsA, final boolean active, final String extRef) {
        final JpaAction newAction = new JpaAction();
        newAction.setActionType(ActionType.SOFT);
        newAction.setDistributionSet(dsA);
        newAction.setActive(active);
        newAction.setStatus(active ? Status.RUNNING : Status.FINISHED);
        newAction.setTarget(target);
        newAction.setWeight(45);
        newAction.setInitiatedBy(AccessContext.actor());
        if (extRef != null) {
            newAction.setExternalRef(extRef);
        }
        actionRepository.save(newAction);

        target.addAction(action);

        return newAction;
    }

    private void assertRSQLQuery(final String rsql, final long expectedEntities) {
        final Slice<Action> findEntity = deploymentManagement.findActionsByTarget(rsql, target.getControllerId(), PAGE);
        final long countAllEntities = deploymentManagement.countActionsByTarget(rsql, target.getControllerId());
        assertThat(findEntity).isNotNull();
        assertThat(findEntity.getContent()).hasSize((int) expectedEntities);
        assertThat(countAllEntities).isEqualTo(expectedEntities);
    }
}