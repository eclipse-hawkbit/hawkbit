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
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.eclipse.hawkbit.repository.DistributionSetFields;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Repository")
@Stories("RSQL filter distribution set")
public class RSQLDistributionSetFieldTest extends AbstractJpaIntegrationTest {

    @Before
    public void seuptBeforeTest() {

        DistributionSet ds = testdataFactory.createDistributionSet("DS");
        ds = distributionSetManagement.update(entityFactory.distributionSet().update(ds.getId()).description("DS"));
        createDistributionSetMetadata(ds.getId(), entityFactory.generateMetadata("metaKey", "metaValue"));

        DistributionSet ds2 = testdataFactory.createDistributionSets("NewDS", 3).get(0);

        ds2 = distributionSetManagement.update(entityFactory.distributionSet().update(ds2.getId()).description("DS%"));
        createDistributionSetMetadata(ds2.getId(), entityFactory.generateMetadata("metaKey", "value"));

        final DistributionSetTag targetTag = distributionSetTagManagement
                .create(entityFactory.tag().create().name("Tag1"));
        distributionSetTagManagement.create(entityFactory.tag().create().name("Tag2"));
        distributionSetTagManagement.create(entityFactory.tag().create().name("Tag3"));
        distributionSetTagManagement.create(entityFactory.tag().create().name("Tag4"));

        distributionSetManagement.assignTag(Arrays.asList(ds.getId(), ds2.getId()), targetTag.getId());
    }

    @Test
    @Description("Test filter distribution set by id")
    public void testFilterByParameterId() {
        assertRSQLQuery(DistributionSetFields.ID.name() + "==*", 4);
    }

    @Test
    @Description("Test filter distribution set by name")
    public void testFilterByParameterName() {
        assertRSQLQuery(DistributionSetFields.NAME.name() + "==DS", 1);
        assertRSQLQuery(DistributionSetFields.NAME.name() + "==*DS", 4);
        assertRSQLQuery(DistributionSetFields.NAME.name() + "==noExist*", 0);
        assertRSQLQuery(DistributionSetFields.NAME.name() + "=in=(DS,notexist)", 1);
        assertRSQLQuery(DistributionSetFields.NAME.name() + "=out=(DS,notexist)", 3);
    }

    @Test
    @Description("Test filter distribution set by description")
    public void testFilterByParameterDescription() {
        assertRSQLQuery(DistributionSetFields.DESCRIPTION.name() + "==DS", 1);
        assertRSQLQuery(DistributionSetFields.DESCRIPTION.name() + "==DS*", 2);
        assertRSQLQuery(DistributionSetFields.DESCRIPTION.name() + "==DS%", 1);
        assertRSQLQuery(DistributionSetFields.DESCRIPTION.name() + "==noExist*", 0);
        assertRSQLQuery(DistributionSetFields.DESCRIPTION.name() + "=in=(DS,notexist)", 1);
        assertRSQLQuery(DistributionSetFields.DESCRIPTION.name() + "=out=(DS,notexist)", 3);
    }

    @Test
    @Description("Test filter distribution set by version")
    public void testFilterByParameterVersion() {
        assertRSQLQuery(DistributionSetFields.VERSION.name() + "==" + TestdataFactory.DEFAULT_VERSION, 1);
        assertRSQLQuery(DistributionSetFields.VERSION.name() + "!=" + TestdataFactory.DEFAULT_VERSION, 3);
        assertRSQLQuery(
                DistributionSetFields.VERSION.name() + "=in=(" + TestdataFactory.DEFAULT_VERSION + ",1.0.0,1.0.1)", 3);
        assertRSQLQuery(DistributionSetFields.VERSION.name() + "=out=(" + TestdataFactory.DEFAULT_VERSION + ",error)",
                3);
    }

    @Test
    @Description("Test filter distribution set by complete property")
    public void testFilterByAttribute() {
        assertRSQLQuery(DistributionSetFields.COMPLETE.name() + "==true", 4);
        try {
            assertRSQLQuery(DistributionSetFields.COMPLETE.name() + "==noExist*", 0);
            fail("Expected RSQLParameterSyntaxException");
        } catch (final RSQLParameterSyntaxException e) {
        }
        assertRSQLQuery(DistributionSetFields.COMPLETE.name() + "=in=(true)", 4);
        assertRSQLQuery(DistributionSetFields.COMPLETE.name() + "=out=(true)", 0);
    }

    @Test
    @Description("Test filter distribution set by tag")
    public void testFilterByTag() {
        assertRSQLQuery(DistributionSetFields.TAG.name() + "==Tag1", 2);
        assertRSQLQuery(DistributionSetFields.TAG.name() + "==T*", 2);
        assertRSQLQuery(DistributionSetFields.TAG.name() + "==noExist*", 0);
        assertRSQLQuery(DistributionSetFields.TAG.name() + "=in=(Tag1,notexist)", 2);
        assertRSQLQuery(DistributionSetFields.TAG.name() + "=out=(Tag1,notexist)", 0);
    }

    @Test
    @Description("Test filter distribution set by type")
    public void testFilterByType() {
        assertRSQLQuery(DistributionSetFields.TYPE.name() + "==" + TestdataFactory.DS_TYPE_DEFAULT, 4);
        assertRSQLQuery(DistributionSetFields.TYPE.name() + "==noExist*", 0);
        assertRSQLQuery(DistributionSetFields.TYPE.name() + "=in=(" + TestdataFactory.DS_TYPE_DEFAULT + ",ecl)", 4);
        assertRSQLQuery(DistributionSetFields.TYPE.name() + "=out=(" + TestdataFactory.DS_TYPE_DEFAULT + ")", 0);
    }

    @Test
    @Description("")
    public void testFilterByMetadata() {
        assertRSQLQuery(DistributionSetFields.METADATA.name() + ".metaKey==metaValue", 1);
        assertRSQLQuery(DistributionSetFields.METADATA.name() + ".metaKey==*v*", 2);
        assertRSQLQuery(DistributionSetFields.METADATA.name() + ".metaKey==noExist*", 0);
        assertRSQLQuery(DistributionSetFields.METADATA.name() + ".metaKey=in=(metaValue,notexist)", 1);
        assertRSQLQuery(DistributionSetFields.METADATA.name() + ".metaKey=out=(metaValue,notexist)", 1);
        assertRSQLQuery(DistributionSetFields.METADATA.name() + ".notExist==metaValue", 0);

    }

    private void assertRSQLQuery(final String rsqlParam, final long excpectedEntity) {
        final Page<DistributionSet> find = distributionSetManagement.findByRsql(new PageRequest(0, 100), rsqlParam);
        final long countAll = find.getTotalElements();
        assertThat(find).as("Founded entity is should not be null").isNotNull();
        assertThat(countAll).as("Founded entity size is wrong").isEqualTo(excpectedEntity);
    }
}
