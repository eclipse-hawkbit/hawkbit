/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.junit.jupiter.api.Test;

/**
 * Feature: Unit Tests - Repository<br/>
 * Story: Repository Model
 */
class ModelEqualsHashcodeTest extends AbstractJpaIntegrationTest {

    /**
     * Verifies that different objects even with identical primary key, version and tenant return different hash codes.
     */
    @Test    void differentEntitiesReturnDifferentHashCodes() {
        assertThat(new JpaAction().hashCode()).as("action should have different hashcode than action status")
                .isNotEqualTo(new JpaActionStatus().hashCode());
        assertThat(new JpaDistributionSet().hashCode())
                .as("Distribution set should have different hashcode than software module")
                .isNotEqualTo(new JpaSoftwareModule().hashCode());
        assertThat(new JpaDistributionSet().hashCode())
                .as("Distribution set should have different hashcode than action status")
                .isNotEqualTo(new JpaActionStatus().hashCode());
        assertThat(new JpaDistributionSetType().hashCode())
                .as("Distribution set type should have different hashcode than action status")
                .isNotEqualTo(new JpaActionStatus().hashCode());
    }

    /**
     * Verifies that different object even with identical primary key, version and tenant are not equal.
     */
    @Test    void differentEntitiesAreNotEqual() {
        assertThat(new JpaAction().equals(new JpaActionStatus())).as("action equals action status").isFalse();
        assertThat(new JpaDistributionSet().equals(new JpaSoftwareModule()))
                .as("Distribution set equals software module").isFalse();
        assertThat(new JpaDistributionSet().equals(new JpaActionStatus())).as("Distribution set equals action status")
                .isFalse();
        assertThat(new JpaDistributionSetType().equals(new JpaActionStatus()))
                .as("Distribution set type equals action status").isFalse();
    }

    /**
     * Verifies that updated entities are not equal.
     */
    @Test    void changedEntitiesAreNotEqual() {
        final SoftwareModuleType type = softwareModuleTypeManagement
                .create(entityFactory.softwareModuleType().create().key("test").name("test"));
        assertThat(type).as("persited entity is not equal to regular object")
                .isNotEqualTo(entityFactory.softwareModuleType().create().key("test").name("test").build());

        final SoftwareModuleType updated = softwareModuleTypeManagement.update(
                entityFactory.softwareModuleType().update(type.getId()).description("another"));
        assertThat(type).as("Changed entity is not equal to the previous version").isNotEqualTo(updated);
    }

    /**
     * Verify that no proxy of the entity manager has an influence on the equals or hashcode result.
     */
    @Test    void managedEntityIsEqualToUnamangedObjectWithSameKey() {
        final SoftwareModuleType type = softwareModuleTypeManagement.create(
                entityFactory.softwareModuleType().create().key("test").name("test").description("test"));

        final JpaSoftwareModuleType mock = new JpaSoftwareModuleType("test", "test", "test", 1);
        mock.setId(type.getId());
        mock.setOptLockRevision(type.getOptLockRevision());
        mock.setTenant(type.getTenant());

        assertThat(type).as("managed entity is equal to regular object with same content").isEqualTo(mock);
        assertThat(type)
                .as("managed entity has same hash code as regular object with same content")
                .hasSameHashCodeAs(mock.hashCode());
    }

}
