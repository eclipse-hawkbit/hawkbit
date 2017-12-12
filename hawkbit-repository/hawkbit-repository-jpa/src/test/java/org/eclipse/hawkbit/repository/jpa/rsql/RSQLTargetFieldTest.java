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
import java.util.HashMap;
import java.util.Map;

import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Page;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Repository")
@Stories("RSQL filter target")
public class RSQLTargetFieldTest extends AbstractJpaIntegrationTest {

    private Target target;
    private Target target2;

    @Before
    public void setupBeforeTest() throws InterruptedException {

        final DistributionSet ds = testdataFactory.createDistributionSet("AssignedDs");

        final Map<String, String> attributes = new HashMap<>();

        target = targetManagement.create(entityFactory.target().create().controllerId("targetId123")
                .name("targetName123").description("targetDesc123"));
        attributes.put("revision", "1.1");
        target = controllerManagement.updateControllerAttributes(target.getControllerId(), attributes);
        target = controllerManagement.findOrRegisterTargetIfItDoesNotexist(target.getControllerId(), LOCALHOST);

        target2 = targetManagement
                .create(entityFactory.target().create().controllerId("targetId1234").description("targetId1234"));
        attributes.put("revision", "1.2");
        Thread.sleep(1);
        target2 = controllerManagement.updateControllerAttributes(target2.getControllerId(), attributes);
        target2 = controllerManagement.findOrRegisterTargetIfItDoesNotexist(target2.getControllerId(), LOCALHOST);

        testdataFactory.createTarget("targetId1235");
        testdataFactory.createTarget("targetId1236");

        final TargetTag targetTag = targetTagManagement.create(entityFactory.tag().create().name("Tag1"));
        targetTagManagement.create(entityFactory.tag().create().name("Tag2"));
        targetTagManagement.create(entityFactory.tag().create().name("Tag3"));
        targetTagManagement.create(entityFactory.tag().create().name("Tag4"));

        targetManagement.assignTag(Arrays.asList(target.getControllerId(), target2.getControllerId()),
                targetTag.getId());

        assignDistributionSet(ds.getId(), target.getControllerId());
    }

    @Test
    @Description("Test filter target by (controller) id")
    public void testFilterByParameterId() {
        assertRSQLQuery(TargetFields.ID.name() + "==targetId123", 1);
        assertRSQLQuery(TargetFields.ID.name() + "==target*", 4);
        assertRSQLQuery(TargetFields.ID.name() + "==noExist*", 0);
        assertRSQLQuery(TargetFields.ID.name() + "=in=(targetId123,notexist)", 1);
        assertRSQLQuery(TargetFields.ID.name() + "=out=(targetId123,notexist)", 3);
    }

    @Test
    @Description("Test filter target by name")
    public void testFilterByParameterName() {
        assertRSQLQuery(TargetFields.NAME.name() + "==targetName123", 1);
        assertRSQLQuery(TargetFields.NAME.name() + "==target*", 4);
        assertRSQLQuery(TargetFields.NAME.name() + "==noExist*", 0);
        assertRSQLQuery(TargetFields.NAME.name() + "=in=(targetName123,notexist)", 1);
        assertRSQLQuery(TargetFields.NAME.name() + "=out=(targetName123,notexist)", 3);
    }

    @Test
    @Description("Test filter target by description")
    public void testFilterByParameterDescription() {
        assertRSQLQuery(TargetFields.DESCRIPTION.name() + "==targetDesc123", 1);
        assertRSQLQuery(TargetFields.DESCRIPTION.name() + "==target*", 2);
        assertRSQLQuery(TargetFields.DESCRIPTION.name() + "==noExist*", 0);
        assertRSQLQuery(TargetFields.DESCRIPTION.name() + "=in=(targetDesc123,notexist)", 1);
        assertRSQLQuery(TargetFields.DESCRIPTION.name() + "=out=(targetDesc123,notexist)", 1);
    }

    @Test
    @Description("Test filter target by controller id")
    public void testFilterByParameterControllerId() {
        assertRSQLQuery(TargetFields.CONTROLLERID.name() + "==targetId123", 1);
        assertRSQLQuery(TargetFields.CONTROLLERID.name() + "==target*", 4);
        assertRSQLQuery(TargetFields.CONTROLLERID.name() + "==noExist*", 0);
        assertRSQLQuery(TargetFields.CONTROLLERID.name() + "=in=(targetId123,notexist)", 1);
        assertRSQLQuery(TargetFields.CONTROLLERID.name() + "=out=(targetId123,notexist)", 3);
    }

