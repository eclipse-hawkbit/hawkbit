/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.hawkbit.repository.TagFields;
import org.eclipse.hawkbit.repository.builder.TagCreate;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Component Tests - Repository")
@Story("RSQL filter target and distribution set tags")
public class RSQLTagFieldsTest extends AbstractJpaIntegrationTest {

    @BeforeEach
    public void seuptBeforeTest() {

        for (int i = 0; i < 5; i++) {
            final TagCreate targetTag = entityFactory.tag().create().name(Integer.toString(i))
                    .description(Integer.toString(i)).colour(i % 2 == 0 ? "red" : "blue");
            targetTagManagement.create(targetTag);
            distributionSetTagManagement.create(targetTag);
        }
    }

    @Test
    @Description("Test filter target tag by name")
    public void testFilterTargetTagByParameterName() {
        assertRSQLQueryTarget(TagFields.NAME.name() + "==''", 0);
        assertRSQLQueryTarget(TagFields.NAME.name() + "!=''", 5);
        assertRSQLQueryTarget(TagFields.NAME.name() + "==1", 1);
        assertRSQLQueryTarget(TagFields.NAME.name() + "!=1", 4);
        assertRSQLQueryTarget(TagFields.NAME.name() + "==*", 5);
        assertRSQLQueryTarget(TagFields.NAME.name() + "==noExist*", 0);
        assertRSQLQueryTarget(TagFields.NAME.name() + "=in=(1,notexist)", 1);
        assertRSQLQueryTarget(TagFields.NAME.name() + "=out=(1,notexist)", 4);
    }

    @Test
    @Description("Test filter target tag by description")
    public void testFilterTargetTagByParameterDescription() {
        assertRSQLQueryTarget(TagFields.DESCRIPTION.name() + "==''", 0);
        assertRSQLQueryTarget(TagFields.DESCRIPTION.name() + "!=''", 5);
        assertRSQLQueryTarget(TagFields.DESCRIPTION.name() + "==1", 1);
        assertRSQLQueryTarget(TagFields.DESCRIPTION.name() + "!=1", 4);
        assertRSQLQueryTarget(TagFields.DESCRIPTION.name() + "==*", 5);
        assertRSQLQueryTarget(TagFields.DESCRIPTION.name() + "==noExist*", 0);
        assertRSQLQueryTarget(TagFields.DESCRIPTION.name() + "=in=(1,notexist)", 1);
        assertRSQLQueryTarget(TagFields.DESCRIPTION.name() + "=out=(1,notexist)", 4);
    }

    @Test
    @Description("Test filter target tag by colour")
    public void testFilterTargetTagByParameterColour() {
        assertRSQLQueryTarget(TagFields.COLOUR.name() + "==''", 0);
        assertRSQLQueryTarget(TagFields.COLOUR.name() + "!=''", 5);
        assertRSQLQueryTarget(TagFields.COLOUR.name() + "==red", 3);
        assertRSQLQueryTarget(TagFields.COLOUR.name() + "!=red", 2);
        assertRSQLQueryTarget(TagFields.COLOUR.name() + "==r*", 3);
        assertRSQLQueryTarget(TagFields.COLOUR.name() + "==noExist*", 0);
        assertRSQLQueryTarget(TagFields.COLOUR.name() + "=in=(red,notexist)", 3);
        assertRSQLQueryTarget(TagFields.COLOUR.name() + "=out=(red,notexist)", 2);
    }

    @Test
    @Description("Test filter distribution set tag by name")
    public void testFilterDistributionSetTagByParameterName() {
        assertRSQLQueryDistributionSet(TagFields.NAME.name() + "==''", 0);
        assertRSQLQueryDistributionSet(TagFields.NAME.name() + "!=''", 5);
        assertRSQLQueryDistributionSet(TagFields.NAME.name() + "==1", 1);
        assertRSQLQueryDistributionSet(TagFields.NAME.name() + "!=1", 4);
        assertRSQLQueryDistributionSet(TagFields.NAME.name() + "==*", 5);
        assertRSQLQueryDistributionSet(TagFields.NAME.name() + "==noExist*", 0);
        assertRSQLQueryDistributionSet(TagFields.NAME.name() + "=in=(1,2)", 2);
        assertRSQLQueryDistributionSet(TagFields.NAME.name() + "=out=(1,2)", 3);
    }

    @Test
    @Description("Test filter distribution set by description")
    public void testFilterDistributionSetTagByParameterDescription() {
        assertRSQLQueryDistributionSet(TagFields.DESCRIPTION.name() + "==''", 0);
        assertRSQLQueryDistributionSet(TagFields.DESCRIPTION.name() + "!=''", 5);
        assertRSQLQueryDistributionSet(TagFields.DESCRIPTION.name() + "==1", 1);
        assertRSQLQueryDistributionSet(TagFields.DESCRIPTION.name() + "!=1", 4);
        assertRSQLQueryDistributionSet(TagFields.DESCRIPTION.name() + "==*", 5);
        assertRSQLQueryDistributionSet(TagFields.DESCRIPTION.name() + "==noExist*", 0);
        assertRSQLQueryDistributionSet(TagFields.DESCRIPTION.name() + "=in=(1,2)", 2);
        assertRSQLQueryDistributionSet(TagFields.DESCRIPTION.name() + "=out=(1,2)", 3);
    }

    @Test
    @Description("Test filter distribution set by colour")
    public void testFilterDistributionSetTagByParameterColour() {
        assertRSQLQueryDistributionSet(TagFields.COLOUR.name() + "==''", 0);
        assertRSQLQueryDistributionSet(TagFields.COLOUR.name() + "!=''", 5);
        assertRSQLQueryDistributionSet(TagFields.COLOUR.name() + "==red", 3);
        assertRSQLQueryDistributionSet(TagFields.COLOUR.name() + "!=red", 2);
        assertRSQLQueryDistributionSet(TagFields.COLOUR.name() + "==r*", 3);
        assertRSQLQueryDistributionSet(TagFields.COLOUR.name() + "==noExist*", 0);
        assertRSQLQueryDistributionSet(TagFields.COLOUR.name() + "=in=(red,notexist)", 3);
        assertRSQLQueryDistributionSet(TagFields.COLOUR.name() + "=out=(red,notexist)", 2);
    }

    private void assertRSQLQueryDistributionSet(final String rsqlParam, final long expectedEntities) {

        final Page<DistributionSetTag> findEnitity = distributionSetTagManagement.findByRsql(PageRequest.of(0, 100),
                rsqlParam);
        final long countAllEntities = findEnitity.getTotalElements();
        assertThat(findEnitity).isNotNull();
        assertThat(countAllEntities).isEqualTo(expectedEntities);
    }

    private void assertRSQLQueryTarget(final String rsqlParam, final long expectedEntities) {

        final Page<TargetTag> findEnitity = targetTagManagement.findByRsql(PageRequest.of(0, 100), rsqlParam);
        final long countAllEntities = findEnitity.getTotalElements();
        assertThat(findEnitity).isNotNull();
        assertThat(countAllEntities).isEqualTo(expectedEntities);
    }
}
