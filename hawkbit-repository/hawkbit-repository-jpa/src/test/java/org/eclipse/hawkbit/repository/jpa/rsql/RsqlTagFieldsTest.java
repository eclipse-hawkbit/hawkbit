/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.TagFields;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * Feature: Component Tests - Repository<br/>
 * Story: RSQL filter target and distribution set tags
 */
class RsqlTagFieldsTest extends AbstractJpaIntegrationTest {

    @BeforeEach
    void seuptBeforeTest() {
        for (int i = 0; i < 5; i++) {
            targetTagManagement.create(TargetTagManagement.Create.builder()
                    .name(Integer.toString(i)).description(Integer.toString(i)).colour(i % 2 == 0 ? "red" : "blue").build());
            distributionSetTagManagement.create(DistributionSetTagManagement.Create.builder()
                    .name(Integer.toString(i)).description(Integer.toString(i)).colour(i % 2 == 0 ? "red" : "blue").build());
        }
    }

    /**
     * Test filter target tag by name
     */
    @Test
    void testFilterTargetTagByParameterName() {
        assertRSQLQueryTarget(TagFields.NAME.name() + "==''", 0);
        assertRSQLQueryTarget(TagFields.NAME.name() + "!=''", 5);
        assertRSQLQueryTarget(TagFields.NAME.name() + "==1", 1);
        assertRSQLQueryTarget(TagFields.NAME.name() + "!=1", 4);
        assertRSQLQueryTarget(TagFields.NAME.name() + "==*", 5);
        assertRSQLQueryTarget(TagFields.NAME.name() + "==noExist*", 0);
        assertRSQLQueryTarget(TagFields.NAME.name() + "=in=(1,notexist)", 1);
        assertRSQLQueryTarget(TagFields.NAME.name() + "=out=(1,notexist)", 4);
    }

    /**
     * Test filter target tag by description
     */
    @Test
    void testFilterTargetTagByParameterDescription() {
        assertRSQLQueryTarget(TagFields.DESCRIPTION.name() + "==''", 0);
        assertRSQLQueryTarget(TagFields.DESCRIPTION.name() + "!=''", 5);
        assertRSQLQueryTarget(TagFields.DESCRIPTION.name() + "==1", 1);
        assertRSQLQueryTarget(TagFields.DESCRIPTION.name() + "!=1", 4);
        assertRSQLQueryTarget(TagFields.DESCRIPTION.name() + "==*", 5);
        assertRSQLQueryTarget(TagFields.DESCRIPTION.name() + "==noExist*", 0);
        assertRSQLQueryTarget(TagFields.DESCRIPTION.name() + "=in=(1,notexist)", 1);
        assertRSQLQueryTarget(TagFields.DESCRIPTION.name() + "=out=(1,notexist)", 4);
    }

    /**
     * Test filter target tag by colour
     */
    @Test
    void testFilterTargetTagByParameterColour() {
        assertRSQLQueryTarget(TagFields.COLOUR.name() + "==''", 0);
        assertRSQLQueryTarget(TagFields.COLOUR.name() + "!=''", 5);
        assertRSQLQueryTarget(TagFields.COLOUR.name() + "==red", 3);
        assertRSQLQueryTarget(TagFields.COLOUR.name() + "!=red", 2);
        assertRSQLQueryTarget(TagFields.COLOUR.name() + "==r*", 3);
        assertRSQLQueryTarget(TagFields.COLOUR.name() + "==noExist*", 0);
        assertRSQLQueryTarget(TagFields.COLOUR.name() + "=in=(red,notexist)", 3);
        assertRSQLQueryTarget(TagFields.COLOUR.name() + "=out=(red,notexist)", 2);
    }

    /**
     * Test filter distribution set tag by name
     */
    @Test
    void testFilterDistributionSetTagByParameterName() {
        assertRSQLQueryDistributionSet(TagFields.NAME.name() + "==''", 0);
        assertRSQLQueryDistributionSet(TagFields.NAME.name() + "!=''", 5);
        assertRSQLQueryDistributionSet(TagFields.NAME.name() + "==1", 1);
        assertRSQLQueryDistributionSet(TagFields.NAME.name() + "!=1", 4);
        assertRSQLQueryDistributionSet(TagFields.NAME.name() + "==*", 5);
        assertRSQLQueryDistributionSet(TagFields.NAME.name() + "==noExist*", 0);
        assertRSQLQueryDistributionSet(TagFields.NAME.name() + "=in=(1,2)", 2);
        assertRSQLQueryDistributionSet(TagFields.NAME.name() + "=out=(1,2)", 3);
    }

    /**
     * Test filter distribution set by description
     */
    @Test
    void testFilterDistributionSetTagByParameterDescription() {
        assertRSQLQueryDistributionSet(TagFields.DESCRIPTION.name() + "==''", 0);
        assertRSQLQueryDistributionSet(TagFields.DESCRIPTION.name() + "!=''", 5);
        assertRSQLQueryDistributionSet(TagFields.DESCRIPTION.name() + "==1", 1);
        assertRSQLQueryDistributionSet(TagFields.DESCRIPTION.name() + "!=1", 4);
        assertRSQLQueryDistributionSet(TagFields.DESCRIPTION.name() + "==*", 5);
        assertRSQLQueryDistributionSet(TagFields.DESCRIPTION.name() + "==noExist*", 0);
        assertRSQLQueryDistributionSet(TagFields.DESCRIPTION.name() + "=in=(1,2)", 2);
        assertRSQLQueryDistributionSet(TagFields.DESCRIPTION.name() + "=out=(1,2)", 3);
    }

    /**
     * Test filter distribution set by colour
     */
    @Test
    void testFilterDistributionSetTagByParameterColour() {
        assertRSQLQueryDistributionSet(TagFields.COLOUR.name() + "==''", 0);
        assertRSQLQueryDistributionSet(TagFields.COLOUR.name() + "!=''", 5);
        assertRSQLQueryDistributionSet(TagFields.COLOUR.name() + "==red", 3);
        assertRSQLQueryDistributionSet(TagFields.COLOUR.name() + "!=red", 2);
        assertRSQLQueryDistributionSet(TagFields.COLOUR.name() + "==r*", 3);
        assertRSQLQueryDistributionSet(TagFields.COLOUR.name() + "==noExist*", 0);
        assertRSQLQueryDistributionSet(TagFields.COLOUR.name() + "=in=(red,notexist)", 3);
        assertRSQLQueryDistributionSet(TagFields.COLOUR.name() + "=out=(red,notexist)", 2);
    }

    private void assertRSQLQueryDistributionSet(final String rsql, final long expectedEntities) {
        final Page<? extends DistributionSetTag> findEntity = distributionSetTagManagement.findByRsql(rsql, PageRequest.of(0, 100));
        final long countAllEntities = findEntity.getTotalElements();
        assertThat(findEntity).isNotNull();
        assertThat(countAllEntities).isEqualTo(expectedEntities);
    }

    private void assertRSQLQueryTarget(final String rsql, final long expectedEntities) {
        final Page<? extends TargetTag> findEntity = targetTagManagement.findByRsql(rsql, PageRequest.of(0, 100));
        final long countAllEntities = findEntity.getTotalElements();
        assertThat(findEntity).isNotNull();
        assertThat(countAllEntities).isEqualTo(expectedEntities);
    }
}