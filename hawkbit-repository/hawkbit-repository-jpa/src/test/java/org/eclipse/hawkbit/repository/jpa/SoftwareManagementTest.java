/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.validation.ConstraintViolationException;

import org.apache.commons.lang3.RandomUtils;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleTypeCreate;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifact;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
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
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
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
    @Description("Verifies that management get access reacts as specfied on calls for non existing entities by means "
            + "of Optional not present.")
    @ExpectEvents({ @Expect(type = SoftwareModuleCreatedEvent.class, count = 1) })
    public void nonExistingEntityAccessReturnsNotPresent() {
        final SoftwareModule module = testdataFactory.createSoftwareModuleApp();

        assertThat(softwareManagement.findSoftwareModuleById(1234L)).isNotPresent();

        assertThat(softwareManagement.findSoftwareModuleTypeById(NOT_EXIST_IDL)).isNotPresent();
        assertThat(softwareManagement.findSoftwareModuleTypeByKey(NOT_EXIST_ID)).isNotPresent();
        assertThat(softwareManagement.findSoftwareModuleTypeByName(NOT_EXIST_ID)).isNotPresent();

        assertThat(softwareManagement.findSoftwareModuleByNameAndVersion(NOT_EXIST_ID, NOT_EXIST_ID, osType.getId()))
                .isNotPresent();

        assertThat(softwareManagement.findSoftwareModuleMetadata(module.getId(), NOT_EXIST_ID)).isNotPresent();
    }

    @Test
    @Description("Verifies that management queries react as specfied on calls for non existing entities "
            + " by means of throwing EntityNotFoundException.")
    @ExpectEvents({ @Expect(type = SoftwareModuleCreatedEvent.class, count = 1) })
    public void entityQueriesReferringToNotExistingEntitiesThrowsException() {
        final SoftwareModule module = testdataFactory.createSoftwareModuleApp();

        verifyThrownExceptionBy(
                () -> softwareManagement.createSoftwareModule(
                        Lists.newArrayList(entityFactory.softwareModule().create().name("xxx").type(NOT_EXIST_ID))),
                "SoftwareModuleType");
        verifyThrownExceptionBy(
                () -> softwareManagement
                        .createSoftwareModule(entityFactory.softwareModule().create().name("xxx").type(NOT_EXIST_ID)),
                "SoftwareModuleType");

        verifyThrownExceptionBy(() -> softwareManagement.createSoftwareModuleMetadata(NOT_EXIST_IDL,
                entityFactory.generateMetadata("xxx", "xxx")), "SoftwareModule");
        verifyThrownExceptionBy(() -> softwareManagement.createSoftwareModuleMetadata(NOT_EXIST_IDL,
                Lists.newArrayList(entityFactory.generateMetadata("xxx", "xxx"))), "SoftwareModule");

        verifyThrownExceptionBy(() -> softwareManagement.deleteSoftwareModule(NOT_EXIST_IDL), "SoftwareModule");
        verifyThrownExceptionBy(() -> softwareManagement.deleteSoftwareModules(Lists.newArrayList(NOT_EXIST_IDL)),
                "SoftwareModule");
        verifyThrownExceptionBy(() -> softwareManagement.deleteSoftwareModuleMetadata(NOT_EXIST_IDL, "xxx"),
                "SoftwareModule");
        verifyThrownExceptionBy(() -> softwareManagement.deleteSoftwareModuleMetadata(module.getId(), NOT_EXIST_ID),
                "SoftwareModuleMetadata");

        verifyThrownExceptionBy(() -> softwareManagement.updateSoftwareModuleMetadata(NOT_EXIST_IDL,
                entityFactory.generateMetadata("xxx", "xxx")), "SoftwareModule");
        verifyThrownExceptionBy(() -> softwareManagement.updateSoftwareModuleMetadata(module.getId(),
                entityFactory.generateMetadata(NOT_EXIST_ID, "xxx")), "SoftwareModuleMetadata");

        verifyThrownExceptionBy(() -> softwareManagement.deleteSoftwareModuleType(NOT_EXIST_IDL), "SoftwareModuleType");

        verifyThrownExceptionBy(() -> softwareManagement.findSoftwareModuleByAssignedTo(PAGE, NOT_EXIST_IDL),
                "DistributionSet");

        verifyThrownExceptionBy(
                () -> softwareManagement.findSoftwareModuleByNameAndVersion("xxx", "xxx", NOT_EXIST_IDL),
                "SoftwareModuleType");

        verifyThrownExceptionBy(() -> softwareManagement.findSoftwareModuleMetadata(NOT_EXIST_IDL, NOT_EXIST_ID),
                "SoftwareModule");

        verifyThrownExceptionBy(
                () -> softwareManagement.findSoftwareModuleMetadataBySoftwareModuleId(PAGE, NOT_EXIST_IDL),
                "SoftwareModule");
        verifyThrownExceptionBy(() -> softwareManagement.findSoftwareModuleMetadataBySoftwareModuleId(NOT_EXIST_IDL,
                "name==*", PAGE), "SoftwareModule");
        verifyThrownExceptionBy(() -> softwareManagement.findSoftwareModulesByType(PAGE, NOT_EXIST_IDL),
                "SoftwareModule");

        verifyThrownExceptionBy(
                () -> softwareManagement.updateSoftwareModule(entityFactory.softwareModule().update(NOT_EXIST_IDL)),
                "SoftwareModule");
        verifyThrownExceptionBy(
                () -> softwareManagement.updateSoftwareModuleType(entityFactory.softwareModuleType().update(1234L)),
                "SoftwareModuleType");
    }

    @Test
    @Description("Calling update without changing fields results in no recorded change in the repository including unchanged audit fields.")
    public void updateNothingResultsInUnchangedRepositoryForType() {
        final SoftwareModuleType created = softwareManagement.createSoftwareModuleType(
                entityFactory.softwareModuleType().create().key("test-key").name("test-name"));

        final SoftwareModuleType updated = softwareManagement
                .updateSoftwareModuleType(entityFactory.softwareModuleType().update(created.getId()));

        assertThat(updated.getOptLockRevision())
                .as("Expected version number of updated entitity to be equal to created version")
                .isEqualTo(created.getOptLockRevision());
    }

    @Test
    @Description("Calling update for changed fields results in change in the repository.")
    public void updateSoftareModuleTypeFieldsToNewValue() {
        final SoftwareModuleType created = softwareManagement.createSoftwareModuleType(
                entityFactory.softwareModuleType().create().key("test-key").name("test-name"));

        final SoftwareModuleType updated = softwareManagement.updateSoftwareModuleType(
                entityFactory.softwareModuleType().update(created.getId()).description("changed").colour("changed"));

        assertThat(updated.getOptLockRevision()).as("Expected version number of updated entitity is")
                .isEqualTo(created.getOptLockRevision() + 1);
        assertThat(updated.getDescription()).as("Updated description is").isEqualTo("changed");
        assertThat(updated.getColour()).as("Updated vendor is").isEqualTo("changed");
    }

    @Test
    @Description("Calling update without changing fields results in no recorded change in the repository including unchanged audit fields.")
    public void updateNothingResultsInUnchangedRepository() {
        final SoftwareModule ah = testdataFactory.createSoftwareModuleOs();

        final SoftwareModule updated = softwareManagement
                .updateSoftwareModule(entityFactory.softwareModule().update(ah.getId()));

        assertThat(updated.getOptLockRevision())
                .as("Expected version number of updated entitity to be equal to created version")
                .isEqualTo(ah.getOptLockRevision());
    }

    @Test
    @Description("Calling update for changed fields results in change in the repository.")
    public void updateSoftareModuleFieldsToNewValue() {
        final SoftwareModule ah = testdataFactory.createSoftwareModuleOs();

        final SoftwareModule updated = softwareManagement.updateSoftwareModule(
                entityFactory.softwareModule().update(ah.getId()).description("changed").vendor("changed"));

        assertThat(updated.getOptLockRevision()).as("Expected version number of updated entitity is")
                .isEqualTo(ah.getOptLockRevision() + 1);
        assertThat(updated.getDescription()).as("Updated description is").isEqualTo("changed");
        assertThat(updated.getVendor()).as("Updated vendor is").isEqualTo("changed");
    }

    @Test
    @Description("Create Software Module call fails when called for existing entity.")
    public void createModuleCallFailsForExistingModule() {
        testdataFactory.createSoftwareModuleOs();
        try {
            testdataFactory.createSoftwareModuleOs();
            fail("Should not have worked as module already exists.");
        } catch (final EntityAlreadyExistsException e) {

        }
    }

    @Test
    @Description("Create Software Module Types call fails when called for existing entities.")
    public void createModuleTypesCallFailsForExistingTypes() {
        final List<SoftwareModuleTypeCreate> created = Lists.newArrayList(
                entityFactory.softwareModuleType().create().key("test-key").name("test-name"),
                entityFactory.softwareModuleType().create().key("test-key2").name("test-name2"));

        softwareManagement.createSoftwareModuleType(created);
        try {
            softwareManagement.createSoftwareModuleType(created);
            fail("Should not have worked as module already exists.");
        } catch (final EntityAlreadyExistsException e) {

        }
    }

    @Test
    @Description("searched for software modules based on the various filter options, e.g. name,desc,type, version.")
    public void findSoftwareModuleByFilters() {
        final SoftwareModule ah = softwareManagement.createSoftwareModule(
                entityFactory.softwareModule().create().type(appType).name("agent-hub").version("1.0.1"));
        final SoftwareModule jvm = softwareManagement.createSoftwareModule(
                entityFactory.softwareModule().create().type(runtimeType).name("oracle-jre").version("1.7.2"));
        final SoftwareModule os = softwareManagement.createSoftwareModule(
                entityFactory.softwareModule().create().type(osType).name("poky").version("3.0.2"));

        final SoftwareModule ah2 = softwareManagement.createSoftwareModule(
                entityFactory.softwareModule().create().type(appType).name("agent-hub").version("1.0.2"));
        JpaDistributionSet ds = (JpaDistributionSet) distributionSetManagement
                .createDistributionSet(entityFactory.distributionSet().create().name("ds-1").version("1.0.1")
                        .type(standardDsType).modules(Lists.newArrayList(os.getId(), jvm.getId(), ah2.getId())));

        final JpaTarget target = (JpaTarget) testdataFactory.createTarget();
        ds = (JpaDistributionSet) assignSet(target, ds).getDistributionSet();

        // standard searches
        assertThat(softwareManagement.findSoftwareModuleByFilters(PAGE, "poky", osType.getId()).getContent())
                .hasSize(1);
        assertThat(softwareManagement.findSoftwareModuleByFilters(PAGE, "poky", osType.getId()).getContent().get(0))
                .isEqualTo(os);
        assertThat(softwareManagement.findSoftwareModuleByFilters(PAGE, "oracle%", runtimeType.getId()).getContent())
                .hasSize(1);
        assertThat(softwareManagement.findSoftwareModuleByFilters(PAGE, "oracle%", runtimeType.getId()).getContent()
                .get(0)).isEqualTo(jvm);
        assertThat(softwareManagement.findSoftwareModuleByFilters(PAGE, "1.0.1", appType.getId()).getContent())
                .hasSize(1);
        assertThat(
                softwareManagement.findSoftwareModuleByFilters(PAGE, "1.0.1", appType.getId()).getContent().get(0))
                        .isEqualTo(ah);
        assertThat(softwareManagement.findSoftwareModuleByFilters(PAGE, "1.0%", appType.getId()).getContent())
                .hasSize(2);

        // no we search with on entity marked as deleted
        softwareManagement.deleteSoftwareModule(
                softwareModuleRepository.findByAssignedToAndType(PAGE, ds, appType).getContent().get(0).getId());

        assertThat(softwareManagement.findSoftwareModuleByFilters(PAGE, "1.0%", appType.getId()).getContent())
                .hasSize(1);
        assertThat(softwareManagement.findSoftwareModuleByFilters(PAGE, "1.0%", appType.getId()).getContent().get(0))
                .isEqualTo(ah);
    }

    private Action assignSet(final JpaTarget target, final JpaDistributionSet ds) {
        assignDistributionSet(ds.getId(), target.getControllerId());
        assertThat(targetManagement.findTargetByControllerID(target.getControllerId()).get().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.PENDING);
        assertThat(deploymentManagement.getAssignedDistributionSet(target.getControllerId()).get()).isEqualTo(ds);
        final Action action = actionRepository.findByTargetAndDistributionSet(PAGE, target, ds).getContent().get(0);
        assertThat(action).isNotNull();
        return action;
    }

    @Test
    @Description("Searches for software modules based on a list of IDs.")
    public void findSoftwareModulesById() {

        final List<Long> modules = Lists.newArrayList(testdataFactory.createSoftwareModuleOs().getId(),
                testdataFactory.createSoftwareModuleApp().getId(), 624355263L);

        assertThat(softwareManagement.findSoftwareModulesById(modules)).hasSize(2);
    }

    @Test
    @Description("Searches for software modules by type.")
    public void findSoftwareModulesByType() {
        // found in test
        final SoftwareModule one = testdataFactory.createSoftwareModuleOs("one");
        final SoftwareModule two = testdataFactory.createSoftwareModuleOs("two");
        // ignored
        softwareManagement.deleteSoftwareModule(testdataFactory.createSoftwareModuleOs("deleted").getId());
        testdataFactory.createSoftwareModuleApp();

        assertThat(softwareManagement.findSoftwareModulesByType(PAGE, osType.getId()).getContent())
                .as("Expected to find the following number of modules:").hasSize(2).as("with the following elements")
                .contains(two, one);
    }

    @Test
    @Description("Counts all software modules in the repsitory that are not marked as deleted.")
    public void countSoftwareModulesAll() {
        // found in test
        final SoftwareModule one = testdataFactory.createSoftwareModuleOs("one");
        final SoftwareModule two = testdataFactory.createSoftwareModuleOs("two");
        final SoftwareModule deleted = testdataFactory.createSoftwareModuleOs("deleted");
        // ignored
        softwareManagement.deleteSoftwareModule(deleted.getId());

        assertThat(softwareManagement.countSoftwareModulesAll()).as("Expected to find the following number of modules:")
                .isEqualTo(2);
    }

    @Test
    @Description("Tests the successfull deletion of software module types. Both unused (hard delete) and used ones (soft delete).")
    public void deleteAssignedAndUnassignedSoftwareModuleTypes() {
        assertThat(softwareManagement.findSoftwareModuleTypesAll(PAGE)).hasSize(3).contains(osType, runtimeType,
                appType);

        SoftwareModuleType type = softwareManagement.createSoftwareModuleType(
                entityFactory.softwareModuleType().create().key("bundle").name("OSGi Bundle"));

        assertThat(softwareManagement.findSoftwareModuleTypesAll(PAGE)).hasSize(4).contains(osType, runtimeType,
                appType, type);

        // delete unassigned
        softwareManagement.deleteSoftwareModuleType(type.getId());
        assertThat(softwareManagement.findSoftwareModuleTypesAll(PAGE)).hasSize(3).contains(osType, runtimeType,
                appType);
        assertThat(softwareModuleTypeRepository.findAll()).hasSize(3).contains((JpaSoftwareModuleType) osType,
                (JpaSoftwareModuleType) runtimeType, (JpaSoftwareModuleType) appType);

        type = softwareManagement.createSoftwareModuleType(
                entityFactory.softwareModuleType().create().key("bundle2").name("OSGi Bundle2"));

        assertThat(softwareManagement.findSoftwareModuleTypesAll(PAGE)).hasSize(4).contains(osType, runtimeType,
                appType, type);

        softwareManagement.createSoftwareModule(
                entityFactory.softwareModule().create().type(type).name("Test SM").version("1.0"));

        // delete assigned
        softwareManagement.deleteSoftwareModuleType(type.getId());
        assertThat(softwareManagement.findSoftwareModuleTypesAll(PAGE)).hasSize(3).contains(osType, runtimeType,
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
        assertThat(softwareManagement.findSoftwareModuleById(unassignedModule.getId())).isNotPresent();

        // verify: binary data of artifact is deleted
        assertArtfiactNull(artifact1, artifact2);

        // verify: meta data of artifact is deleted
        assertThat(artifactRepository.findOne(artifact1.getId())).isNull();
        assertThat(artifactRepository.findOne(artifact2.getId())).isNull();
    }

    @Test
    @Description("Deletes an artifact, which is assigned to a DistributionSet")
    public void softDeleteOfAssignedArtifact() {

        // [STEP1]: Create SoftwareModuleX with ArtifactX
        SoftwareModule assignedModule = createSoftwareModuleWithArtifacts(osType, "moduleX", "3.0.2", 2);

        // [STEP2]: Assign SoftwareModule to DistributionSet
        testdataFactory.createDistributionSet(Sets.newHashSet(assignedModule));

        // [STEP3]: Delete the assigned SoftwareModule
        softwareManagement.deleteSoftwareModule(assignedModule.getId());

        // [VERIFY EXPECTED RESULT]:
        // verify: assignedModule is marked as deleted
        assignedModule = softwareManagement.findSoftwareModuleById(assignedModule.getId()).get();
        assertTrue("The module should be flagged as deleted", assignedModule.isDeleted());
        assertThat(softwareManagement.findSoftwareModulesAll(PAGE)).hasSize(0);
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

        // Init target
        final Target target = testdataFactory.createTarget();

        // [STEP1]: Create SoftwareModuleX and include the new ArtifactX
        SoftwareModule assignedModule = createSoftwareModuleWithArtifacts(osType, "moduleX", "3.0.2", 2);

        // [STEP2]: Assign SoftwareModule to DistributionSet
        final DistributionSet disSet = testdataFactory.createDistributionSet(Sets.newHashSet(assignedModule));

        // [STEP3]: Assign DistributionSet to a Device
        assignDistributionSet(disSet, Lists.newArrayList(target));

        // [STEP4]: Delete the DistributionSet
        distributionSetManagement.deleteDistributionSet(disSet.getId());

        // [STEP5]: Delete the assigned SoftwareModule
        softwareManagement.deleteSoftwareModule(assignedModule.getId());

        // [VERIFY EXPECTED RESULT]:
        // verify: assignedModule is marked as deleted
        assignedModule = softwareManagement.findSoftwareModuleById(assignedModule.getId()).get();
        assertTrue("The found module should be flagged deleted", assignedModule.isDeleted());
        assertThat(softwareManagement.findSoftwareModulesAll(PAGE)).hasSize(0);
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
        moduleX = softwareManagement.findSoftwareModuleById(moduleX.getId()).get();
        final Artifact artifactX = moduleX.getArtifacts().iterator().next();

        // [STEP3]: Create SoftwareModuleY and add the same ArtifactX
        SoftwareModule moduleY = createSoftwareModuleWithArtifacts(osType, "moduley", "v1.0", 0);

        // [STEP4]: Assign the same ArtifactX to SoftwareModuleY
        artifactManagement.createArtifact(new ByteArrayInputStream(source), moduleY.getId(), "artifactx", false);
        moduleY = softwareManagement.findSoftwareModuleById(moduleY.getId()).get();
        final Artifact artifactY = moduleY.getArtifacts().iterator().next();

        // [STEP5]: Delete SoftwareModuleX
        softwareManagement.deleteSoftwareModule(moduleX.getId());

        // [VERIFY EXPECTED RESULT]:
        // verify: SoftwareModuleX is deleted, and ModuelY still exists
        assertThat(softwareModuleRepository.findAll()).hasSize(1);
        assertThat(softwareManagement.findSoftwareModuleById(moduleX.getId())).isNotPresent();
        assertThat(softwareManagement.findSoftwareModuleById(moduleY.getId())).isPresent();

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
        final Target target = testdataFactory.createTarget();

        // [STEP1]: Create SoftwareModuleX and add a new ArtifactX
        SoftwareModule moduleX = createSoftwareModuleWithArtifacts(osType, "modulex", "v1.0", 0);

        artifactManagement.createArtifact(new ByteArrayInputStream(source), moduleX.getId(), "artifactx", false);
        moduleX = softwareManagement.findSoftwareModuleById(moduleX.getId()).get();
        final Artifact artifactX = moduleX.getArtifacts().iterator().next();

        // [STEP2]: Create SoftwareModuleY and add the same ArtifactX
        SoftwareModule moduleY = createSoftwareModuleWithArtifacts(osType, "moduley", "v1.0", 0);

        artifactManagement.createArtifact(new ByteArrayInputStream(source), moduleY.getId(), "artifactx", false);
        moduleY = softwareManagement.findSoftwareModuleById(moduleY.getId()).get();
        final Artifact artifactY = moduleY.getArtifacts().iterator().next();

        // [STEP3]: Assign SoftwareModuleX to DistributionSetX and to target
        final DistributionSet disSetX = testdataFactory.createDistributionSet(Sets.newHashSet(moduleX), "X");
        assignDistributionSet(disSetX, Lists.newArrayList(target));

        // [STEP4]: Assign SoftwareModuleY to DistributionSet and to target
        final DistributionSet disSetY = testdataFactory.createDistributionSet(Sets.newHashSet(moduleY), "Y");
        assignDistributionSet(disSetY, Lists.newArrayList(target));

        // [STEP5]: Delete SoftwareModuleX
        softwareManagement.deleteSoftwareModule(moduleX.getId());

        // [STEP6]: Delete SoftwareModuleY
        softwareManagement.deleteSoftwareModule(moduleY.getId());

        // [VERIFY EXPECTED RESULT]:
        moduleX = softwareManagement.findSoftwareModuleById(moduleX.getId()).get();
        moduleY = softwareManagement.findSoftwareModuleById(moduleY.getId()).get();

        // verify: SoftwareModuleX and SofwtareModule are marked as deleted
        assertThat(moduleX).isNotNull();
        assertThat(moduleY).isNotNull();
        assertTrue("The module should be flagged deleted", moduleX.isDeleted());
        assertTrue("The module should be flagged deleted", moduleY.isDeleted());
        assertThat(softwareManagement.findSoftwareModulesAll(PAGE)).hasSize(0);
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
        SoftwareModule softwareModule = softwareManagement.createSoftwareModule(entityFactory.softwareModule().create()
                .type(type).name(name).version(version).description("description of artifact " + name));

        for (int i = 0; i < numberArtifacts; i++) {
            artifactManagement.createArtifact(new RandomGeneratedInputStream(5 * 1024), softwareModule.getId(),
                    "file" + (i + 1), false);
        }

        // Verify correct Creation of SoftwareModule and corresponding artifacts
        softwareModule = softwareManagement.findSoftwareModuleById(softwareModule.getId()).get();
        assertThat(softwareModuleRepository.findAll()).hasSize((int) countSoftwareModule + 1);

        final List<Artifact> artifacts = softwareModule.getArtifacts();

        assertThat(artifacts).hasSize(numberArtifacts);
        if (numberArtifacts != 0) {
            assertArtfiactNotNull(artifacts.toArray(new Artifact[artifacts.size()]));
        }

        artifacts.forEach(artifact -> assertThat(artifactRepository.findOne(artifact.getId())).isNotNull());
        return softwareModule;
    }

    private void assertArtfiactNotNull(final Artifact... results) {
        assertThat(artifactRepository.findAll()).hasSize(results.length);
        for (final Artifact result : results) {
            assertThat(result.getId()).isNotNull();
            assertThat(binaryArtifactRepository.getArtifactBySha1(((JpaArtifact) result).getSha1Hash())).isNotNull();
        }
    }

    private void assertArtfiactNull(final Artifact... results) {
        for (final Artifact result : results) {
            assertThat(binaryArtifactRepository.getArtifactBySha1(((JpaArtifact) result).getSha1Hash())).isNull();
        }
    }

    @Test
    @Description("Test verfies that results are returned based on given filter parameters and in the specified order.")
    public void findSoftwareModuleOrderByDistributionModuleNameAscModuleVersionAsc() {
        // test meta data
        final SoftwareModuleType testType = softwareManagement.createSoftwareModuleType(
                entityFactory.softwareModuleType().create().key("thetype").name("thename").maxAssignments(100));
        DistributionSetType testDsType = distributionSetManagement
                .createDistributionSetType(entityFactory.distributionSetType().create().key("key").name("name"));

        distributionSetManagement.assignMandatorySoftwareModuleTypes(testDsType.getId(),
                Lists.newArrayList(osType.getId()));
        testDsType = distributionSetManagement.assignOptionalSoftwareModuleTypes(testDsType.getId(),
                Lists.newArrayList(testType.getId()));

        // found in test
        final SoftwareModule unassigned = testdataFactory.createSoftwareModule("thetype", "unassignedfound");
        final SoftwareModule one = testdataFactory.createSoftwareModule("thetype", "bfound");
        final SoftwareModule two = testdataFactory.createSoftwareModule("thetype", "cfound");
        final SoftwareModule differentName = testdataFactory.createSoftwareModule("thetype", "a");

        // ignored
        final SoftwareModule deleted = testdataFactory.createSoftwareModule("thetype", "deleted");
        final SoftwareModule four = testdataFactory.createSoftwareModuleOs("e");

        final DistributionSet set = distributionSetManagement.createDistributionSet(
                entityFactory.distributionSet().create().name("set").version("1").type(testDsType).modules(Lists
                        .newArrayList(one.getId(), two.getId(), deleted.getId(), four.getId(), differentName.getId())));
        softwareManagement.deleteSoftwareModule(deleted.getId());

        // with filter on name, version and module type
        assertThat(softwareManagement.findSoftwareModuleOrderBySetAssignmentAndModuleNameAscModuleVersionAsc(PAGE,
                set.getId(), "%found%", testType.getId()).getContent())
                        .as("Found modules with given name, given module type and the assigned ones first")
                        .containsExactly(new AssignedSoftwareModule(one, true), new AssignedSoftwareModule(two, true),
                                new AssignedSoftwareModule(unassigned, false));

        // with filter on module type only
        assertThat(softwareManagement.findSoftwareModuleOrderBySetAssignmentAndModuleNameAscModuleVersionAsc(PAGE,
                set.getId(), null, testType.getId()).getContent())
                        .as("Found modules with given module type and the assigned ones first").containsExactly(
                                new AssignedSoftwareModule(differentName, true), new AssignedSoftwareModule(one, true),
                                new AssignedSoftwareModule(two, true), new AssignedSoftwareModule(unassigned, false));

        // without any filter
        assertThat(softwareManagement.findSoftwareModuleOrderBySetAssignmentAndModuleNameAscModuleVersionAsc(PAGE,
                set.getId(), null, null).getContent()).as("Found modules with the assigned ones first").containsExactly(
                        new AssignedSoftwareModule(differentName, true), new AssignedSoftwareModule(one, true),
                        new AssignedSoftwareModule(two, true), new AssignedSoftwareModule(four, true),
                        new AssignedSoftwareModule(unassigned, false));
    }

    @Test
    @Description("Checks that number of modules is returned as expected based on given filters.")
    public void countSoftwareModuleByFilters() {
        // test meta data
        final SoftwareModuleType testType = softwareManagement.createSoftwareModuleType(
                entityFactory.softwareModuleType().create().key("thetype").name("thename").maxAssignments(100));
        DistributionSetType testDsType = distributionSetManagement
                .createDistributionSetType(entityFactory.distributionSetType().create().key("key").name("name"));

        distributionSetManagement.assignMandatorySoftwareModuleTypes(testDsType.getId(),
                Lists.newArrayList(osType.getId()));
        testDsType = distributionSetManagement.assignOptionalSoftwareModuleTypes(testDsType.getId(),
                Lists.newArrayList(testType.getId()));

        // found in test
        testdataFactory.createSoftwareModule("thetype", "unassignedfound");
        final SoftwareModule one = testdataFactory.createSoftwareModule("thetype", "bfound");
        final SoftwareModule two = testdataFactory.createSoftwareModule("thetype", "cfound");
        final SoftwareModule differentName = testdataFactory.createSoftwareModule("thetype", "d");

        // ignored
        final SoftwareModule deleted = testdataFactory.createSoftwareModule("thetype", "deleted");
        final SoftwareModule four = testdataFactory.createSoftwareModuleOs("e");

        distributionSetManagement.createDistributionSet(
                entityFactory.distributionSet().create().name("set").version("1").type(testDsType).modules(Lists
                        .newArrayList(one.getId(), two.getId(), deleted.getId(), four.getId(), differentName.getId())));
        softwareManagement.deleteSoftwareModule(deleted.getId());

        // test
        assertThat(softwareManagement.countSoftwareModuleByFilters("%found%", testType.getId()))
                .as("Number of modules with given name or version and type").isEqualTo(3);
        assertThat(softwareManagement.countSoftwareModuleByFilters(null, testType.getId()))
                .as("Number of modules with given type").isEqualTo(4);
        assertThat(softwareManagement.countSoftwareModuleByFilters(null, null)).as("Number of modules overall")
                .isEqualTo(5);
    }

    @Test
    @Description("Verfies that all undeleted software modules are found in the repository.")
    public void countSoftwareModuleTypesAll() {
        testdataFactory.createSoftwareModuleOs();

        // one soft deleted
        final SoftwareModule deleted = testdataFactory.createSoftwareModuleApp();
        testdataFactory.createDistributionSet(Lists.newArrayList(deleted));
        softwareManagement.deleteSoftwareModule(deleted.getId());

        assertThat(softwareManagement.countSoftwareModulesAll()).as("Number of undeleted modules").isEqualTo(1);
        assertThat(softwareModuleRepository.count()).as("Number of all modules").isEqualTo(2);
    }

    @Test
    @Description("Checks that software module typeis found based on given name.")
    public void findSoftwareModuleTypeByName() {
        testdataFactory.createSoftwareModuleOs();
        final SoftwareModuleType found = softwareManagement
                .createSoftwareModuleType(entityFactory.softwareModuleType().create().key("thetype").name("thename"));
        softwareManagement.createSoftwareModuleType(
                entityFactory.softwareModuleType().create().key("thetype2").name("anothername"));

        assertThat(softwareManagement.findSoftwareModuleTypeByName("thename").get()).as("Type with given name")
                .isEqualTo(found);
    }

    @Test
    @Description("Verfies that it is not possible to create a type that alrady exists.")
    public void createSoftwareModuleTypeFailsWithExistingEntity() {
        final SoftwareModuleType created = softwareManagement
                .createSoftwareModuleType(entityFactory.softwareModuleType().create().key("thetype").name("thename"));
        try {
            softwareManagement.createSoftwareModuleType(
                    entityFactory.softwareModuleType().create().key("thetype").name("thename"));
            fail("should not have worked as module type already exists");
        } catch (final EntityAlreadyExistsException e) {

        }

    }

    @Test
    @Description("Verfies that it is not possible to create a list of types where one already exists.")
    public void createSoftwareModuleTypesFailsWithExistingEntity() {
        final SoftwareModuleType created = softwareManagement
                .createSoftwareModuleType(entityFactory.softwareModuleType().create().key("thetype").name("thename"));
        try {
            softwareManagement.createSoftwareModuleType(
                    Lists.newArrayList(entityFactory.softwareModuleType().create().key("thetype").name("thename"),
                            entityFactory.softwareModuleType().create().key("anothertype").name("anothername")));
            fail("should not have worked as module type already exists");
        } catch (final EntityAlreadyExistsException e) {

        }
    }

    @Test
    @Description("Verifies that the creation of a softwareModuleType is failing because of invalid max assignment")
    public void createSoftwareModuleTypesFailsWithInvalidMaxAssignment() {
        try {
            softwareManagement.createSoftwareModuleType(
                    entityFactory.softwareModuleType().create().key("type").name("name").maxAssignments(0));
            fail("should not have worked as max assignment is invalid. Should be greater than 0.");
        } catch (final ConstraintViolationException e) {

        }
    }

    @Test
    @Description("Verfies that multiple types are created as requested.")
    public void createMultipleSoftwareModuleTypes() {
        final List<SoftwareModuleType> created = softwareManagement.createSoftwareModuleType(
                Lists.newArrayList(entityFactory.softwareModuleType().create().key("thetype").name("thename"),
                        entityFactory.softwareModuleType().create().key("thetype2").name("thename2")));

        assertThat(created.size()).as("Number of created types").isEqualTo(2);
        assertThat(softwareManagement.countSoftwareModuleTypesAll()).as("Number of types in repository").isEqualTo(5);
    }

    @Test
    @Description("Verfies that software modules are resturned that are assigned to given DS.")
    public void findSoftwareModuleByAssignedTo() {
        // test modules
        final SoftwareModule one = testdataFactory.createSoftwareModuleOs();
        testdataFactory.createSoftwareModuleOs("notassigned");

        // one soft deleted
        final SoftwareModule deleted = testdataFactory.createSoftwareModuleApp();
        final DistributionSet set = distributionSetManagement.createDistributionSet(entityFactory.distributionSet()
                .create().name("set").version("1").modules(Lists.newArrayList(one.getId(), deleted.getId())));
        softwareManagement.deleteSoftwareModule(deleted.getId());

        assertThat(softwareManagement.findSoftwareModuleByAssignedTo(PAGE, set.getId()).getContent())
                .as("Found this number of modules").hasSize(2);
    }

    @Test
    @Description("Checks that metadata for a software module can be created.")
    public void createSoftwareModuleMetadata() {

        final String knownKey1 = "myKnownKey1";
        final String knownValue1 = "myKnownValue1";

        final String knownKey2 = "myKnownKey2";
        final String knownValue2 = "myKnownValue2";

        final SoftwareModule ah = testdataFactory.createSoftwareModuleApp();

        assertThat(ah.getOptLockRevision()).isEqualTo(1);

        final SoftwareModuleMetadata swMetadata1 = new JpaSoftwareModuleMetadata(knownKey1, ah, knownValue1);

        final SoftwareModuleMetadata swMetadata2 = new JpaSoftwareModuleMetadata(knownKey2, ah, knownValue2);

        final List<SoftwareModuleMetadata> softwareModuleMetadata = softwareManagement
                .createSoftwareModuleMetadata(ah.getId(), Lists.newArrayList(swMetadata1, swMetadata2));

        final SoftwareModule changedLockRevisionModule = softwareManagement.findSoftwareModuleById(ah.getId()).get();
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

        final SoftwareModule ah = testdataFactory.createSoftwareModuleApp();

        softwareManagement.createSoftwareModuleMetadata(ah.getId(),
                entityFactory.generateMetadata(knownKey1, knownValue1));

        try {
            softwareManagement.createSoftwareModuleMetadata(ah.getId(),
                    entityFactory.generateMetadata(knownKey1, knownValue2));
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
        final SoftwareModule ah = testdataFactory.createSoftwareModuleApp();
        // initial opt lock revision must be 1
        assertThat(ah.getOptLockRevision()).isEqualTo(1);

        // create an software module meta data entry
        final List<SoftwareModuleMetadata> softwareModuleMetadata = softwareManagement.createSoftwareModuleMetadata(
                ah.getId(), Collections.singleton(entityFactory.generateMetadata(knownKey, knownValue)));
        assertThat(softwareModuleMetadata).hasSize(1);
        // base software module should have now the opt lock revision one
        // because we are modifying the
        // base software module
        SoftwareModule changedLockRevisionModule = softwareManagement.findSoftwareModuleById(ah.getId()).get();
        assertThat(changedLockRevisionModule.getOptLockRevision()).isEqualTo(2);

        // update the software module metadata
        Thread.sleep(100);
        final SoftwareModuleMetadata updated = softwareManagement.updateSoftwareModuleMetadata(ah.getId(),
                entityFactory.generateMetadata(knownKey, knownUpdateValue));
        // we are updating the sw meta data so also modiying the base software
        // module so opt lock
        // revision must be two
        changedLockRevisionModule = softwareManagement.findSoftwareModuleById(ah.getId()).get();
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

        SoftwareModule ah = testdataFactory.createSoftwareModuleApp();

        ah = softwareManagement
                .createSoftwareModuleMetadata(ah.getId(), entityFactory.generateMetadata(knownKey1, knownValue1))
                .getSoftwareModule();

        assertThat(softwareManagement.findSoftwareModuleMetadataBySoftwareModuleId(new PageRequest(0, 100), ah.getId())
                .getContent()).as("Contains the created metadata element")
                        .containsExactly(new JpaSoftwareModuleMetadata(knownKey1, ah, knownValue1));

        softwareManagement.deleteSoftwareModuleMetadata(ah.getId(), knownKey1);
        assertThat(softwareManagement.findSoftwareModuleMetadataBySoftwareModuleId(new PageRequest(0, 100), ah.getId())
                .getContent()).as("Metadata elemenets are").isEmpty();
    }

    @Test
    @Description("Verfies that non existing metadata find results in exception.")
    public void findSoftwareModuleMetadataFailsIfEntryDoesNotExist() {
        final String knownKey1 = "myKnownKey1";
        final String knownValue1 = "myKnownValue1";

        SoftwareModule ah = testdataFactory.createSoftwareModuleApp();

        ah = softwareManagement
                .createSoftwareModuleMetadata(ah.getId(), entityFactory.generateMetadata(knownKey1, knownValue1))
                .getSoftwareModule();

        assertThat(softwareManagement.findSoftwareModuleMetadata(ah.getId(), "doesnotexist")).isNotPresent();
    }

    @Test
    @Description("Queries and loads the metadata related to a given software module.")
    public void findAllSoftwareModuleMetadataBySwId() {

        SoftwareModule sw1 = testdataFactory.createSoftwareModuleApp();

        SoftwareModule sw2 = testdataFactory.createSoftwareModuleOs();

        for (int index = 0; index < 10; index++) {
            sw1 = softwareManagement.createSoftwareModuleMetadata(sw1.getId(),
                    entityFactory.generateMetadata("key" + index, "value" + index)).getSoftwareModule();
        }

        for (int index = 0; index < 20; index++) {
            sw2 = softwareManagement.createSoftwareModuleMetadata(sw2.getId(),
                    new JpaSoftwareModuleMetadata("key" + index, sw2, "value" + index)).getSoftwareModule();
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
