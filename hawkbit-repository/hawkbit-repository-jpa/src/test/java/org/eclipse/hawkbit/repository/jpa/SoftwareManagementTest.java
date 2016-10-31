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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.validation.ConstraintViolationException;

import org.apache.commons.lang3.RandomUtils;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifact;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.AssignedSoftwareModule;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Repository")
@Stories("Software Management")
public class SoftwareManagementTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Try to update non updatable fields results in repository doing nothing.")
    public void updateTypeNonUpdateableFieldsFails() {
        final SoftwareModuleType created = softwareManagement
                .createSoftwareModuleType(new JpaSoftwareModuleType("test-key", "test-name", "test-desc", 1));

        created.setName("a new name");
        final SoftwareModuleType updated = softwareManagement.updateSoftwareModuleType(created);

        assertThat(updated.getOptLockRevision())
                .as("Expected version number of updated entitity to be equal to created version")
                .isEqualTo(created.getOptLockRevision());
    }

    @Test
    @Description("Calling update without changing fields results in no recorded change in the repository including unchanged audit fields.")
    public void updateNothingResultsInUnchangedRepositoryForType() {
        final SoftwareModuleType created = softwareManagement
                .createSoftwareModuleType(new JpaSoftwareModuleType("test-key", "test-name", "test-desc", 1));

        final SoftwareModuleType updated = softwareManagement.updateSoftwareModuleType(created);

        assertThat(updated.getOptLockRevision())
                .as("Expected version number of updated entitity to be equal to created version")
                .isEqualTo(created.getOptLockRevision());
    }

    @Test
    @Description("Calling update for changed fields results in change in the repository.")
    public void updateSoftareModuleTypeFieldsToNewValue() {
        final SoftwareModuleType created = softwareManagement
                .createSoftwareModuleType(new JpaSoftwareModuleType("test-key", "test-name", "test-desc", 1));

        created.setDescription("changed");
        created.setColour("changed");

        final SoftwareModuleType updated = softwareManagement.updateSoftwareModuleType(created);

        assertThat(updated.getOptLockRevision()).as("Expected version number of updated entitity is")
                .isEqualTo(created.getOptLockRevision() + 1);
        assertThat(updated.getDescription()).as("Updated description is").isEqualTo("changed");
        assertThat(updated.getColour()).as("Updated vendor is").isEqualTo("changed");
    }

    @Test
    @Description("Try to update non updatable fields results in repository doing nothing.")
    public void updateNonUpdateableFieldsFails() {
        final SoftwareModule ah = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(appType, "agent-hub", "1.0.1", null, ""));

        ah.setName("a new name");
        final SoftwareModule updated = softwareManagement.updateSoftwareModule(ah);

        assertThat(updated.getOptLockRevision())
                .as("Expected version number of updated entitity to be equal to created version")
                .isEqualTo(ah.getOptLockRevision());
    }

    @Test
    @Description("Calling update without changing fields results in no recorded change in the repository including unchanged audit fields.")
    public void updateNothingResultsInUnchangedRepository() {
        final SoftwareModule ah = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(appType, "agent-hub", "1.0.1", null, ""));

        final SoftwareModule updated = softwareManagement.updateSoftwareModule(ah);

        assertThat(updated.getOptLockRevision())
                .as("Expected version number of updated entitity to be equal to created version")
                .isEqualTo(ah.getOptLockRevision());
    }

    @Test
    @Description("Calling update for changed fields results in change in the repository.")
    public void updateSoftareModuleFieldsToNewValue() {
        final SoftwareModule ah = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(appType, "agent-hub", "1.0.1", "test desc", "test vendor"));

        ah.setDescription("changed");
        ah.setVendor("changed");
        final SoftwareModule updated = softwareManagement.updateSoftwareModule(ah);

        assertThat(updated.getOptLockRevision()).as("Expected version number of updated entitity is")
                .isEqualTo(ah.getOptLockRevision() + 1);
        assertThat(updated.getDescription()).as("Updated description is").isEqualTo("changed");
        assertThat(updated.getVendor()).as("Updated vendor is").isEqualTo("changed");
    }

    @Test
    @Description("Create Software Module call fails when called for existing entity.")
    public void createModuleCallFailsForExistingModule() {
        final SoftwareModule ah = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(appType, "agent-hub", "1.0.1", "test desc", "test vendor"));
        try {
            softwareManagement.createSoftwareModule(ah);
            fail("Should not have worked as module already exists.");
        } catch (final EntityAlreadyExistsException e) {

        }
    }

    @Test
    @Description("Create Software Modules call fails when called for existing entities.")
    public void createModulesCallFailsForExistingModule() {
        final List<SoftwareModule> modules = softwareManagement.createSoftwareModule(
                Lists.newArrayList(new JpaSoftwareModule(appType, "agent-hub", "1.0.1", "test desc", "test vendor"),
                        new JpaSoftwareModule(appType, "agent-hub", "1.0.2", "test desc", "test vendor")));
        try {
            softwareManagement.createSoftwareModule(modules);
            fail("Should not have worked as module already exists.");
        } catch (final EntityAlreadyExistsException e) {

        }
    }

    @Test
    @Description("Create Software Module Type call fails when called for existing entity.")
    public void createModuleTypeCallFailsForExistingType() {
        final SoftwareModuleType created = softwareManagement
                .createSoftwareModuleType(new JpaSoftwareModuleType("test-key", "test-name", "test-desc", 1));

        try {
            softwareManagement.createSoftwareModuleType(created);
            fail("Should not have worked as module already exists.");
        } catch (final EntityAlreadyExistsException e) {

        }
    }

    @Test
    @Description("Create Software Module Types call fails when called for existing entities.")
    public void createModuleTypesCallFailsForExistingTypes() {
        final List<SoftwareModuleType> created = softwareManagement.createSoftwareModuleType(
                Lists.newArrayList(new JpaSoftwareModuleType("test-key-bumlux", "test-name", "test-desc", 1),
                        new JpaSoftwareModuleType("test-key-bumlux2", "test-name2", "test-desc", 1)));

        try {
            softwareManagement.createSoftwareModuleType(created);
            fail("Should not have worked as module already exists.");
        } catch (final EntityAlreadyExistsException e) {

        }
    }

    @Test
    @Description("Calling update for changing fields to null results in change in the repository.")
    public void eraseSoftareModuleFields() {
        final SoftwareModule ah = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(appType, "agent-hub", "1.0.1", "test desc", "test vendor"));

        ah.setDescription(null);
        ah.setVendor(null);
        final SoftwareModule updated = softwareManagement.updateSoftwareModule(ah);

        assertThat(updated.getOptLockRevision()).as("Expected version number of updated entitity is")
                .isEqualTo(ah.getOptLockRevision() + 1);
        assertThat(updated.getDescription()).as("Updated description is").isNull();
        assertThat(updated.getVendor()).as("Updated vendor is").isNull();
    }

    @Test
    @Description("searched for software modules based on the various filter options, e.g. name,desc,type, version.")
    public void findSoftwareModuleByFilters() {
        final SoftwareModule ah = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(appType, "agent-hub", "1.0.1", null, ""));
        final SoftwareModule jvm = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(runtimeType, "oracle-jre", "1.7.2", null, ""));
        final SoftwareModule os = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(osType, "poky", "3.0.2", null, ""));

        final SoftwareModule ah2 = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(appType, "agent-hub", "1.0.2", null, ""));
        JpaDistributionSet ds = (JpaDistributionSet) distributionSetManagement.createDistributionSet(testdataFactory
                .generateDistributionSet("ds-1", "1.0.1", standardDsType, Lists.newArrayList(os, jvm, ah2)));

        final JpaTarget target = (JpaTarget) targetManagement.createTarget(new JpaTarget("test123"));
        ds = (JpaDistributionSet) assignSet(target, ds).getDistributionSet();

        // standard searches
        assertThat(softwareManagement.findSoftwareModuleByFilters(pageReq, "poky", osType.getId()).getContent())
                .hasSize(1);
        assertThat(softwareManagement.findSoftwareModuleByFilters(pageReq, "poky", osType.getId()).getContent().get(0))
                .isEqualTo(os);
        assertThat(softwareManagement.findSoftwareModuleByFilters(pageReq, "oracle%", runtimeType.getId()).getContent())
                .hasSize(1);
        assertThat(softwareManagement.findSoftwareModuleByFilters(pageReq, "oracle%", runtimeType.getId()).getContent()
                .get(0)).isEqualTo(jvm);
        assertThat(softwareManagement.findSoftwareModuleByFilters(pageReq, "1.0.1", appType.getId()).getContent())
                .hasSize(1);
        assertThat(
                softwareManagement.findSoftwareModuleByFilters(pageReq, "1.0.1", appType.getId()).getContent().get(0))
                        .isEqualTo(ah);
        assertThat(softwareManagement.findSoftwareModuleByFilters(pageReq, "1.0%", appType.getId()).getContent())
                .hasSize(2);

        // no we search with on entity marked as deleted
        softwareManagement.deleteSoftwareModule(
                softwareModuleRepository.findByAssignedToAndType(pageReq, ds, appType).getContent().get(0).getId());

        assertThat(softwareManagement.findSoftwareModuleByFilters(pageReq, "1.0%", appType.getId()).getContent())
                .hasSize(1);
        assertThat(softwareManagement.findSoftwareModuleByFilters(pageReq, "1.0%", appType.getId()).getContent().get(0))
                .isEqualTo(ah);
    }

    private Action assignSet(final JpaTarget target, final JpaDistributionSet ds) {
        deploymentManagement.assignDistributionSet(ds.getId(), new String[] { target.getControllerId() });
        assertThat(
                targetManagement.findTargetByControllerID(target.getControllerId()).getTargetInfo().getUpdateStatus())
                        .isEqualTo(TargetUpdateStatus.PENDING);
        assertThat(targetManagement.findTargetByControllerID(target.getControllerId()).getAssignedDistributionSet())
                .isEqualTo(ds);
        final Action action = actionRepository.findByTargetAndDistributionSet(pageReq, target, ds).getContent().get(0);
        assertThat(action).isNotNull();
        return action;
    }

    @Test
    @Description("Searches for software modules based on a list of IDs.")
    public void findSoftwareModulesById() {

        final List<Long> modules = new ArrayList<>();

        modules.add(softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(osType, "poky-una", "3.0.2", null, "")).getId());
        modules.add(softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(osType, "poky-u2na", "3.0.3", null, "")).getId());
        modules.add(624355263L);

        assertThat(softwareManagement.findSoftwareModulesById(modules)).hasSize(2);
    }

    @Test
    @Description("Searches for software modules by type.")
    public void findSoftwareModulesByType() {
        // found in test
        final SoftwareModule one = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(osType, "one", "one", null, ""));
        final SoftwareModule two = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(osType, "two", "two", null, ""));
        // ignored
        softwareManagement.deleteSoftwareModule(softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(osType, "deleted", "deleted", null, "")).getId());
        softwareManagement.createSoftwareModule(new JpaSoftwareModule(appType, "three", "3.0.2", null, ""));

        assertThat(softwareManagement.findSoftwareModulesByType(pageReq, osType.getId()).getContent())
                .as("Expected to find the following number of modules:").hasSize(2).as("with the following elements")
                .contains(two, one);
    }

    @Test
    @Description("Counts all software modules in the repsitory that are not marked as deleted.")
    public void countSoftwareModulesAll() {
        // found in test
        softwareManagement.createSoftwareModule(new JpaSoftwareModule(osType, "one", "one", null, ""));
        softwareManagement.createSoftwareModule(new JpaSoftwareModule(appType, "two", "two", null, ""));
        // ignored
        softwareManagement.deleteSoftwareModule(softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(osType, "deleted", "deleted", null, "")).getId());

        assertThat(softwareManagement.countSoftwareModulesAll()).as("Expected to find the following number of modules:")
                .isEqualTo(2);
    }

    @Test
    @Description("Tests the successfull deletion of software module types. Both unused (hard delete) and used ones (soft delete).")
    public void deleteAssignedAndUnassignedSoftwareModuleTypes() {
        assertThat(softwareManagement.findSoftwareModuleTypesAll(pageReq)).hasSize(3).contains(osType, runtimeType,
                appType);

        SoftwareModuleType type = softwareManagement.createSoftwareModuleType(
                new JpaSoftwareModuleType("bundle", "OSGi Bundle", "fancy stuff", Integer.MAX_VALUE));

        assertThat(softwareManagement.findSoftwareModuleTypesAll(pageReq)).hasSize(4).contains(osType, runtimeType,
                appType, type);

        // delete unassigned
        softwareManagement.deleteSoftwareModuleType(type);
        assertThat(softwareManagement.findSoftwareModuleTypesAll(pageReq)).hasSize(3).contains(osType, runtimeType,
                appType);
        assertThat(softwareModuleTypeRepository.findAll()).hasSize(3).contains((JpaSoftwareModuleType) osType,
                (JpaSoftwareModuleType) runtimeType, (JpaSoftwareModuleType) appType);

        type = softwareManagement.createSoftwareModuleType(
                new JpaSoftwareModuleType("bundle2", "OSGi Bundle2", "fancy stuff", Integer.MAX_VALUE));

        assertThat(softwareManagement.findSoftwareModuleTypesAll(pageReq)).hasSize(4).contains(osType, runtimeType,
                appType, type);

        softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(type, "Test SM", "1.0", "cool module", "from meeee"));

        // delete assigned
        softwareManagement.deleteSoftwareModuleType(type);
        assertThat(softwareManagement.findSoftwareModuleTypesAll(pageReq)).hasSize(3).contains(osType, runtimeType,
                appType);

        assertThat(softwareModuleTypeRepository.findAll()).hasSize(4).contains((JpaSoftwareModuleType) osType,
                (JpaSoftwareModuleType) runtimeType, (JpaSoftwareModuleType) appType,
                softwareModuleTypeRepository.findOne(type.getId()));
    }

    @Test
    @Description("Deletes an artifact, which is not assigned to a Distribution Set")
    public void hardDeleteOfNotAssignedArtifact() {

        // [STEP1]: Create SoftwareModuleX with Artifacts
        final SoftwareModule unassignedModule = createSoftwareModuleWithArtifacts(osType, "moduleX", "3.0.2", 2);
        final Iterator<Artifact> artifactsIt = unassignedModule.getArtifacts().iterator();
        final Artifact artifact1 = artifactsIt.next();
        final Artifact artifact2 = artifactsIt.next();

        // [STEP2]: Delete unassigned SoftwareModule
        softwareManagement.deleteSoftwareModule(unassignedModule.getId());

        // [VERIFY EXPECTED RESULT]:
        // verify: SoftwareModule is deleted
        assertThat(softwareModuleRepository.findAll()).hasSize(0);
        assertThat(softwareManagement.findSoftwareModuleById(unassignedModule.getId())).isNull();

        // verify: binary data of artifact is deleted
        assertArtfiactNull(artifact1, artifact2);

        // verify: meta data of artifact is deleted
        assertThat(artifactRepository.findOne(artifact1.getId())).isNull();
        assertThat(artifactRepository.findOne(artifact2.getId())).isNull();
    }

    @Test
    @Description("Deletes an artifact, which is assigned to a DistributionSet")
    public void softDeleteOfAssignedArtifact() {

        // Init DistributionSet
        final DistributionSet disSet = distributionSetManagement
                .createDistributionSet(new JpaDistributionSet("ds1", "v1.0", "test ds", standardDsType, null));

        // [STEP1]: Create SoftwareModuleX with ArtifactX
        SoftwareModule assignedModule = createSoftwareModuleWithArtifacts(osType, "moduleX", "3.0.2", 2);

        // [STEP2]: Assign SoftwareModule to DistributionSet
        distributionSetManagement.assignSoftwareModules(disSet, Sets.newHashSet(assignedModule));

        // [STEP3]: Delete the assigned SoftwareModule
        softwareManagement.deleteSoftwareModule(assignedModule.getId());

        // [VERIFY EXPECTED RESULT]:
        // verify: assignedModule is marked as deleted
        assignedModule = softwareManagement.findSoftwareModuleById(assignedModule.getId());
        assertTrue("The module should be flagged as deleted", assignedModule.isDeleted());
        assertThat(softwareManagement.findSoftwareModulesAll(pageReq)).hasSize(0);
        assertThat(softwareModuleRepository.findAll()).hasSize(1);

        // verify: binary data is deleted
        final Iterator<Artifact> artifactsIt = assignedModule.getArtifacts().iterator();
        final Artifact artifact1 = artifactsIt.next();
        final Artifact artifact2 = artifactsIt.next();
        assertArtfiactNull(artifact1, artifact2);

        // verify: artifact meta data is still available
        assertThat(artifactRepository.findOne(artifact1.getId())).isNotNull();
        assertThat(artifactRepository.findOne(artifact2.getId())).isNotNull();
    }

    @Test
    @Description("Delete an artifact, which has been assigned to a rolled out DistributionSet in the past")
    public void softDeleteOfHistoricalAssignedArtifact() {

        // Init target and DistributionSet
        final Target target = targetManagement.createTarget(new JpaTarget("test123"));
        final DistributionSet disSet = distributionSetManagement
                .createDistributionSet(new JpaDistributionSet("ds1", "v1.0", "test ds", standardDsType, null));

        // [STEP1]: Create SoftwareModuleX and include the new ArtifactX
        SoftwareModule assignedModule = createSoftwareModuleWithArtifacts(osType, "moduleX", "3.0.2", 2);

        // [STEP2]: Assign SoftwareModule to DistributionSet
        distributionSetManagement.assignSoftwareModules(disSet, Sets.newHashSet(assignedModule));

        // [STEP3]: Assign DistributionSet to a Device
        deploymentManagement.assignDistributionSet(disSet, Lists.newArrayList(target));

        // [STEP4]: Delete the DistributionSet
        distributionSetManagement.deleteDistributionSet(disSet);

        // [STEP5]: Delete the assigned SoftwareModule
        softwareManagement.deleteSoftwareModule(assignedModule.getId());

        // [VERIFY EXPECTED RESULT]:
        // verify: assignedModule is marked as deleted
        assignedModule = softwareManagement.findSoftwareModuleById(assignedModule.getId());
        assertTrue("The found module should be flagged deleted", assignedModule.isDeleted());
        assertThat(softwareManagement.findSoftwareModulesAll(pageReq)).hasSize(0);
        assertThat(softwareModuleRepository.findAll()).hasSize(1);

        // verify: binary data is deleted
        final Iterator<Artifact> artifactsIt = assignedModule.getArtifacts().iterator();
        final Artifact artifact1 = artifactsIt.next();
        final Artifact artifact2 = artifactsIt.next();
        assertArtfiactNull(artifact1, artifact2);

        // verify: artifact meta data is still available
        assertThat(artifactRepository.findOne(artifact1.getId())).isNotNull();
        assertThat(artifactRepository.findOne(artifact2.getId())).isNotNull();
    }

    @Test
    @Description("Delete an softwaremodule with an artifact, which is alsoused by another softwaremodule.")
    public void deleteSoftwareModulesWithSharedArtifact() throws IOException {

        // Init artifact binary data, target and DistributionSets
        final byte[] source = RandomUtils.nextBytes(1024);

        // [STEP1]: Create SoftwareModuleX and add a new ArtifactX
        SoftwareModule moduleX = createSoftwareModuleWithArtifacts(osType, "modulex", "v1.0", 0);

        // [STEP2]: Create newArtifactX and add it to SoftwareModuleX
        artifactManagement.createArtifact(new ByteArrayInputStream(source), moduleX.getId(), "artifactx", false);
        moduleX = softwareManagement.findSoftwareModuleById(moduleX.getId());
        final Artifact artifactX = moduleX.getArtifacts().iterator().next();

        // [STEP3]: Create SoftwareModuleY and add the same ArtifactX
        SoftwareModule moduleY = createSoftwareModuleWithArtifacts(osType, "moduley", "v1.0", 0);

        // [STEP4]: Assign the same ArtifactX to SoftwareModuleY
        artifactManagement.createArtifact(new ByteArrayInputStream(source), moduleY.getId(), "artifactx", false);
        moduleY = softwareManagement.findSoftwareModuleById(moduleY.getId());
        final Artifact artifactY = moduleY.getArtifacts().iterator().next();

        // [STEP5]: Delete SoftwareModuleX
        softwareManagement.deleteSoftwareModule(moduleX.getId());

        // [VERIFY EXPECTED RESULT]:
        // verify: SoftwareModuleX is deleted, and ModuelY still exists
        assertThat(softwareModuleRepository.findAll()).hasSize(1);
        assertThat(softwareManagement.findSoftwareModuleById(moduleX.getId())).isNull();
        assertThat(softwareManagement.findSoftwareModuleById(moduleY.getId())).isNotNull();

        // verify: binary data of artifact is not deleted
        assertArtfiactNotNull(artifactY);

        // verify: meta data of artifactX is deleted
        assertThat(artifactRepository.findOne(artifactX.getId())).isNull();

        // verify: meta data of artifactY is not deleted
        assertThat(artifactRepository.findOne(artifactY.getId())).isNotNull();
    }

    @Test
    @Description("Delete two assigned softwaremodules which share an artifact.")
    public void deleteMultipleSoftwareModulesWhichShareAnArtifact() throws IOException {

        // Init artifact binary data, target and DistributionSets
        final byte[] source = RandomUtils.nextBytes(1024);
        final Target target = targetManagement.createTarget(new JpaTarget("test123"));
        final DistributionSet disSetX = distributionSetManagement
                .createDistributionSet(new JpaDistributionSet("dsX", "v1.0", "test dsX", standardDsType, null));
        final DistributionSet disSetY = distributionSetManagement
                .createDistributionSet(new JpaDistributionSet("dsY", "v1.0", "test dsY", standardDsType, null));

        // [STEP1]: Create SoftwareModuleX and add a new ArtifactX
        SoftwareModule moduleX = createSoftwareModuleWithArtifacts(osType, "modulex", "v1.0", 0);

        artifactManagement.createArtifact(new ByteArrayInputStream(source), moduleX.getId(), "artifactx", false);
        moduleX = softwareManagement.findSoftwareModuleById(moduleX.getId());
        final Artifact artifactX = moduleX.getArtifacts().iterator().next();

        // [STEP2]: Create SoftwareModuleY and add the same ArtifactX
        SoftwareModule moduleY = createSoftwareModuleWithArtifacts(osType, "moduley", "v1.0", 0);

        artifactManagement.createArtifact(new ByteArrayInputStream(source), moduleY.getId(), "artifactx", false);
        moduleY = softwareManagement.findSoftwareModuleById(moduleY.getId());
        final Artifact artifactY = moduleY.getArtifacts().iterator().next();

        // [STEP3]: Assign SoftwareModuleX to DistributionSetX and to target
        distributionSetManagement.assignSoftwareModules(disSetX, Sets.newHashSet(moduleX));
        deploymentManagement.assignDistributionSet(disSetX, Lists.newArrayList(target));

        // [STEP4]: Assign SoftwareModuleY to DistributionSet and to target
        distributionSetManagement.assignSoftwareModules(disSetY, Sets.newHashSet(moduleY));
        deploymentManagement.assignDistributionSet(disSetY, Lists.newArrayList(target));

        // [STEP5]: Delete SoftwareModuleX
        softwareManagement.deleteSoftwareModule(moduleX.getId());

        // [STEP6]: Delete SoftwareModuleY
        softwareManagement.deleteSoftwareModule(moduleY.getId());

        // [VERIFY EXPECTED RESULT]:
        moduleX = softwareManagement.findSoftwareModuleById(moduleX.getId());
        moduleY = softwareManagement.findSoftwareModuleById(moduleY.getId());

        // verify: SoftwareModuleX and SofwtareModule are marked as deleted
        assertThat(moduleX).isNotNull();
        assertThat(moduleY).isNotNull();
        assertTrue("The module should be flagged deleted", moduleX.isDeleted());
        assertTrue("The module should be flagged deleted", moduleY.isDeleted());
        assertThat(softwareManagement.findSoftwareModulesAll(pageReq)).hasSize(0);
        assertThat(softwareModuleRepository.findAll()).hasSize(2);

        // verify: binary data of artifact is deleted
        assertArtfiactNull(artifactX, artifactY);

        // verify: meta data of artifactX and artifactY is not deleted
        assertThat(artifactRepository.findOne(artifactY.getId())).isNotNull();
    }

    private SoftwareModule createSoftwareModuleWithArtifacts(final SoftwareModuleType type, final String name,
            final String version, final int numberArtifacts) {

        final long countSoftwareModule = softwareModuleRepository.count();

        // create SoftwareModule
        SoftwareModule softwareModule = softwareManagement.createSoftwareModule(
                new JpaSoftwareModule(type, name, version, "description of artifact " + name, ""));

        for (int i = 0; i < numberArtifacts; i++) {
            artifactManagement.createArtifact(new RandomGeneratedInputStream(5 * 1024), softwareModule.getId(),
                    "file" + (i + 1), false);
        }

        // Verify correct Creation of SoftwareModule and corresponding artifacts
        softwareModule = softwareManagement.findSoftwareModuleById(softwareModule.getId());
        assertThat(softwareModuleRepository.findAll()).hasSize((int) countSoftwareModule + 1);

        final List<Artifact> artifacts = softwareModule.getArtifacts();

        assertThat(artifacts).hasSize(numberArtifacts);
        if (numberArtifacts != 0) {
            assertArtfiactNotNull(artifacts.toArray(new Artifact[artifacts.size()]));
        }

        artifacts.forEach(artifact -> {
            assertThat(artifactRepository.findOne(artifact.getId())).isNotNull();
        });
        return softwareModule;
    }

    private void assertArtfiactNotNull(final Artifact... results) {
        assertThat(artifactRepository.findAll()).hasSize(results.length);
        for (final Artifact result : results) {
            assertThat(result.getId()).isNotNull();
            assertThat(binaryArtifactRepository.getArtifactBySha1(((JpaArtifact) result).getGridFsFileName()))
                    .isNotNull();
        }
    }

    private void assertArtfiactNull(final Artifact... results) {
        for (final Artifact result : results) {
            assertThat(binaryArtifactRepository.getArtifactBySha1(((JpaArtifact) result).getGridFsFileName())).isNull();
        }
    }

    @Test
    @Description("Test verfies that results are returned based on given filter parameters and in the specified order.")
    public void findSoftwareModuleOrderByDistributionModuleNameAscModuleVersionAsc() {
        // test meta data
        final SoftwareModuleType testType = softwareManagement
                .createSoftwareModuleType(new JpaSoftwareModuleType("thetype", "thename", "desc", 100));
        final DistributionSetType testDsType = distributionSetManagement
                .createDistributionSetType(new JpaDistributionSetType("key", "name", "desc")
                        .addMandatoryModuleType(osType).addOptionalModuleType(testType));

        // found in test
        final SoftwareModule unassigned = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(testType, "asis", "found", null, ""));
        final SoftwareModule one = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(testType, "found", "b", null, ""));
        final SoftwareModule two = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(testType, "found", "c", null, ""));
        final SoftwareModule differentName = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(testType, "differentname", "d", null, ""));

        // ignored
        final SoftwareModule deleted = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(testType, "deleted", "deleted", null, ""));
        final SoftwareModule four = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(osType, "sdfjhsdj", "e", null, ""));

        final DistributionSet set = distributionSetManagement.createDistributionSet(new JpaDistributionSet("set", "1",
                "desc", testDsType, Lists.newArrayList(one, two, deleted, four, differentName)));
        softwareManagement.deleteSoftwareModule(deleted.getId());

        // with filter on name, version and module type
        assertThat(softwareManagement.findSoftwareModuleOrderBySetAssignmentAndModuleNameAscModuleVersionAsc(pageReq,
                set.getId(), "found", testType.getId()).getContent())
                        .as("Found modules with given name, given module type and the assigned ones first")
                        .containsExactly(new AssignedSoftwareModule(one, true), new AssignedSoftwareModule(two, true),
                                new AssignedSoftwareModule(unassigned, false));

        // with filter on module type only
        assertThat(softwareManagement.findSoftwareModuleOrderBySetAssignmentAndModuleNameAscModuleVersionAsc(pageReq,
                set.getId(), null, testType.getId()).getContent())
                        .as("Found modules with given module type and the assigned ones first").containsExactly(
                                new AssignedSoftwareModule(differentName, true), new AssignedSoftwareModule(one, true),
                                new AssignedSoftwareModule(two, true), new AssignedSoftwareModule(unassigned, false));

        // without any filter
        assertThat(softwareManagement.findSoftwareModuleOrderBySetAssignmentAndModuleNameAscModuleVersionAsc(pageReq,
                set.getId(), null, null).getContent()).as("Found modules with the assigned ones first").containsExactly(
                        new AssignedSoftwareModule(differentName, true), new AssignedSoftwareModule(one, true),
                        new AssignedSoftwareModule(two, true), new AssignedSoftwareModule(four, true),
                        new AssignedSoftwareModule(unassigned, false));
    }

    @Test
    @Description("Checks that number of modules is returned as expected based on given filters.")
    public void countSoftwareModuleByFilters() {

        // test meta data
        final SoftwareModuleType testType = softwareManagement
                .createSoftwareModuleType(new JpaSoftwareModuleType("thetype", "thename", "desc", 100));
        final DistributionSetType testDsType = distributionSetManagement
                .createDistributionSetType(new JpaDistributionSetType("key", "name", "desc")
                        .addMandatoryModuleType(osType).addOptionalModuleType(testType));

        // test modules
        softwareManagement.createSoftwareModule(new JpaSoftwareModule(testType, "asis", "found", null, ""));
        final SoftwareModule one = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(testType, "found", "b", null, ""));
        final SoftwareModule two = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(testType, "found", "c", null, ""));
        final SoftwareModule differentName = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(testType, "differentname", "d", null, ""));
        final SoftwareModule four = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(osType, "found", "3.0.2", null, ""));

        // one soft deleted
        final SoftwareModule deleted = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(testType, "deleted", "deleted", null, ""));
        distributionSetManagement.createDistributionSet(new JpaDistributionSet("set", "1", "desc", testDsType,
                Lists.newArrayList(one, two, deleted, four, differentName)));
        softwareManagement.deleteSoftwareModule(deleted.getId());

        // test
        assertThat(softwareManagement.countSoftwareModuleByFilters("found", testType.getId()))
                .as("Number of modules with given name or version and type").isEqualTo(3);
        assertThat(softwareManagement.countSoftwareModuleByFilters(null, testType.getId()))
                .as("Number of modules with given type").isEqualTo(4);
        assertThat(softwareManagement.countSoftwareModuleByFilters(null, null)).as("Number of modules overall")
                .isEqualTo(5);
    }

    @Test
    @Description("Verfies that all undeleted software modules are found in the repository.")
    public void countSoftwareModuleTypesAll() {
        final SoftwareModuleType testType = softwareManagement
                .createSoftwareModuleType(new JpaSoftwareModuleType("thetype", "thename", "desc", 100));
        final DistributionSetType testDsType = distributionSetManagement
                .createDistributionSetType(new JpaDistributionSetType("key", "name", "desc")
                        .addMandatoryModuleType(osType).addOptionalModuleType(testType));
        final SoftwareModule four = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(osType, "found", "3.0.2", null, ""));

        // one soft deleted
        final SoftwareModule deleted = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(testType, "deleted", "deleted", null, ""));
        distributionSetManagement.createDistributionSet(
                new JpaDistributionSet("set", "1", "desc", testDsType, Lists.newArrayList(deleted, four)));
        softwareManagement.deleteSoftwareModule(deleted.getId());

        assertThat(softwareManagement.countSoftwareModulesAll()).as("Number of undeleted modules").isEqualTo(1);
        assertThat(softwareModuleRepository.count()).as("Number of all modules").isEqualTo(2);
    }

    @Test
    @Description("Checks that software module typeis found based on given name.")
    public void findSoftwareModuleTypeByName() {
        final SoftwareModuleType found = softwareManagement
                .createSoftwareModuleType(new JpaSoftwareModuleType("thetype", "thename", "desc", 100));
        softwareManagement.createSoftwareModuleType(new JpaSoftwareModuleType("thetype2", "anothername", "desc", 100));

        assertThat(softwareManagement.findSoftwareModuleTypeByName("thename")).as("Type with given name")
                .isEqualTo(found);
    }

    @Test
    @Description("Verfies that it is not possible to create a type that alrady exists.")
    public void createSoftwareModuleTypeFailsWithExistingEntity() {
        final SoftwareModuleType created = softwareManagement
                .createSoftwareModuleType(new JpaSoftwareModuleType("thetype", "thename", "desc", 100));
        try {
            softwareManagement.createSoftwareModuleType(created);
            fail("should not have worked as module type already exists");
        } catch (final EntityAlreadyExistsException e) {

        }

    }

    @Test
    @Description("Verfies that it is not possible to create a list of types where one already exists.")
    public void createSoftwareModuleTypesFailsWithExistingEntity() {
        final SoftwareModuleType created = softwareManagement
                .createSoftwareModuleType(new JpaSoftwareModuleType("thetype", "thename", "desc", 100));
        try {
            softwareManagement.createSoftwareModuleType(
                    Lists.newArrayList(created, new JpaSoftwareModuleType("anothertype", "anothername", "desc", 100)));
            fail("should not have worked as module type already exists");
        } catch (final EntityAlreadyExistsException e) {

        }
    }

    @Test
    @Description("Verifies that the creation of a softwareModuleType is failing because of invalid max assignment")
    public void createSoftwareModuleTypesFailsWithInvalidMaxAssignment() {
        try {
            softwareManagement.createSoftwareModuleType(new JpaSoftwareModuleType("type", "name", "desc", 0));
            fail("should not have worked as max assignment is invalid. Should be greater than 0.");
        } catch (final ConstraintViolationException e) {

        }
    }

    @Test
    @Description("Verfies that multiple types are created as requested.")
    public void createMultipleSoftwareModuleTypes() {
        final List<SoftwareModuleType> created = softwareManagement.createSoftwareModuleType(
                Lists.newArrayList(new JpaSoftwareModuleType("thetype", "thename", "desc", 100),
                        new JpaSoftwareModuleType("thetype2", "thename2", "desc2", 100)));

        assertThat(created.size()).as("Number of created types").isEqualTo(2);
        assertThat(softwareManagement.countSoftwareModuleTypesAll()).as("Number of types in repository").isEqualTo(5);
    }

    @Test
    @Description("Verfies that software modules are resturned that are assigned to given DS.")
    public void findSoftwareModuleByAssignedTo() {
        // test meta data
        final SoftwareModuleType testType = softwareManagement
                .createSoftwareModuleType(new JpaSoftwareModuleType("thetype", "thename", "desc", 100));
        final DistributionSetType testDsType = distributionSetManagement
                .createDistributionSetType(new JpaDistributionSetType("key", "name", "desc")
                        .addMandatoryModuleType(osType).addOptionalModuleType(testType));

        // test modules
        softwareManagement.createSoftwareModule(new JpaSoftwareModule(testType, "asis", "found", null, ""));
        final SoftwareModule one = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(testType, "found", "b", null, ""));

        // one soft deleted
        final SoftwareModule deleted = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(testType, "deleted", "deleted", null, ""));
        final DistributionSet set = distributionSetManagement.createDistributionSet(
                new JpaDistributionSet("set", "1", "desc", testDsType, Lists.newArrayList(one, deleted)));
        softwareManagement.deleteSoftwareModule(deleted.getId());

        assertThat(softwareManagement.findSoftwareModuleByAssignedTo(pageReq, set).getContent())
                .as("Found this number of modules").hasSize(2);
    }

    @Test
    @Description("Checks that metadata for a software module can be created.")
    public void createSoftwareModuleMetadata() {

        final String knownKey1 = "myKnownKey1";
        final String knownValue1 = "myKnownValue1";

        final String knownKey2 = "myKnownKey2";
        final String knownValue2 = "myKnownValue2";

        final SoftwareModule ah = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(appType, "agent-hub", "1.0.1", null, ""));

        assertThat(ah.getOptLockRevision()).isEqualTo(1);

        final SoftwareModuleMetadata swMetadata1 = new JpaSoftwareModuleMetadata(knownKey1, ah, knownValue1);

        final SoftwareModuleMetadata swMetadata2 = new JpaSoftwareModuleMetadata(knownKey2, ah, knownValue2);

        final List<SoftwareModuleMetadata> softwareModuleMetadata = softwareManagement
                .createSoftwareModuleMetadata(Lists.newArrayList(swMetadata1, swMetadata2));

        final SoftwareModule changedLockRevisionModule = softwareManagement.findSoftwareModuleById(ah.getId());
        assertThat(changedLockRevisionModule.getOptLockRevision()).isEqualTo(2);

        assertThat(softwareModuleMetadata).hasSize(2);
        assertThat(softwareModuleMetadata.get(0)).isNotNull();
        assertThat(softwareModuleMetadata.get(0).getValue()).isEqualTo(knownValue1);
        assertThat(((JpaSoftwareModuleMetadata) softwareModuleMetadata.get(0)).getId().getKey()).isEqualTo(knownKey1);
        assertThat(softwareModuleMetadata.get(0).getSoftwareModule().getId()).isEqualTo(ah.getId());
    }

    @Test
    @Description("Checks that metadata for a software module cannot be created for an existing key.")
    public void createSoftwareModuleMetadataFailsIfKeyExists() {

        final String knownKey1 = "myKnownKey1";
        final String knownValue1 = "myKnownValue1";
        final String knownValue2 = "myKnownValue2";

        final SoftwareModule ah = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(appType, "agent-hub", "1.0.1", null, ""));

        softwareManagement.createSoftwareModuleMetadata(new JpaSoftwareModuleMetadata(knownKey1, ah, knownValue1));

        try {
            softwareManagement.createSoftwareModuleMetadata(new JpaSoftwareModuleMetadata(knownKey1, ah, knownValue2));
            fail("should not have worked as module metadata already exists");
        } catch (final EntityAlreadyExistsException e) {

        }
    }

    @Test
    @WithUser(allSpPermissions = true)
    @Description("Checks that metadata for a software module can be updated.")
    public void updateSoftwareModuleMetadata() throws InterruptedException {
        final String knownKey = "myKnownKey";
        final String knownValue = "myKnownValue";
        final String knownUpdateValue = "myNewUpdatedValue";

        // create a base software module
        final SoftwareModule ah = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(appType, "agent-hub", "1.0.1", null, ""));
        // initial opt lock revision must be 1
        assertThat(ah.getOptLockRevision()).isEqualTo(1);

        // create an software module meta data entry
        final List<SoftwareModuleMetadata> softwareModuleMetadata = softwareManagement.createSoftwareModuleMetadata(
                Collections.singleton(new JpaSoftwareModuleMetadata(knownKey, ah, knownValue)));
        assertThat(softwareModuleMetadata).hasSize(1);
        // base software module should have now the opt lock revision one
        // because we are modifying the
        // base software module
        SoftwareModule changedLockRevisionModule = softwareManagement.findSoftwareModuleById(ah.getId());
        assertThat(changedLockRevisionModule.getOptLockRevision()).isEqualTo(2);

        // modifying the meta data value
        softwareModuleMetadata.get(0).setValue(knownUpdateValue);
        softwareModuleMetadata.get(0).setSoftwareModule(softwareManagement.findSoftwareModuleById(ah.getId()));
        softwareModuleMetadata.get(0).setKey(knownKey);

        // update the software module metadata
        Thread.sleep(100);
        final SoftwareModuleMetadata updated = softwareManagement
                .updateSoftwareModuleMetadata(softwareModuleMetadata.get(0));
        // we are updating the sw meta data so also modiying the base software
        // module so opt lock
        // revision must be two
        changedLockRevisionModule = softwareManagement.findSoftwareModuleById(ah.getId());
        assertThat(changedLockRevisionModule.getOptLockRevision()).isEqualTo(3);

        // verify updated meta data contains the updated value
        assertThat(updated).isNotNull();
        assertThat(updated.getValue()).isEqualTo(knownUpdateValue);
        assertThat(((JpaSoftwareModuleMetadata) updated).getId().getKey()).isEqualTo(knownKey);
        assertThat(updated.getSoftwareModule().getId()).isEqualTo(ah.getId());
    }

    @Test
    @Description("Verfies that existing metadata can be deleted.")
    public void deleteSoftwareModuleMetadata() {
        final String knownKey1 = "myKnownKey1";
        final String knownValue1 = "myKnownValue1";

        SoftwareModule ah = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(appType, "agent-hub", "1.0.1", null, ""));

        ah = softwareManagement.createSoftwareModuleMetadata(new JpaSoftwareModuleMetadata(knownKey1, ah, knownValue1))
                .getSoftwareModule();

        assertThat(softwareManagement.findSoftwareModuleMetadataBySoftwareModuleId(ah.getId()))
                .as("Contains the created metadata element")
                .containsExactly(new JpaSoftwareModuleMetadata(knownKey1, ah, knownValue1));

        softwareManagement.deleteSoftwareModuleMetadata(ah.getId(), knownKey1);
        assertThat(softwareManagement.findSoftwareModuleMetadataBySoftwareModuleId(ah.getId()))
                .as("Metadata elemenets are").isEmpty();
    }

    @Test
    @Description("Verfies that non existing metadata find results in exception.")
    public void findSoftwareModuleMetadataFailsIfEntryDoesNotExist() {
        final String knownKey1 = "myKnownKey1";
        final String knownValue1 = "myKnownValue1";

        SoftwareModule ah = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(appType, "agent-hub", "1.0.1", null, ""));

        ah = softwareManagement.createSoftwareModuleMetadata(new JpaSoftwareModuleMetadata(knownKey1, ah, knownValue1))
                .getSoftwareModule();

        try {
            softwareManagement.findSoftwareModuleMetadata(ah.getId(), "doesnotexist");
            fail("should not have worked as module metadata with that key does not exist");
        } catch (final EntityNotFoundException e) {

        }
    }

    @Test
    @Description("Queries and loads the metadata related to a given software module.")
    public void findAllSoftwareModuleMetadataBySwId() {

        SoftwareModule sw1 = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(appType, "agent-hub", "1.0.1", null, ""));

        SoftwareModule sw2 = softwareManagement
                .createSoftwareModule(new JpaSoftwareModule(osType, "os", "1.0.1", null, ""));

        for (int index = 0; index < 10; index++) {
            sw1 = softwareManagement
                    .createSoftwareModuleMetadata(new JpaSoftwareModuleMetadata("key" + index, sw1, "value" + index))
                    .getSoftwareModule();
        }

        for (int index = 0; index < 20; index++) {
            sw2 = softwareManagement
                    .createSoftwareModuleMetadata(new JpaSoftwareModuleMetadata("key" + index, sw2, "value" + index))
                    .getSoftwareModule();
        }

        final Page<SoftwareModuleMetadata> metadataOfSw1 = softwareManagement
                .findSoftwareModuleMetadataBySoftwareModuleId(sw1.getId(), new PageRequest(0, 100));

        final Page<SoftwareModuleMetadata> metadataOfSw2 = softwareManagement
                .findSoftwareModuleMetadataBySoftwareModuleId(sw2.getId(), new PageRequest(0, 100));

        assertThat(metadataOfSw1.getNumberOfElements()).isEqualTo(10);
        assertThat(metadataOfSw1.getTotalElements()).isEqualTo(10);

        assertThat(metadataOfSw2.getNumberOfElements()).isEqualTo(20);
        assertThat(metadataOfSw2.getTotalElements()).isEqualTo(20);
    }
}
