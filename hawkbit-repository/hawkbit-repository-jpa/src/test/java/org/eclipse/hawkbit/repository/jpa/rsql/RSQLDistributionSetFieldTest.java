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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.Collections;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.repository.DistributionSetFields;
import org.eclipse.hawkbit.repository.SoftwareModuleFields;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.jpa.vendor.Database;

@Feature("Component Tests - Repository")
@Story("RSQL filter distribution set")
public class RSQLDistributionSetFieldTest extends AbstractJpaIntegrationTest {

    private DistributionSet ds;
    private SoftwareModule sm;

    @BeforeEach
    public void setupBeforeTest() {

        sm = testdataFactory.createSoftwareModuleApp("SM");

        ds = testdataFactory.createDistributionSet(Collections.singletonList(sm), "DS");
        ds = distributionSetManagement.update(entityFactory.distributionSet().update(ds.getId()).description("DS"));
        createDistributionSetMetadata(ds.getId(), entityFactory.generateDsMetadata("metaKey", "metaValue"));

        DistributionSet ds2 = testdataFactory.createDistributionSets("NewDS", 3).get(0);

        ds2 = distributionSetManagement.update(entityFactory.distributionSet().update(ds2.getId()).description("DS%"));
        createDistributionSetMetadata(ds2.getId(), entityFactory.generateDsMetadata("metaKey", "value"));

        final DistributionSetTag distSetTag = distributionSetTagManagement
                .create(entityFactory.tag().create().name("Tag1"));
        distributionSetTagManagement.create(entityFactory.tag().create().name("Tag2"));
        distributionSetTagManagement.create(entityFactory.tag().create().name("Tag3"));
        distributionSetTagManagement.create(entityFactory.tag().create().name("Tag4"));

        distributionSetManagement.assignTag(Arrays.asList(ds.getId(), ds2.getId()), distSetTag.getId());

        final DistributionSet ds3 = distributionSetManagement
                .create(entityFactory.distributionSet().create().name("test123").version("noDescription"));
        distributionSetManagement.invalidate(ds3);
    }

    @Test
    @Description("Test filter distribution set by id")
    public void testFilterByParameterId() {
        assertRSQLQuery(DistributionSetFields.ID.name() + "==" + ds.getId(), 1);
        assertRSQLQuery(DistributionSetFields.ID.name() + "!=" + ds.getId(), 4);
        assertRSQLQuery(DistributionSetFields.ID.name() + "==" + -1, 0);
        assertRSQLQuery(DistributionSetFields.ID.name() + "!=" + -1, 5);

        // Not supported for numbers
        if (Database.POSTGRESQL.equals(getDatabase())) {
            return;
        }

        assertRSQLQuery(DistributionSetFields.ID.name() + "=in=(" + ds.getId() + ",10000000)", 1);
        assertRSQLQuery(DistributionSetFields.ID.name() + "=out=(" + ds.getId() + ",10000000)", 4);
    }

    @Test
    @Description("Test filter distribution set by name")
    public void testFilterByParameterName() {
        assertRSQLQuery(DistributionSetFields.NAME.name() + "==DS", 1);
        assertRSQLQuery(DistributionSetFields.NAME.name() + "!=DS", 4);
        assertRSQLQuery(DistributionSetFields.NAME.name() + "==*DS", 4);
        assertRSQLQuery(DistributionSetFields.NAME.name() + "==noExist*", 0);
        assertRSQLQuery(DistributionSetFields.NAME.name() + "=in=(DS,notexist)", 1);
        assertRSQLQuery(DistributionSetFields.NAME.name() + "=out=(DS,notexist)", 4);
    }

    @Test
    @Description("Test filter distribution set by assigned software module")
    public void testFilterBySoftwareModule() {
        assertRSQLQuery(DistributionSetFields.MODULE.name() + "." + SoftwareModuleFields.NAME.name() + "==" + sm.getName(), 1);
        assertRSQLQuery(DistributionSetFields.MODULE.name() + "." + SoftwareModuleFields.ID.name() + "==" + sm.getId(), 1);
        assertRSQLQuery(DistributionSetFields.MODULE.name() + "." + SoftwareModuleFields.NAME.name() + "==noExist", 0);
        assertRSQLQuery(DistributionSetFields.MODULE.name() + "." + SoftwareModuleFields.ID.name() + "=in=(" + sm.getId() + ", -1)", 1);
        assertRSQLQuery(DistributionSetFields.MODULE.name() + "." + SoftwareModuleFields.ID.name() + "=out=(" + sm.getId() + ", -1)", 4);
    }