    @Test
    @Description("Test filter target by status")
    public void testFilterByParameterUpdateStatus() {
        assertRSQLQuery(TargetFields.UPDATESTATUS.name() + "==pending", 1);
        assertRSQLQuery(TargetFields.UPDATESTATUS.name() + "!=pending", 3);
        try {
            assertRSQLQuery(TargetFields.UPDATESTATUS.name() + "==noExist*", 0);
            fail("RSQLParameterUnsupportedFieldException was expected since update status unknown");
        } catch (final RSQLParameterUnsupportedFieldException e) {
            // test ok - exception was excepted
        }
        assertRSQLQuery(TargetFields.UPDATESTATUS.name() + "=in=(pending,error)", 1);
        assertRSQLQuery(TargetFields.UPDATESTATUS.name() + "=out=(pending,error)", 3);
    }

    @Test
    @Description("Test filter target by attribute")
    public void testFilterByAttribute() {
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".revision==1.1", 1);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".revision==1*", 2);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".revision==noExist*", 0);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".revision=in=(1.1,notexist)", 1);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".revision=out=(1.1)", 1);
    }

    @Test
    @Description("Test filter target by assigned ds name")
    public void testFilterByAssignedDsName() {
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".name==AssignedDs", 1);
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".name==A*", 1);
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".name==noExist*", 0);
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".name=in=(AssignedDs,notexist)", 1);
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".name=out=(AssignedDs,notexist)", 0);
    }

    @Test
    @Description("Test filter target by assigned ds version")
    public void testFilterByAssignedDsVersion() {
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".version==" + TestdataFactory.DEFAULT_VERSION, 1);
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".version==*1*", 1);
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".version==noExist*", 0);
        assertRSQLQuery(
                TargetFields.ASSIGNEDDS.name() + ".version=in=(" + TestdataFactory.DEFAULT_VERSION + ",notexist)", 1);
        assertRSQLQuery(
                TargetFields.ASSIGNEDDS.name() + ".version=out=(" + TestdataFactory.DEFAULT_VERSION + ",notexist)", 0);
    }

    @Test
    @Description("Test filter target by tag")
    public void testFilterByTag() {
        assertRSQLQuery(TargetFields.TAG.name() + "==Tag1", 2);
        assertRSQLQuery(TargetFields.TAG.name() + "==T*", 2);
        assertRSQLQuery(TargetFields.TAG.name() + "==noExist*", 0);
        assertRSQLQuery(TargetFields.TAG.name() + "=in=(Tag1,notexist)", 2);
        assertRSQLQuery(TargetFields.TAG.name() + "=out=(Tag1,notexist)", 0);
    }

    @Test
    @Description("Test filter target by lastTargetQuery")
    public void testFilterByLastTargetQuery() throws InterruptedException {
        assertRSQLQuery(TargetFields.LASTCONTROLLERREQUESTAT.name() + "==" + target.getLastTargetQuery(), 1);
        assertRSQLQuery(TargetFields.LASTCONTROLLERREQUESTAT.name() + "!=" + target.getLastTargetQuery(), 1);
        assertRSQLQuery(TargetFields.LASTCONTROLLERREQUESTAT.name() + "=lt=" + target.getLastTargetQuery(), 0);
        assertRSQLQuery(TargetFields.LASTCONTROLLERREQUESTAT.name() + "=lt=" + target2.getLastTargetQuery(), 1);
        assertRSQLQuery(TargetFields.LASTCONTROLLERREQUESTAT.name() + "=gt=" + target.getLastTargetQuery(), 1);
        assertRSQLQuery(TargetFields.LASTCONTROLLERREQUESTAT.name() + "=gt=" + target2.getLastTargetQuery(), 0);
        assertRSQLQuery(TargetFields.LASTCONTROLLERREQUESTAT.name() + "=le=${NOW_TS}", 2);
        assertRSQLQuery(TargetFields.LASTCONTROLLERREQUESTAT.name() + "=gt=${OVERDUE_TS}", 2);
    }

    private void assertRSQLQuery(final String rsqlParam, final long expcetedTargets) {
        final Page<Target> findTargetPage = targetManagement.findByRsql(PAGE, rsqlParam);
        final long countTargetsAll = findTargetPage.getTotalElements();
        assertThat(findTargetPage).isNotNull();
        assertThat(countTargetsAll).isEqualTo(expcetedTargets);
    }
}
