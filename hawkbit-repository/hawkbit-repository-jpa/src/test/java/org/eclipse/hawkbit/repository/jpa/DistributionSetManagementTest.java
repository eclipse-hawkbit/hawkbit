/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.exception.DistributionSetTypeUndefinedException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityLockedException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.exception.UnsupportedSoftwareModuleForThisDistributionSetException;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetMetadata;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetTag;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter.DistributionSetFilterBuilder;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.fest.assertions.core.Condition;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.google.common.collect.Lists;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * {@link DistributionSetManagement} tests.
 *
 */
@Features("Component Tests - Repository")
@Stories("DistributionSet Management")
public class DistributionSetManagementTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Tests the successfull module update of unused distribution set type which is in fact allowed.")
    public void updateUnassignedDistributionSetTypeModules() {
        DistributionSetType updatableType = distributionSetManagement
                .createDistributionSetType(new JpaDistributionSetType("updatableType", "to be deleted", ""));
        assertThat(distributionSetManagement.findDistributionSetTypeByKey("updatableType").getMandatoryModuleTypes())
                .isEmpty();

        // add OS
        updatableType.addMandatoryModuleType(osType);
        updatableType = distributionSetManagement.updateDistributionSetType(updatableType);
        assertThat(distributionSetManagement.findDistributionSetTypeByKey("updatableType").getMandatoryModuleTypes())
                .containsOnly(osType);

        // add JVM
        updatableType.addMandatoryModuleType(runtimeType);
        updatableType = distributionSetManagement.updateDistributionSetType(updatableType);
        assertThat(distributionSetManagement.findDistributionSetTypeByKey("updatableType").getMandatoryModuleTypes())
                .containsOnly(osType, runtimeType);

        // remove OS
        updatableType.removeModuleType(osType.getId());
        updatableType = distributionSetManagement.updateDistributionSetType(updatableType);
        assertThat(distributionSetManagement.findDistributionSetTypeByKey("updatableType").getMandatoryModuleTypes())
                .containsOnly(runtimeType);
    }

    @Test
    @Description("Tests the successfull update of used distribution set type meta data hich is in fact allowed.")
    public void updateAssignedDistributionSetTypeMetaData() {
        final DistributionSetType nonUpdatableType = distributionSetManagement
                .createDistributionSetType(new JpaDistributionSetType("updatableType", "to be deletd", ""));
        assertThat(distributionSetManagement.findDistributionSetTypeByKey("updatableType").getMandatoryModuleTypes())
                .isEmpty();
        distributionSetManagement
                .createDistributionSet(new JpaDistributionSet("newtypesoft", "1", "", nonUpdatableType, null));

        nonUpdatableType.setDescription("a new description");
        nonUpdatableType.setColour("test123");

        distributionSetManagement.updateDistributionSetType(nonUpdatableType);

        assertThat(distributionSetManagement.findDistributionSetTypeByKey("updatableType").getDescription())
                .isEqualTo("a new description");
        assertThat(distributionSetManagement.findDistributionSetTypeByKey("updatableType").getColour())
                .isEqualTo("test123");
    }

    @Test
    @Description("Tests the unsuccessfull update of used distribution set type (module addition).")
    public void addModuleToAssignedDistributionSetTypeFails() {
        final DistributionSetType nonUpdatableType = distributionSetManagement
                .createDistributionSetType(new JpaDistributionSetType("updatableType", "to be deletd", ""));
        assertThat(distributionSetManagement.findDistributionSetTypeByKey("updatableType").getMandatoryModuleTypes())
                .isEmpty();
        distributionSetManagement
                .createDistributionSet(new JpaDistributionSet("newtypesoft", "1", "", nonUpdatableType, null));

        nonUpdatableType.addMandatoryModuleType(osType);

        try {
            distributionSetManagement.updateDistributionSetType(nonUpdatableType);
            fail("Should not have worked as DS is in use.");
        } catch (final EntityReadOnlyException e) {

        }

    }

    @Test
    @Description("Tests the unsuccessfull update of used distribution set type (module removal).")
    public void removeModuleToAssignedDistributionSetTypeFails() {
        DistributionSetType nonUpdatableType = distributionSetManagement
                .createDistributionSetType(new JpaDistributionSetType("updatableType", "to be deletd", ""));
        assertThat(distributionSetManagement.findDistributionSetTypeByKey("updatableType").getMandatoryModuleTypes())
                .isEmpty();

        nonUpdatableType.addMandatoryModuleType(osType);
        nonUpdatableType = distributionSetManagement.updateDistributionSetType(nonUpdatableType);
        distributionSetManagement
                .createDistributionSet(new JpaDistributionSet("newtypesoft", "1", "", nonUpdatableType, null));

        nonUpdatableType.removeModuleType(osType.getId());
        try {
            distributionSetManagement.updateDistributionSetType(nonUpdatableType);
            fail("Should not have worked as DS is in use.");
        } catch (final EntityReadOnlyException e) {

        }
    }

    @Test
    @Description("Tests the successfull deletion of unused (hard delete) distribution set types.")
    public void deleteUnassignedDistributionSetType() {
        final JpaDistributionSetType hardDelete = (JpaDistributionSetType) distributionSetManagement
                .createDistributionSetType(new JpaDistributionSetType("deleted", "to be deleted", ""));

        assertThat(distributionSetTypeRepository.findAll()).contains(hardDelete);
        distributionSetManagement.deleteDistributionSetType(hardDelete);

        assertThat(distributionSetTypeRepository.findAll()).doesNotContain(hardDelete);
    }

    @Test
    @Description("Tests the successfull deletion of used (soft delete) distribution set types.")
    public void deleteAssignedDistributionSetType() {
        final JpaDistributionSetType softDelete = (JpaDistributionSetType) distributionSetManagement
                .createDistributionSetType(new JpaDistributionSetType("softdeleted", "to be deletd", ""));

        assertThat(distributionSetTypeRepository.findAll()).contains(softDelete);
        distributionSetManagement
                .createDistributionSet(new JpaDistributionSet("newtypesoft", "1", "", softDelete, null));

        distributionSetManagement.deleteDistributionSetType(softDelete);
        assertThat(distributionSetManagement.findDistributionSetTypeByKey("softdeleted").isDeleted()).isEqualTo(true);
    }

    @Test
    @Description("Ensures that it is not possible to create a DS that already exists (unique constraint is on name,version for DS).")
    public void createDuplicateDistributionSetsFailsWithException() {
        testdataFactory.createDistributionSet("a");

        try {
            testdataFactory.createDistributionSet("a");
            fail("Should not have worked as DS with same UK already exists.");
        } catch (final EntityAlreadyExistsException e) {

        }
    }

    @Test
    @Description("Verfies that a DS is of default type if not specified explicitly at creation time.")
    public void createDistributionSetWithImplicitType() {
        final DistributionSet set = distributionSetManagement
                .createDistributionSet(new JpaDistributionSet("newtypesoft", "1", "", null, null));

        assertThat(set.getType()).as("Type should be equal to default type of tenant")
                .isEqualTo(systemManagement.getTenantMetadata().getDefaultDsType());

    }

    @Test
    @Description("Verfies that multiple DS are of default type if not specified explicitly at creation time.")
    public void createMultipleDistributionSetsWithImplicitType() {

        List<DistributionSet> sets = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            sets.add(new JpaDistributionSet("another DS" + i, "X" + i, "", null, null));
        }

        sets = distributionSetManagement.createDistributionSets(sets);

        assertThat(sets).as("Type should be equal to default type of tenant").are(new Condition<DistributionSet>() {
            @Override
            public boolean matches(final DistributionSet value) {
                return value.getType().equals(systemManagement.getTenantMetadata().getDefaultDsType());
            }
        });

    }

    @Test
    @Description("Verfies that a DS entity cannot be used for creation.")
    public void createDistributionSetFailsOnExistingEntity() {
        final DistributionSet set = distributionSetManagement
                .createDistributionSet(new JpaDistributionSet("newtypesoft", "1", "", null, null));

        try {
            distributionSetManagement.createDistributionSet(set);
            fail("Should not have worked to create based on a persisted entity.");
        } catch (final EntityAlreadyExistsException e) {

        }
    }

    @Test
    @Description("Checks that metadata for a distribution set can be created.")
    public void createDistributionSetMetadata() {
        final String knownKey = "dsMetaKnownKey";
        final String knownValue = "dsMetaKnownValue";

        final DistributionSet ds = testdataFactory.createDistributionSet("testDs");

        final DistributionSetMetadata metadata = new JpaDistributionSetMetadata(knownKey, ds, knownValue);
        final JpaDistributionSetMetadata createdMetadata = (JpaDistributionSetMetadata) distributionSetManagement
                .createDistributionSetMetadata(metadata);

        assertThat(createdMetadata).isNotNull();
        assertThat(createdMetadata.getId().getKey()).isEqualTo(knownKey);
        assertThat(createdMetadata.getDistributionSet().getId()).isEqualTo(ds.getId());
        assertThat(createdMetadata.getValue()).isEqualTo(knownValue);
    }

    @Test
    @Description("Ensures that updates concerning the internal software structure of a DS are not possible if the DS is already assigned.")
    public void updateDistributionSetForbiddedWithIllegalUpdate() {
        // prepare data
        Target target = new JpaTarget("4711");
        target = targetManagement.createTarget(target);

        SoftwareModule ah2 = new JpaSoftwareModule(appType, "agent-hub2", "1.0.5", null, "");
        SoftwareModule os2 = new JpaSoftwareModule(osType, "poky2", "3.0.3", null, "");

        DistributionSet ds = testdataFactory.createDistributionSet("ds-1");

        ah2 = softwareManagement.createSoftwareModule(ah2);
        os2 = softwareManagement.createSoftwareModule(os2);

        // update is allowed as it is still not assigned to a target
        ds.addModule(ah2);
        ds = distributionSetManagement.updateDistributionSet(ds);

        // assign target
        deploymentManagement.assignDistributionSet(ds.getId(), target.getControllerId());
        ds = distributionSetManagement.findDistributionSetByIdWithDetails(ds.getId());

        // description change is still allowed
        ds.setDescription("a different desc");
        ds = distributionSetManagement.updateDistributionSet(ds);

        // description change is still allowed
        ds.setName("a new name");
        ds = distributionSetManagement.updateDistributionSet(ds);

        // description change is still allowed
        ds.setVersion("a new version");
        ds = distributionSetManagement.updateDistributionSet(ds);

        // not allowed as it is assigned now
        ds.addModule(os2);
        try {
            ds = distributionSetManagement.updateDistributionSet(ds);
            fail("Expected EntityLockedException");
        } catch (final EntityLockedException e) {

        }

        // not allowed as it is assigned now
        ds.removeModule(ds.findFirstModuleByType(appType));
        try {
            ds = distributionSetManagement.updateDistributionSet(ds);
            fail("Expected EntityLockedException");
        } catch (final EntityLockedException e) {

        }
    }

    @Test
    @Description("Ensures that it is not possible to add a software module to a set that has no type defined.")
    public void updateDistributionSetModuleWithUndefinedTypeFails() {
        final DistributionSet testSet = new JpaDistributionSet();
        final SoftwareModule module = new JpaSoftwareModule(appType, "agent-hub2", "1.0.5", null, "");

        // update data
        try {
            testSet.addModule(module);
            fail("Should not have worked as DS type is undefined.");
        } catch (final DistributionSetTypeUndefinedException e) {

        }
    }

    @Test
    @Description("Ensures that it is not possible to add a software module that is not defined of the DS's type.")
    public void updateDistributionSetUnsupportedModuleFails() {
        final DistributionSet set = new JpaDistributionSet("agent-hub2", "1.0.5", "desc",
                new JpaDistributionSetType("test", "test", "test").addMandatoryModuleType(osType), null);
        final SoftwareModule module = new JpaSoftwareModule(appType, "agent-hub2", "1.0.5", null, "");

        // update data
        try {
            set.addModule(module);
            fail("Should not have worked as module type is not in DS type.");
        } catch (final UnsupportedSoftwareModuleForThisDistributionSetException e) {

        }
    }

    @Test
    @Description("Legal updates of a DS, e.g. name or description and module addition, removal while still unassigned.")
    public void updateDistributionSet() {
        // prepare data
        Target target = new JpaTarget("4711");
        target = targetManagement.createTarget(target);

        SoftwareModule os2 = new JpaSoftwareModule(osType, "poky2", "3.0.3", null, "");
        final SoftwareModule app2 = new JpaSoftwareModule(appType, "app2", "3.0.3", null, "");

        DistributionSet ds = testdataFactory.createDistributionSet("");

        os2 = softwareManagement.createSoftwareModule(os2);

        // update data
        // legal update of module addition
        ds.addModule(os2);
        distributionSetManagement.updateDistributionSet(ds);
        ds = distributionSetManagement.findDistributionSetByIdWithDetails(ds.getId());
        assertThat(ds.findFirstModuleByType(osType)).isEqualTo(os2);

        // legal update of module removal
        ds.removeModule(ds.findFirstModuleByType(appType));
        distributionSetManagement.updateDistributionSet(ds);
        ds = distributionSetManagement.findDistributionSetByIdWithDetails(ds.getId());
        assertThat(ds.findFirstModuleByType(appType)).isNull();

        // Update description
        ds.setDescription("a new description");
        distributionSetManagement.updateDistributionSet(ds);
        ds = distributionSetManagement.findDistributionSetByIdWithDetails(ds.getId());
        assertThat(ds.getDescription()).isEqualTo("a new description");

        // Update name
        ds.setName("a new name");
        distributionSetManagement.updateDistributionSet(ds);
        ds = distributionSetManagement.findDistributionSetByIdWithDetails(ds.getId());
        assertThat(ds.getName()).isEqualTo("a new name");
    }

    @Test
    @WithUser(allSpPermissions = true)
    @Description("Checks that metadata for a distribution set can be updated.")
    public void updateDistributionSetMetadata() throws InterruptedException {
        final String knownKey = "myKnownKey";
        final String knownValue = "myKnownValue";
        final String knownUpdateValue = "myNewUpdatedValue";

        // create a DS
        final DistributionSet ds = testdataFactory.createDistributionSet("testDs");
        // initial opt lock revision must be zero
        assertThat(ds.getOptLockRevision()).isEqualTo(1L);

        // create an DS meta data entry
        final DistributionSetMetadata dsMetadata = distributionSetManagement
                .createDistributionSetMetadata(new JpaDistributionSetMetadata(knownKey, ds, knownValue));

        DistributionSet changedLockRevisionDS = distributionSetManagement.findDistributionSetById(ds.getId());
        assertThat(changedLockRevisionDS.getOptLockRevision()).isEqualTo(2L);

        // modifying the meta data value
        dsMetadata.setValue(knownUpdateValue);
        dsMetadata.setKey(knownKey);
        ((JpaDistributionSetMetadata) dsMetadata).setDistributionSet(changedLockRevisionDS);

        Thread.sleep(100);

        // update the DS metadata
        final JpaDistributionSetMetadata updated = (JpaDistributionSetMetadata) distributionSetManagement
                .updateDistributionSetMetadata(dsMetadata);
        // we are updating the sw meta data so also modifying the base software
        // module so opt lock
        // revision must be three
        changedLockRevisionDS = distributionSetManagement.findDistributionSetById(ds.getId());
        assertThat(changedLockRevisionDS.getOptLockRevision()).isEqualTo(3L);
        assertThat(changedLockRevisionDS.getLastModifiedAt()).isGreaterThan(0L);

        // verify updated meta data contains the updated value
        assertThat(updated).isNotNull();
        assertThat(updated.getValue()).isEqualTo(knownUpdateValue);
        assertThat(updated.getId().getKey()).isEqualTo(knownKey);
        assertThat(updated.getDistributionSet().getId()).isEqualTo(ds.getId());
    }

    @Test
    @Description("Tests that a DS queue is possible where the result is ordered by the target assignment, i.e. assigned first in the list.")
    public void findDistributionSetsAllOrderedByLinkTarget() {

        final List<DistributionSet> buildDistributionSets = testdataFactory.createDistributionSets("dsOrder", 10);

        final List<Target> buildTargetFixtures = targetManagement
                .createTargets(testdataFactory.generateTargets(5, "tOrder", "someDesc"));

        final Iterator<DistributionSet> dsIterator = buildDistributionSets.iterator();
        final Iterator<Target> tIterator = buildTargetFixtures.iterator();
        final DistributionSet dsFirst = dsIterator.next();
        final DistributionSet dsSecond = dsIterator.next();
        final DistributionSet dsThree = dsIterator.next();
        final DistributionSet dsFour = dsIterator.next();
        final Target tFirst = tIterator.next();
        final Target tSecond = tIterator.next();

        // set assigned
        deploymentManagement.assignDistributionSet(dsSecond.getId(), tSecond.getControllerId());
        deploymentManagement.assignDistributionSet(dsThree.getId(), tFirst.getControllerId());
        // set installed
        final ArrayList<Target> installedDSSecond = new ArrayList<>();
        installedDSSecond.add(tSecond);
        sendUpdateActionStatusToTargets(dsSecond, installedDSSecond, Status.FINISHED, "some message");

        deploymentManagement.assignDistributionSet(dsFour.getId(), tSecond.getControllerId());

        final DistributionSetFilterBuilder distributionSetFilterBuilder = new DistributionSetFilterBuilder()
                .setIsDeleted(false).setIsComplete(true).setSelectDSWithNoTag(Boolean.FALSE);

        // target first only has an assigned DS-three so check order correct
        final List<DistributionSet> tFirstPin = distributionSetManagement.findDistributionSetsAllOrderedByLinkTarget(
                pageReq, distributionSetFilterBuilder, tFirst.getControllerId()).getContent();
        assertThat(tFirstPin.get(0)).isEqualTo(dsThree);
        assertThat(tFirstPin).hasSize(10);

        // target second has installed DS-2 and assigned DS-4 so check order
        // correct
        final List<DistributionSet> tSecondPin = distributionSetManagement.findDistributionSetsAllOrderedByLinkTarget(
                pageReq, distributionSetFilterBuilder, tSecond.getControllerId()).getContent();
        assertThat(tSecondPin.get(0)).isEqualTo(dsSecond);
        assertThat(tSecondPin.get(1)).isEqualTo(dsFour);
        assertThat(tFirstPin).hasSize(10);
    }

    @Test
    @Description("searches for distribution sets based on the various filter options, e.g. name, version, desc., tags.")
    public void searchDistributionSetsOnFilters() {
        DistributionSetTag dsTagA = tagManagement
                .createDistributionSetTag(new JpaDistributionSetTag("DistributionSetTag-A"));
        final DistributionSetTag dsTagB = tagManagement
                .createDistributionSetTag(new JpaDistributionSetTag("DistributionSetTag-B"));
        final DistributionSetTag dsTagC = tagManagement
                .createDistributionSetTag(new JpaDistributionSetTag("DistributionSetTag-C"));
        final DistributionSetTag dsTagD = tagManagement
                .createDistributionSetTag(new JpaDistributionSetTag("DistributionSetTag-D"));

        Collection<DistributionSet> ds100Group1 = testdataFactory.createDistributionSets("", 100);
        Collection<DistributionSet> ds100Group2 = testdataFactory.createDistributionSets("test2", 100);
        DistributionSet dsDeleted = testdataFactory.createDistributionSet("deleted");
        final DistributionSet dsInComplete = distributionSetManagement
                .createDistributionSet(new JpaDistributionSet("notcomplete", "1", "", standardDsType, null));

        final DistributionSetType newType = distributionSetManagement.createDistributionSetType(
                new JpaDistributionSetType("foo", "bar", "test").addMandatoryModuleType(osType)
                        .addOptionalModuleType(appType).addOptionalModuleType(runtimeType));

        final DistributionSet dsNewType = distributionSetManagement
                .createDistributionSet(new JpaDistributionSet("newtype", "1", "", newType, dsDeleted.getModules()));

        deploymentManagement.assignDistributionSet(dsDeleted, Lists.newArrayList(testdataFactory.createTargets(5)));
        distributionSetManagement.deleteDistributionSet(dsDeleted);
        dsDeleted = distributionSetManagement.findDistributionSetById(dsDeleted.getId());

        ds100Group1 = distributionSetManagement.toggleTagAssignment(ds100Group1, dsTagA).getAssignedEntity();
        dsTagA = distributionSetTagRepository.findByNameEquals(dsTagA.getName());
        ds100Group1 = distributionSetManagement.toggleTagAssignment(ds100Group1, dsTagB).getAssignedEntity();
        dsTagA = distributionSetTagRepository.findByNameEquals(dsTagA.getName());
        ds100Group2 = distributionSetManagement.toggleTagAssignment(ds100Group2, dsTagA).getAssignedEntity();
        dsTagA = distributionSetTagRepository.findByNameEquals(dsTagA.getName());

        // check setup
        assertThat(distributionSetRepository.findAll()).hasSize(203);

        // Find all
        List<DistributionSet> expected = new ArrayList<DistributionSet>();
        expected.addAll(ds100Group1);
        expected.addAll(ds100Group2);
        expected.add(dsDeleted);
        expected.add(dsInComplete);
        expected.add(dsNewType);

        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, getDistributionSetFilterBuilder().build()).getContent())
                        .hasSize(203).containsOnly(expected.toArray(new DistributionSet[0]));

        DistributionSetFilterBuilder distributionSetFilterBuilder;

        // search for not deleted
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsDeleted(Boolean.TRUE);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(1);

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsDeleted(false);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent())
                        .hasSize(202);

        // search for completed
        expected = new ArrayList<DistributionSet>();
        expected.addAll(ds100Group1);
        expected.addAll(ds100Group2);
        expected.add(dsDeleted);
        expected.add(dsNewType);

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(true);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(202)
                        .containsOnly(expected.toArray(new DistributionSet[0]));

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.FALSE);
        expected = new ArrayList<DistributionSet>();
        expected.add(dsInComplete);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(1)
                        .containsOnly(expected.toArray(new DistributionSet[0]));

        // search for type
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setType(newType);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(1);

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setType(standardDsType);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent())
                        .hasSize(202);

        // search for text
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setSearchText("%test2");
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent())
                        .hasSize(100);

        // search for tags
        distributionSetFilterBuilder = getDistributionSetFilterBuilder()
                .setTagNames(Lists.newArrayList(dsTagA.getName()));
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent())
                        .hasSize(200);

        distributionSetFilterBuilder = getDistributionSetFilterBuilder()
                .setTagNames(Lists.newArrayList(dsTagB.getName()));
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent())
                        .hasSize(100);

        distributionSetFilterBuilder = getDistributionSetFilterBuilder()
                .setTagNames(Lists.newArrayList(dsTagA.getName(), dsTagB.getName()));
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent())
                        .hasSize(200);

        distributionSetFilterBuilder = getDistributionSetFilterBuilder()
                .setTagNames(Lists.newArrayList(dsTagC.getName(), dsTagB.getName()));
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent())
                        .hasSize(100);

        distributionSetFilterBuilder = getDistributionSetFilterBuilder()
                .setTagNames(Lists.newArrayList(dsTagC.getName()));
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(0);

        // combine deleted and complete
        expected = new ArrayList<DistributionSet>();
        expected.addAll(ds100Group1);
        expected.addAll(ds100Group2);
        expected.add(dsNewType);

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE)
                .setIsDeleted(Boolean.FALSE);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(201)
                        .containsOnly(expected.toArray(new DistributionSet[0]));

        expected = new ArrayList<DistributionSet>();
        expected.add(dsInComplete);
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.FALSE);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(1)
                        .containsOnly(expected.toArray(new DistributionSet[0]));

        expected = new ArrayList<DistributionSet>();
        expected.add(dsDeleted);
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE)
                .setIsDeleted(Boolean.TRUE);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(1)
                        .containsOnly(expected.toArray(new DistributionSet[0]));

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsDeleted(Boolean.TRUE)
                .setIsComplete(Boolean.FALSE);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(0);

        // combine deleted and complete and type
        expected = new ArrayList<>();
        expected.addAll(ds100Group1);
        expected.addAll(ds100Group2);
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsDeleted(Boolean.FALSE)
                .setIsComplete(Boolean.TRUE).setType(standardDsType);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(200)
                        .containsOnly(expected.toArray(new DistributionSet[0]));

        expected = new ArrayList<>();
        expected.add(dsDeleted);
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE)
                .setType(standardDsType).setIsDeleted(Boolean.TRUE);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(1)
                        .containsOnly(expected.toArray(new DistributionSet[0]));

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsDeleted(Boolean.TRUE)
                .setIsComplete(Boolean.FALSE).setType(standardDsType);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(0);

        expected = new ArrayList<>();
        expected.add(dsNewType);
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE).setType(newType);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(1)
                        .containsOnly(expected.toArray(new DistributionSet[0]));

        // combine deleted and complete and type and text
        expected = new ArrayList<>();
        expected.addAll(ds100Group2);
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE)
                .setType(standardDsType).setSearchText("%test2");
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(100)
                        .containsOnly(expected.toArray(new DistributionSet[0]));

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE)
                .setIsDeleted(Boolean.TRUE).setType(standardDsType).setSearchText("%test2");
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(0);

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setType(standardDsType).setSearchText("%test2")
                .setIsComplete(false).setIsDeleted(false);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(0);

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setType(newType).setSearchText("%test2")
                .setIsComplete(Boolean.TRUE).setIsDeleted(false);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(0);

        // combine deleted and complete and type and text and tag
        expected = new ArrayList<>();
        expected.addAll(ds100Group2);
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(true).setType(standardDsType)
                .setSearchText("%test2").setTagNames(Lists.newArrayList(dsTagA.getName()));
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(100)
                        .containsOnly(expected.toArray(new DistributionSet[0]));

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setType(standardDsType).setSearchText("%test2")
                .setTagNames(Lists.newArrayList(dsTagA.getName())).setIsComplete(Boolean.FALSE)
                .setIsDeleted(Boolean.FALSE);
        assertThat(distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getContent()).hasSize(0);

    }

    private DistributionSetFilterBuilder getDistributionSetFilterBuilder() {
        return new DistributionSetFilterBuilder();
    }

    private List<Target> sendUpdateActionStatusToTargets(final DistributionSet dsA, final Iterable<Target> targs,
            final Status status, final String... msgs) {
        final List<Target> result = new ArrayList<>();
        for (final Target t : targs) {
            final List<Action> findByTarget = actionRepository.findByTarget((JpaTarget) t);
            for (final Action action : findByTarget) {
                result.add(sendUpdateActionStatusToTarget(status, action, t, msgs));
            }
        }
        return result;
    }

    @Test
    @Description("Simple DS load without the related data that should be loaded lazy.")
    public void findDistributionSetsWithoutLazy() {
        testdataFactory.createDistributionSets(20);

        assertThat(distributionSetManagement.findDistributionSetsByDeletedAndOrCompleted(pageReq, false, true))
                .hasSize(20);
    }

    @Test
    @Description("Deltes a DS that is no in use. Expected behaviour is a hard delete on the database.")
    public void deleteUnassignedDistributionSet() {
        DistributionSet ds1 = testdataFactory.createDistributionSet("ds-1");
        DistributionSet ds2 = testdataFactory.createDistributionSet("ds-2");

        ds1 = distributionSetManagement.findDistributionSetByNameAndVersion(ds1.getName(), ds1.getVersion());
        ds2 = distributionSetManagement.findDistributionSetByNameAndVersion(ds2.getName(), ds2.getVersion());

        // delete a ds
        assertThat(distributionSetRepository.findAll()).hasSize(2);
        distributionSetManagement.deleteDistributionSet(ds1.getId());
        // not assigned so not marked as deleted but fully deleted
        assertThat(distributionSetRepository.findAll()).hasSize(1);
        assertThat(distributionSetManagement
                .findDistributionSetsByDeletedAndOrCompleted(pageReq, Boolean.FALSE, Boolean.TRUE).getTotalElements())
                        .isEqualTo(1);
    }

    @Test
    @Description("Queries and loads the metadata related to a given software module.")
    public void findAllDistributionSetMetadataByDsId() {
        // create a DS
        DistributionSet ds1 = testdataFactory.createDistributionSet("testDs1");
        DistributionSet ds2 = testdataFactory.createDistributionSet("testDs2");

        for (int index = 0; index < 10; index++) {

            ds1 = distributionSetManagement
                    .createDistributionSetMetadata(new JpaDistributionSetMetadata("key" + index, ds1, "value" + index))
                    .getDistributionSet();
        }

        for (int index = 0; index < 20; index++) {

            ds2 = distributionSetManagement
                    .createDistributionSetMetadata(new JpaDistributionSetMetadata("key" + index, ds2, "value" + index))
                    .getDistributionSet();
        }

        final Page<DistributionSetMetadata> metadataOfDs1 = distributionSetManagement
                .findDistributionSetMetadataByDistributionSetId(ds1.getId(), new PageRequest(0, 100));

        final Page<DistributionSetMetadata> metadataOfDs2 = distributionSetManagement
                .findDistributionSetMetadataByDistributionSetId(ds2.getId(), new PageRequest(0, 100));

        assertThat(metadataOfDs1.getNumberOfElements()).isEqualTo(10);
        assertThat(metadataOfDs1.getTotalElements()).isEqualTo(10);

        assertThat(metadataOfDs2.getNumberOfElements()).isEqualTo(20);
        assertThat(metadataOfDs2.getTotalElements()).isEqualTo(20);
    }

    @Test
    @Description("Deletes a DS that is in use by either target assignment or rollout. Expected behaviour is a soft delete on the database, i.e. only marked as "
            + "deleted, kept as reference but unavailable for future use..")
    public void deleteAssignedDistributionSet() {
        DistributionSet ds1 = testdataFactory.createDistributionSet("ds-1");
        DistributionSet ds2 = testdataFactory.createDistributionSet("ds-2");
        DistributionSet dsToTargetAssigned = testdataFactory.createDistributionSet("ds-3");
        final DistributionSet dsToRolloutAssigned = testdataFactory.createDistributionSet("ds-4");

        ds1 = distributionSetManagement.findDistributionSetByNameAndVersion(ds1.getName(), ds1.getVersion());
        ds2 = distributionSetManagement.findDistributionSetByNameAndVersion(ds2.getName(), ds2.getVersion());

        // create assigned DS
        dsToTargetAssigned = distributionSetManagement.findDistributionSetByNameAndVersion(dsToTargetAssigned.getName(),
                dsToTargetAssigned.getVersion());
        final Target target = new JpaTarget("4712");
        final Target savedTarget = targetManagement.createTarget(target);
        final List<Target> toAssign = new ArrayList<>();
        toAssign.add(savedTarget);
        deploymentManagement.assignDistributionSet(dsToTargetAssigned, toAssign);

        // create assigned rollout
        createRolloutByVariables("test", "test", 5, "name==*", dsToRolloutAssigned, "50", "5");

        // delete assigned ds
        assertThat(distributionSetRepository.findAll()).hasSize(4);
        distributionSetManagement.deleteDistributionSet(dsToTargetAssigned.getId(), dsToRolloutAssigned.getId());

        // not assigned so not marked as deleted
        assertThat(distributionSetRepository.findAll()).hasSize(4);
        assertThat(distributionSetManagement
                .findDistributionSetsByDeletedAndOrCompleted(pageReq, Boolean.FALSE, Boolean.TRUE).getTotalElements())
                        .isEqualTo(2);
    }

    private Rollout createRolloutByVariables(final String rolloutName, final String rolloutDescription,
            final int groupSize, final String filterQuery, final DistributionSet distributionSet,
            final String successCondition, final String errorCondition) {
        final RolloutGroupConditions conditions = new RolloutGroupConditionBuilder()
                .successCondition(RolloutGroupSuccessCondition.THRESHOLD, successCondition)
                .errorCondition(RolloutGroupErrorCondition.THRESHOLD, errorCondition)
                .errorAction(RolloutGroupErrorAction.PAUSE, null).build();
        final Rollout rolloutToCreate = new JpaRollout();
        rolloutToCreate.setName(rolloutName);
        rolloutToCreate.setDescription(rolloutDescription);
        rolloutToCreate.setTargetFilterQuery(filterQuery);
        rolloutToCreate.setDistributionSet(distributionSet);
        return rolloutManagement.createRollout(rolloutToCreate, groupSize, conditions);
    }

    private Target sendUpdateActionStatusToTarget(final Status status, final Action updActA, final Target t,
            final String... msgs) {
        updActA.setStatus(status);

        final ActionStatus statusMessages = new JpaActionStatus();
        statusMessages.setAction(updActA);
        statusMessages.setOccurredAt(System.currentTimeMillis());
        statusMessages.setStatus(status);
        for (final String msg : msgs) {
            statusMessages.addMessage(msg);
        }
        controllerManagament.addUpdateActionStatus(statusMessages);
        return targetManagement.findTargetByControllerID(t.getControllerId());
    }

}