    @Test
    @Description("Test filter distribution set by description")
    public void testFilterByParameterDescription() {
        assertRSQLQuery(DistributionSetFields.DESCRIPTION.name() + "==''", 1);
        assertRSQLQuery(DistributionSetFields.DESCRIPTION.name() + "!=''", 4);
        assertRSQLQuery(DistributionSetFields.DESCRIPTION.name() + "==DS", 1);
        assertRSQLQuery(DistributionSetFields.DESCRIPTION.name() + "!=DS*", 3);
        assertRSQLQuery(DistributionSetFields.DESCRIPTION.name() + "!=DS", 4);
        assertRSQLQuery(DistributionSetFields.DESCRIPTION.name() + "==DS*", 2);
        assertRSQLQuery(DistributionSetFields.DESCRIPTION.name() + "==DS%", 1);
        assertRSQLQuery(DistributionSetFields.DESCRIPTION.name() + "==noExist*", 0);
        assertRSQLQuery(DistributionSetFields.DESCRIPTION.name() + "=in=(DS,notexist)", 1);
        assertRSQLQuery(DistributionSetFields.DESCRIPTION.name() + "=out=(DS,notexist)", 4);
    }

    @Test
    @Description("Test filter distribution set by version")
    public void testFilterByParameterVersion() {
        assertRSQLQuery(DistributionSetFields.VERSION.name() + "==" + TestdataFactory.DEFAULT_VERSION, 1);
        assertRSQLQuery(DistributionSetFields.VERSION.name() + "!=" + TestdataFactory.DEFAULT_VERSION, 4);
        assertRSQLQuery(DistributionSetFields.VERSION.name() + "=in=(" + TestdataFactory.DEFAULT_VERSION + ",1.0.0,1.0.1)", 3);
        assertRSQLQuery(DistributionSetFields.VERSION.name() + "=out=(" + TestdataFactory.DEFAULT_VERSION + ",error)", 4);
    }

    @Test
    @Description("Test filter distribution set by complete property")
    public void testFilterByAttributeComplete() {
        assertRSQLQuery(DistributionSetFields.COMPLETE.name() + "==true", 3);
        try {
            assertRSQLQuery(DistributionSetFields.COMPLETE.name() + "==noExist*", 0);
            fail("Expected RSQLParameterSyntaxException");
        } catch (final RSQLParameterSyntaxException e) {
        }
        assertRSQLQuery(DistributionSetFields.COMPLETE.name() + "=in=(true)", 3);
        assertRSQLQuery(DistributionSetFields.COMPLETE.name() + "=out=(true)", 2);
    }

    @Test
    @Description("Test filter distribution set by valid property")
    public void testFilterByAttributeValid() {
        assertRSQLQuery(DistributionSetFields.VALID.name() + "==true", 4);
        assertRSQLQuery(DistributionSetFields.VALID.name() + "==false", 1);
        assertThatExceptionOfType(RSQLParameterSyntaxException.class)
                .isThrownBy(() -> assertRSQLQuery(DistributionSetFields.VALID.name() + "==noExist*", 0));
        assertRSQLQuery(DistributionSetFields.VALID.name() + "=in=(true)", 4);
        assertRSQLQuery(DistributionSetFields.VALID.name() + "=out=(true)", 1);
    }

    @Test
    @Description("Test filter distribution set by tag name")
    public void testFilterByTag() {
        assertRSQLQuery(DistributionSetFields.TAG.name() + "==Tag1", 2);
        assertRSQLQuery(DistributionSetFields.TAG.name() + "!=Tag1", 3);
        assertRSQLQuery(DistributionSetFields.TAG.name() + "==T*", 2);
        assertRSQLQuery(DistributionSetFields.TAG.name() + "==noExist*", 0);
        assertRSQLQuery(DistributionSetFields.TAG.name() + "=in=(Tag1,notexist)", 2);
        assertRSQLQuery(DistributionSetFields.TAG.name() + "=out=(null)", 5);
    }

