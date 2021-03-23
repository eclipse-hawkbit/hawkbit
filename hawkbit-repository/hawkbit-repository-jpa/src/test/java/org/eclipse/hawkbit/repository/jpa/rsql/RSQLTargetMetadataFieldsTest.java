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

import org.eclipse.hawkbit.repository.TargetMetadataFields;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Component Tests - Repository")
@Story("RSQL filter target metadata")
public class RSQLTargetMetadataFieldsTest extends AbstractJpaIntegrationTest {
    private String controllerId;

    @BeforeEach
    public void setupBeforeTest() {
        final Target target = testdataFactory.createTarget("target");
        controllerId = target.getControllerId();

        final List<MetaData> metadata = new ArrayList<>(5);
        for (int i = 0; i < 5; i++) {
            metadata.add(entityFactory.generateTargetMetadata("" + i, "" + i));
        }

        targetManagement.createMetaData(controllerId, metadata);

        targetManagement.createMetaData(controllerId,
                Arrays.asList(entityFactory.generateTargetMetadata("emptyValueTest", null)));
    }

    @Test
    @Description("Test filter target metadata by key")
    public void testFilterByParameterKey() {
        assertRSQLQuery(TargetMetadataFields.KEY.name() + "==1", 1);
        assertRSQLQuery(TargetMetadataFields.KEY.name() + "!=1", 5);
        assertRSQLQuery(TargetMetadataFields.KEY.name() + "=in=(1,2)", 2);
        assertRSQLQuery(TargetMetadataFields.KEY.name() + "=out=(1,2)", 4);
    }

    @Test
    @Description("Test filter target metadata by value")
    public void testFilterByParameterValue() {
        assertRSQLQuery(TargetMetadataFields.VALUE.name() + "==''", 1);
        assertRSQLQuery(TargetMetadataFields.VALUE.name() + "!=''", 5);
        assertRSQLQuery(TargetMetadataFields.VALUE.name() + "==1", 1);
        assertRSQLQuery(TargetMetadataFields.VALUE.name() + "!=1", 5);
        assertRSQLQuery(TargetMetadataFields.VALUE.name() + "=in=(1,2)", 2);
        assertRSQLQuery(TargetMetadataFields.VALUE.name() + "=out=(1,2)", 4);
    }

    private void assertRSQLQuery(final String rsqlParam, final long expectedEntities) {

        final Page<TargetMetadata> findEnitity = targetManagement
                .findMetaDataByControllerIdAndRsql(PageRequest.of(0, 100), controllerId, rsqlParam);
        final long countAllEntities = findEnitity.getTotalElements();
        assertThat(findEnitity).isNotNull();
        assertThat(countAllEntities).isEqualTo(expectedEntities);
    }
}
