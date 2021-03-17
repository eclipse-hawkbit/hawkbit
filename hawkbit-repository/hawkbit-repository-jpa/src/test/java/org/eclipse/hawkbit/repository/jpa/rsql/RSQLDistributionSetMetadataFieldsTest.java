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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.hawkbit.repository.DistributionSetMetadataFields;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Component Tests - Repository")
@Story("RSQL filter distribution set metadata")
public class RSQLDistributionSetMetadataFieldsTest extends AbstractJpaIntegrationTest {

    private Long distributionSetId;

    @BeforeEach
    public void setupBeforeTest() {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("DS");
        distributionSetId = distributionSet.getId();

        final List<MetaData> metadata = new ArrayList<>(5);
        for (int i = 0; i < 5; i++) {
            metadata.add(entityFactory.generateDsMetadata("" + i, "" + i));
        }

        distributionSetManagement.createMetaData(distributionSetId, metadata);

        distributionSetManagement.createMetaData(distributionSetId,
                Arrays.asList(entityFactory.generateDsMetadata("emptyValueTest", null)));
    }

    @Test
    @Description("Test filter distribution set metadata by key")
    public void testFilterByParameterKey() {
        assertRSQLQuery(DistributionSetMetadataFields.KEY.name() + "==1", 1);
        assertRSQLQuery(DistributionSetMetadataFields.KEY.name() + "!=1", 5);
        assertRSQLQuery(DistributionSetMetadataFields.KEY.name() + "=in=(1,2)", 2);
        assertRSQLQuery(DistributionSetMetadataFields.KEY.name() + "=out=(1,2)", 4);
    }

    @Test
    @Description("Test filter distribution set metadata by value")
    public void testFilterByParameterValue() {
        assertRSQLQuery(DistributionSetMetadataFields.VALUE.name() + "==''", 1);
        assertRSQLQuery(DistributionSetMetadataFields.VALUE.name() + "!=''", 5);
        assertRSQLQuery(DistributionSetMetadataFields.VALUE.name() + "==1", 1);
        assertRSQLQuery(DistributionSetMetadataFields.VALUE.name() + "!=1", 5);
        assertRSQLQuery(DistributionSetMetadataFields.VALUE.name() + "=in=(1,2)", 2);
        assertRSQLQuery(DistributionSetMetadataFields.VALUE.name() + "=out=(1,2)", 4);
    }

    private void assertRSQLQuery(final String rsqlParam, final long expectedEntities) {

        final Page<DistributionSetMetadata> findEnitity = distributionSetManagement
                .findMetaDataByDistributionSetIdAndRsql(PageRequest.of(0, 100), distributionSetId, rsqlParam);
        final long countAllEntities = findEnitity.getTotalElements();
        assertThat(findEnitity).isNotNull();
        assertThat(countAllEntities).isEqualTo(expectedEntities);
    }
}