    @Test
    @Description("Test filter distribution set by type key")
    public void testFilterByType() {
        assertRSQLQuery(DistributionSetFields.TYPE.name() + "==" + TestdataFactory.DS_TYPE_DEFAULT, 4);
        assertRSQLQuery(DistributionSetFields.TYPE.name() + "==noExist*", 0);
        assertRSQLQuery(DistributionSetFields.TYPE.name() + "=in=(" + TestdataFactory.DS_TYPE_DEFAULT + ",ecl)", 4);
        assertRSQLQuery(DistributionSetFields.TYPE.name() + "=out=(" + TestdataFactory.DS_TYPE_DEFAULT + ")", 1);
    }

    @Test
    @Description("Test filter distribution set by metadata")
    public void testFilterByMetadata() {
        createDistributionSetWithMetadata("key.dot", "value.dot");

        assertRSQLQuery(DistributionSetFields.METADATA.name() + ".metaKey==metaValue", 1);
        assertRSQLQuery(DistributionSetFields.METADATA.name() + ".metaKey==*v*", 2);
        assertRSQLQuery(DistributionSetFields.METADATA.name() + ".metaKey==noExist*", 0);
        assertRSQLQuery(DistributionSetFields.METADATA.name() + ".metaKey=in=(metaValue,notexist)", 1);
        assertRSQLQuery(DistributionSetFields.METADATA.name() + ".metaKey=out=(metaValue,notexist)", 1);
        assertRSQLQuery(DistributionSetFields.METADATA.name() + ".notExist==metaValue", 0);
        assertRSQLQuery(DistributionSetFields.METADATA.name() + ".key.dot==value.dot", 1);
        assertRSQLQuery(DistributionSetFields.METADATA.name() + ".key.dot*==value.dot", 0);
        assertRSQLQuery(DistributionSetFields.METADATA.name() + ".key.*==value.dot", 0);
        assertRSQLQuery(DistributionSetFields.METADATA.name() + ".key.==value.dot", 0);
        assertRSQLQuery(DistributionSetFields.METADATA.name() + ".key*==value.dot", 0);
        assertRSQLQuery(DistributionSetFields.METADATA.name() + ".*==value.dot", 0);
        assertRSQLQuery(DistributionSetFields.METADATA.name() + "..==value.dot", 0);
        assertRSQLQueryThrowsException(DistributionSetFields.METADATA.name() + ".==value.dot",
                RSQLParameterUnsupportedFieldException.class);
        assertRSQLQueryThrowsException(DistributionSetFields.METADATA.name() + "*==value.dot",
                RSQLParameterUnsupportedFieldException.class);
        assertRSQLQueryThrowsException(DistributionSetFields.METADATA.name() + "==value.dot",
                RSQLParameterUnsupportedFieldException.class);

    }

    private void assertRSQLQuery(final String rsqlParam, final long expectedEntity) {
        final Page<DistributionSet> find = distributionSetManagement.findByRsql(rsqlParam, PageRequest.of(0, 100));
        final long countAll = find.getTotalElements();
        assertThat(find).as("Found entity is should not be null").isNotNull();
        assertThat(countAll).as("Found entity size is wrong").isEqualTo(expectedEntity);
    }

    private <T extends Throwable> void assertRSQLQueryThrowsException(final String rsqlParam,
            final Class<T> expectedException) {
        assertThatExceptionOfType(expectedException)
                .isThrownBy(() -> RSQLUtility.validateRsqlFor(rsqlParam, DistributionSetFields.class));
    }

    private DistributionSet createDistributionSetWithMetadata(final String metadataKeyName,
            final String metadataValue) {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();
        createDistributionSetMetadata(distributionSet.getId(),
                entityFactory.generateDsMetadata(metadataKeyName, metadataValue));
        return distributionSet;
    }

}
