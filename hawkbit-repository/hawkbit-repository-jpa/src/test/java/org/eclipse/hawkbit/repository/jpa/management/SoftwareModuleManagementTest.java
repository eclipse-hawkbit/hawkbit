/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.RandomUtils;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleMetadataCreate;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.LockedException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.RandomGeneratedInputStream;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet_;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget_;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.ArtifactUpload;
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
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Component Tests - Repository")
@Story("Software Module Management")
public class SoftwareModuleManagementTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Verifies that management get access reacts as specified on calls for non existing entities by means "
            + "of Optional not present.")
    @ExpectEvents({ @Expect(type = SoftwareModuleCreatedEvent.class, count = 1) })
    public void nonExistingEntityAccessReturnsNotPresent() {
        final SoftwareModule module = testdataFactory.createSoftwareModuleApp();

        assertThat(softwareModuleManagement.get(1234L)).isNotPresent();

        assertThat(softwareModuleManagement.getByNameAndVersionAndType(NOT_EXIST_ID, NOT_EXIST_ID, osType.getId()))
                .isNotPresent();

        assertThat(softwareModuleManagement.getMetaDataBySoftwareModuleId(module.getId(), NOT_EXIST_ID)).isNotPresent();
    }

    @Test
    @Description("Verifies that management queries react as specfied on calls for non existing entities "
            + " by means of throwing EntityNotFoundException.")
    @ExpectEvents({ @Expect(type = SoftwareModuleCreatedEvent.class, count = 1) })
    public void entityQueriesReferringToNotExistingEntitiesThrowsException() {
        final SoftwareModule module = testdataFactory.createSoftwareModuleApp();

        verifyThrownExceptionBy(
                () -> softwareModuleManagement.create(Collections
                        .singletonList(entityFactory.softwareModule().create().name("xxx").type(NOT_EXIST_ID))),
                "SoftwareModuleType");
        verifyThrownExceptionBy(
                () -> softwareModuleManagement
                        .create(entityFactory.softwareModule().create().name("xxx").type(NOT_EXIST_ID)),
                "SoftwareModuleType");

        verifyThrownExceptionBy(
                () -> softwareModuleManagement.createMetaData(
                        entityFactory.softwareModuleMetadata().create(NOT_EXIST_IDL).key("xxx").value("xxx")),
                "SoftwareModule");
        verifyThrownExceptionBy(
                () -> softwareModuleManagement.createMetaData(Collections.singletonList(
                        entityFactory.softwareModuleMetadata().create(NOT_EXIST_IDL).key("xxx").value("xxx"))),
                "SoftwareModule");

        verifyThrownExceptionBy(() -> softwareModuleManagement.delete(NOT_EXIST_IDL), "SoftwareModule");
        verifyThrownExceptionBy(() -> softwareModuleManagement.delete(Collections.singletonList(NOT_EXIST_IDL)),
                "SoftwareModule");
        verifyThrownExceptionBy(() -> softwareModuleManagement.deleteMetaData(NOT_EXIST_IDL, "xxx"), "SoftwareModule");
        verifyThrownExceptionBy(() -> softwareModuleManagement.deleteMetaData(module.getId(), NOT_EXIST_ID),
                "SoftwareModuleMetadata");

        verifyThrownExceptionBy(
                () -> softwareModuleManagement.updateMetaData(
                        entityFactory.softwareModuleMetadata().update(NOT_EXIST_IDL, "xxx").value("xxx")),
                "SoftwareModule");
        verifyThrownExceptionBy(
                () -> softwareModuleManagement.updateMetaData(
                        entityFactory.softwareModuleMetadata().update(module.getId(), NOT_EXIST_ID).value("xxx")),
                "SoftwareModuleMetadata");

        verifyThrownExceptionBy(() -> softwareModuleManagement.findByAssignedTo(PAGE, NOT_EXIST_IDL),
                "DistributionSet");

        verifyThrownExceptionBy(() -> softwareModuleManagement.getByNameAndVersionAndType("xxx", "xxx", NOT_EXIST_IDL),
                "SoftwareModuleType");

        verifyThrownExceptionBy(
                () -> softwareModuleManagement.getMetaDataBySoftwareModuleId(NOT_EXIST_IDL, NOT_EXIST_ID),
                "SoftwareModule");

        verifyThrownExceptionBy(() -> softwareModuleManagement.findMetaDataBySoftwareModuleId(PAGE, NOT_EXIST_IDL),
                "SoftwareModule");
        verifyThrownExceptionBy(() -> softwareModuleManagement.findMetaDataByRsql(PAGE, NOT_EXIST_IDL, "name==*"),
                "SoftwareModule");
        verifyThrownExceptionBy(() -> softwareModuleManagement.findByType(PAGE, NOT_EXIST_IDL), "SoftwareModule");

        verifyThrownExceptionBy(
                () -> softwareModuleManagement.update(entityFactory.softwareModule().update(NOT_EXIST_IDL)),
                "SoftwareModule");
    }

    @Test
    @Description("Calling update without changing fields results in no recorded change in the repository including unchanged audit fields.")
    public void updateNothingResultsInUnchangedRepository() {
        final SoftwareModule ah = testdataFactory.createSoftwareModuleOs();

        final SoftwareModule updated = softwareModuleManagement
                .update(entityFactory.softwareModule().update(ah.getId()));

        assertThat(updated.getOptLockRevision())
                .as("Expected version number of updated entity to be equal to created version")
                .isEqualTo(ah.getOptLockRevision());
    }

    @Test
    @Description("Calling update for changed fields results in change in the repository.")
    public void updateSoftwareModuleFieldsToNewValue() {
        final SoftwareModule ah = testdataFactory.createSoftwareModuleOs();

        final SoftwareModule updated = softwareModuleManagement
                .update(entityFactory.softwareModule().update(ah.getId()).description("changed").vendor("changed"));

        assertThat(updated.getOptLockRevision()).as("Expected version number of updated entitity is")
                .isEqualTo(ah.getOptLockRevision() + 1);
        assertThat(updated.getDescription()).as("Updated description is").isEqualTo("changed");
        assertThat(updated.getVendor()).as("Updated vendor is").isEqualTo("changed");
    }

    @Test
    @Description("Create Software Module call fails when called for existing entity.")
    public void createModuleCallFailsForExistingModule() {
        testdataFactory.createSoftwareModuleOs();
        assertThatExceptionOfType(EntityAlreadyExistsException.class)
                .as("Should not have worked as module already exists.")
                .isThrownBy(() -> testdataFactory.createSoftwareModuleOs());
    }

    @Test
    @Description("searched for software modules based on the various filter options, e.g. name,desc,type, version.")
    public void findSoftwareModuleByFilters() {
        final SoftwareModule ah = softwareModuleManagement
                .create(entityFactory.softwareModule().create().type(appType).name("agent-hub").version("1.0.1"));
        final SoftwareModule jvm = softwareModuleManagement
                .create(entityFactory.softwareModule().create().type(runtimeType).name("oracle-jre").version("1.7.2"));
        final SoftwareModule os = softwareModuleManagement
                .create(entityFactory.softwareModule().create().type(osType).name("poky").version("3.0.2"));

        final SoftwareModule ah2 = softwareModuleManagement
                .create(entityFactory.softwareModule().create().type(appType).name("agent-hub").version("1.0.2"));
        JpaDistributionSet ds = (JpaDistributionSet) distributionSetManagement
                .create(entityFactory.distributionSet().create().name("ds-1").version("1.0.1").type(standardDsType)
                        .modules(Arrays.asList(os.getId(), jvm.getId(), ah2.getId())));

        final JpaTarget target = (JpaTarget) testdataFactory.createTarget();
        ds = (JpaDistributionSet) assignSet(target, ds).getDistributionSet();
        implicitLock(os);
        implicitLock(jvm);

        // standard searches
        assertThat(softwareModuleManagement.findByTextAndType(PAGE, "poky", osType.getId()).getContent()).hasSize(1);
        assertThat(softwareModuleManagement.findByTextAndType(PAGE, "poky", osType.getId()).getContent().get(0))
                .isEqualTo(os);
        assertThat(softwareModuleManagement.findByTextAndType(PAGE, "oracle", runtimeType.getId()).getContent())
                .hasSize(1);
        assertThat(
                softwareModuleManagement.findByTextAndType(PAGE, "oracle", runtimeType.getId()).getContent().get(0))
                .isEqualTo(jvm);
        assertThat(softwareModuleManagement.findByTextAndType(PAGE, ":1.0.1", appType.getId()).getContent()).hasSize(1)
                .first().isEqualTo(ah);
        assertThat(softwareModuleManagement.findByTextAndType(PAGE, ":1.0", appType.getId()).getContent()).hasSize(2);

        softwareModuleManagement.delete(ah2.getId());

        assertThat(softwareModuleManagement.findByTextAndType(PAGE, ":1.0", appType.getId()).getContent()).hasSize(1);
        assertThat(softwareModuleManagement.findByTextAndType(PAGE, ":1.0", appType.getId()).getContent().get(0))
                .isEqualTo(ah);
    }

    private Action assignSet(final JpaTarget target, final JpaDistributionSet ds) {
        assignDistributionSet(ds.getId(), target.getControllerId());
        implicitLock(ds);
        assertThat(targetManagement.getByControllerID(target.getControllerId()).get().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.PENDING);
        final Optional<DistributionSet> assignedDistributionSet = deploymentManagement
                .getAssignedDistributionSet(target.getControllerId());
        assertThat(assignedDistributionSet).contains(ds);
        final Action action = actionRepository
                .findAll(
                        (root, query, cb) ->
                                cb.and(
                                        cb.equal(root.get(JpaAction_.target).get(JpaTarget_.id), target.getId()),
                                        cb.equal(root.get(JpaAction_.distributionSet).get(JpaDistributionSet_.id), ds.getId())),
                        PAGE).getContent().get(0);;
        assertThat(action).isNotNull();
        return action;
    }

    @Test
    @Description("Searches for software modules based on a list of IDs.")
    public void findSoftwareModulesById() {

        final List<Long> modules = Arrays.asList(testdataFactory.createSoftwareModuleOs().getId(),
                testdataFactory.createSoftwareModuleApp().getId(), 624355263L);

        assertThat(softwareModuleManagement.get(modules)).hasSize(2);
    }

    @Test
    @Description("Searches for software modules by type.")
    public void findSoftwareModulesByType() {
        // found in test
        final SoftwareModule one = testdataFactory.createSoftwareModuleOs("one");
        final SoftwareModule two = testdataFactory.createSoftwareModuleOs("two");
        // ignored
        softwareModuleManagement.delete(testdataFactory.createSoftwareModuleOs("deleted").getId());
        testdataFactory.createSoftwareModuleApp();

        assertThat(softwareModuleManagement.findByType(PAGE, osType.getId()).getContent())
                .as("Expected to find the following number of modules:").hasSize(2).as("with the following elements")
                .contains(two, one);
    }

    @Test
    @Description("Counts all software modules in the repsitory that are not marked as deleted.")
    public void countSoftwareModulesAll() {
        // found in test
        testdataFactory.createSoftwareModuleOs("one");
        testdataFactory.createSoftwareModuleOs("two");
        final SoftwareModule deleted = testdataFactory.createSoftwareModuleOs("deleted");
        // ignored
        softwareModuleManagement.delete(deleted.getId());

        assertThat(softwareModuleManagement.count()).as("Expected to find the following number of modules:")
                .isEqualTo(2);
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
        softwareModuleManagement.delete(unassignedModule.getId());

        // [VERIFY EXPECTED RESULT]:
        // verify: SoftwareModule is deleted
        assertThat(softwareModuleRepository.findAll()).isEmpty();
        assertThat(softwareModuleManagement.get(unassignedModule.getId())).isNotPresent();

        // verify: binary data of artifact is deleted
        assertArtifactNull(artifact1, artifact2);

        // verify: metadata of artifact is deleted
        assertThat(artifactRepository.findById(artifact1.getId())).isNotPresent();
        assertThat(artifactRepository.findById(artifact2.getId())).isNotPresent();
    }

    @Test
    @Description("Deletes an artifact, which is assigned to a DistributionSet")
    public void softDeleteOfAssignedArtifact() {

        // [STEP1]: Create SoftwareModuleX with ArtifactX
        SoftwareModule assignedModule = createSoftwareModuleWithArtifacts(osType, "moduleX", "3.0.2", 2);

        // [STEP2]: Assign SoftwareModule to DistributionSet
        testdataFactory.createDistributionSet(Set.of(assignedModule));

        // [STEP3]: Delete the assigned SoftwareModule
        softwareModuleManagement.delete(assignedModule.getId());

        // [VERIFY EXPECTED RESULT]:
        // verify: assignedModule is marked as deleted
        assignedModule = softwareModuleManagement.get(assignedModule.getId()).get();
        assertTrue(assignedModule.isDeleted(), "The module should be flagged as deleted");
        assertThat(softwareModuleManagement.findAll(PAGE)).isEmpty();
        assertThat(softwareModuleManagement.findByRsql(PAGE, "name==*")).isEmpty();
        assertThat(softwareModuleManagement.count()).isZero();
        assertThat(softwareModuleRepository.findAll()).hasSize(1);

        // verify: binary data is deleted
        final Iterator<Artifact> artifactsIt = assignedModule.getArtifacts().iterator();
        final Artifact artifact1 = artifactsIt.next();
        final Artifact artifact2 = artifactsIt.next();
        assertArtifactNull(artifact1, artifact2);

        // verify: artifact meta data is still available
        assertThat(artifactRepository.findById(artifact1.getId())).isNotNull();
        assertThat(artifactRepository.findById(artifact2.getId())).isNotNull();
    }

    @Test
    @Description("Delete an artifact, which has been assigned to a rolled out DistributionSet in the past")
    public void softDeleteOfHistoricalAssignedArtifact() {

        // Init target
        final Target target = testdataFactory.createTarget();

        // [STEP1]: Create SoftwareModuleX and include the new ArtifactX
        SoftwareModule assignedModule = createSoftwareModuleWithArtifacts(osType, "moduleX", "3.0.2", 2);

        // [STEP2]: Assign SoftwareModule to DistributionSet
        final DistributionSet disSet = testdataFactory.createDistributionSet(Set.of(assignedModule));

        // [STEP3]: Assign DistributionSet to a Device
        assignDistributionSet(disSet, Collections.singletonList(target));

        // [STEP4]: Delete the DistributionSet
        distributionSetManagement.delete(disSet.getId());

        // [STEP5]: Delete the assigned SoftwareModule
        softwareModuleManagement.delete(assignedModule.getId());

        // [VERIFY EXPECTED RESULT]:
        // verify: assignedModule is marked as deleted
        assignedModule = softwareModuleManagement.get(assignedModule.getId()).get();
        assertTrue(assignedModule.isDeleted(), "The found module should be flagged deleted");
        assertThat(softwareModuleManagement.findAll(PAGE)).isEmpty();
        assertThat(softwareModuleRepository.findAll()).hasSize(1);

        // verify: binary data is deleted
        final Iterator<Artifact> artifactsIt = assignedModule.getArtifacts().iterator();
        final Artifact artifact1 = artifactsIt.next();
        final Artifact artifact2 = artifactsIt.next();
        assertArtifactNull(artifact1, artifact2);

        // verify: artifact meta data is still available
        assertThat(artifactRepository.findById(artifact1.getId())).isNotNull();
        assertThat(artifactRepository.findById(artifact2.getId())).isNotNull();
    }

    @Test
    @Description("Delete an software module with an artifact, which is also used by another software module.")
    public void deleteSoftwareModulesWithSharedArtifact() {

        // Init artifact binary data, target and DistributionSets
        final int artifactSize = 1024;
        final byte[] source = RandomUtils.nextBytes(artifactSize);

        // [STEP1]: Create SoftwareModuleX and add a new ArtifactX
        SoftwareModule moduleX = createSoftwareModuleWithArtifacts(osType, "modulex", "v1.0", 0);

        // [STEP2]: Create newArtifactX and add it to SoftwareModuleX
        artifactManagement.create(new ArtifactUpload(new ByteArrayInputStream(source), moduleX.getId(), "artifactx",
                false, artifactSize));
        moduleX = softwareModuleManagement.get(moduleX.getId()).get();
        final Artifact artifactX = moduleX.getArtifacts().iterator().next();

        // [STEP3]: Create SoftwareModuleY and add the same ArtifactX
        SoftwareModule moduleY = createSoftwareModuleWithArtifacts(osType, "moduley", "v1.0", 0);

        // [STEP4]: Assign the same ArtifactX to SoftwareModuleY
        artifactManagement.create(new ArtifactUpload(new ByteArrayInputStream(source), moduleY.getId(), "artifactx",
                false, artifactSize));
        moduleY = softwareModuleManagement.get(moduleY.getId()).get();
        final Artifact artifactY = moduleY.getArtifacts().iterator().next();

        // [STEP5]: Delete SoftwareModuleX
        softwareModuleManagement.delete(moduleX.getId());

        // [VERIFY EXPECTED RESULT]:
        // verify: SoftwareModuleX is deleted, and ModuelY still exists
        assertThat(softwareModuleRepository.findAll()).hasSize(1);
        assertThat(softwareModuleManagement.get(moduleX.getId())).isNotPresent();
        assertThat(softwareModuleManagement.get(moduleY.getId())).isPresent();

        // verify: binary data of artifact is not deleted
        assertArtifactNotNull(artifactY);

        // verify: meta data of artifactX is deleted
        assertThat(artifactRepository.findById(artifactX.getId())).isNotPresent();

        // verify: meta data of artifactY is not deleted
        assertThat(artifactRepository.findById(artifactY.getId())).isPresent();

    }

    @Test
    @Description("Delete two assigned softwaremodules which share an artifact.")
    public void deleteMultipleSoftwareModulesWhichShareAnArtifact() throws IOException {

        // Init artifact binary data, target and DistributionSets
        final int artifactSize = 1024;
        final byte[] source = RandomUtils.nextBytes(artifactSize);
        final Target target = testdataFactory.createTarget();

        // [STEP1]: Create SoftwareModuleX and add a new ArtifactX
        SoftwareModule moduleX = createSoftwareModuleWithArtifacts(osType, "modulex", "v1.0", 0);

        artifactManagement.create(new ArtifactUpload(new ByteArrayInputStream(source), moduleX.getId(), "artifactx",
                false, artifactSize));
        moduleX = softwareModuleManagement.get(moduleX.getId()).get();
        final Artifact artifactX = moduleX.getArtifacts().iterator().next();

        // [STEP2]: Create SoftwareModuleY and add the same ArtifactX
        SoftwareModule moduleY = createSoftwareModuleWithArtifacts(osType, "moduley", "v1.0", 0);

        artifactManagement.create(new ArtifactUpload(new ByteArrayInputStream(source), moduleY.getId(), "artifactx",
                false, artifactSize));
        moduleY = softwareModuleManagement.get(moduleY.getId()).get();
        final Artifact artifactY = moduleY.getArtifacts().iterator().next();

        // [STEP3]: Assign SoftwareModuleX to DistributionSetX and to target
        final DistributionSet disSetX = testdataFactory.createDistributionSet(Set.of(moduleX), "X");
        assignDistributionSet(disSetX, Collections.singletonList(target));

        // [STEP4]: Assign SoftwareModuleY to DistributionSet and to target
        final DistributionSet disSetY = testdataFactory.createDistributionSet(Set.of(moduleY), "Y");
        assignDistributionSet(disSetY, Collections.singletonList(target));

        // [STEP5]: Delete SoftwareModuleX
        softwareModuleManagement.delete(moduleX.getId());

        // [STEP6]: Delete SoftwareModuleY
        softwareModuleManagement.delete(moduleY.getId());

        // [VERIFY EXPECTED RESULT]:
        moduleX = softwareModuleManagement.get(moduleX.getId()).get();
        moduleY = softwareModuleManagement.get(moduleY.getId()).get();

        // verify: SoftwareModuleX and SoftwareModule are marked as deleted
        assertThat(moduleX).isNotNull();
        assertThat(moduleY).isNotNull();
        assertTrue(moduleX.isDeleted(), "The module should be flagged deleted");
        assertTrue(moduleY.isDeleted(), "The module should be flagged deleted");
        assertThat(softwareModuleManagement.findAll(PAGE)).isEmpty();
        assertThat(softwareModuleRepository.findAll()).hasSize(2);

        // verify: binary data of artifact is deleted
        assertArtifactNull(artifactX, artifactY);

        // verify: meta data of artifactX and artifactY is not deleted
        assertThat(artifactRepository.findById(artifactY.getId())).isNotNull();
    }

    private SoftwareModule createSoftwareModuleWithArtifacts(final SoftwareModuleType type, final String name,
            final String version, final int numberArtifacts) {

        final long countSoftwareModule = softwareModuleRepository.count();

        // create SoftwareModule
        SoftwareModule softwareModule = softwareModuleManagement.create(entityFactory.softwareModule().create()
                .type(type).name(name).version(version).description("description of artifact " + name));

        final int artifactSize = 5 * 1024;
        for (int i = 0; i < numberArtifacts; i++) {
            artifactManagement.create(new ArtifactUpload(new RandomGeneratedInputStream(artifactSize),
                    softwareModule.getId(), "file" + (i + 1), false, artifactSize));
        }

        // Verify correct Creation of SoftwareModule and corresponding artifacts
        softwareModule = softwareModuleManagement.get(softwareModule.getId()).get();
        assertThat(softwareModuleRepository.findAll()).hasSize((int) countSoftwareModule + 1);

        final List<Artifact> artifacts = softwareModule.getArtifacts();

        assertThat(artifacts).hasSize(numberArtifacts);
        if (numberArtifacts != 0) {
            assertArtifactNotNull(artifacts.toArray(new Artifact[artifacts.size()]));
        }

        artifacts.forEach(artifact -> assertThat(artifactRepository.findById(artifact.getId())).isNotNull());
        return softwareModule;
    }

    private void assertArtifactNotNull(final Artifact... results) {
        assertThat(artifactRepository.findAll()).hasSize(results.length);
        for (final Artifact result : results) {
            assertThat(result.getId()).isNotNull();
            assertThat(binaryArtifactRepository.getArtifactBySha1(tenantAware.getCurrentTenant(), result.getSha1Hash()))
                    .isNotNull();
        }
    }

    private void assertArtifactNull(final Artifact... results) {
        for (final Artifact result : results) {
            assertThat(binaryArtifactRepository.getArtifactBySha1(tenantAware.getCurrentTenant(), result.getSha1Hash()))
                    .isNull();
        }
    }

    @Test
    @Description("Test verifies that results are returned based on given filter parameters and in the specified order.")
    public void findSoftwareModuleOrderByDistributionModuleNameAscModuleVersionAsc() {
        // test meta data
        final SoftwareModuleType testType = softwareModuleTypeManagement
                .create(entityFactory.softwareModuleType().create().key("thetype").name("thename").maxAssignments(100));
        DistributionSetType testDsType = distributionSetTypeManagement
                .create(entityFactory.distributionSetType().create().key("key").name("name"));

        distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(testDsType.getId(),
                Collections.singletonList(osType.getId()));
        testDsType = distributionSetTypeManagement.assignOptionalSoftwareModuleTypes(testDsType.getId(),
                Collections.singletonList(testType.getId()));

        // found in test
        final SoftwareModule unassigned = testdataFactory.createSoftwareModule("thetype", "unassignedfound", false);
        final SoftwareModule one = testdataFactory.createSoftwareModule("thetype", "bfound", false);
        final SoftwareModule two = testdataFactory.createSoftwareModule("thetype", "cfound", false);
        final SoftwareModule differentName = testdataFactory.createSoftwareModule("thetype", "a", false);

        // ignored
        final SoftwareModule deleted = testdataFactory.createSoftwareModule("thetype", "deleted", false);
        final SoftwareModule four = testdataFactory.createSoftwareModuleOs("e");

        final DistributionSet set = distributionSetManagement
                .create(entityFactory.distributionSet().create().name("set").version("1").type(testDsType).modules(
                        List.of(one.getId(), two.getId(), deleted.getId(), four.getId(), differentName.getId())));
        softwareModuleManagement.delete(deleted.getId());

        // with filter on name, version and module type
        assertThat(softwareModuleManagement.findAllOrderBySetAssignmentAndModuleNameAscModuleVersionAsc(PAGE,
                set.getId(), "%found%", testType.getId()).getContent())
                .as("Found modules with given name, given module type and the assigned ones first")
                .containsExactly(new AssignedSoftwareModule(one, true), new AssignedSoftwareModule(two, true),
                        new AssignedSoftwareModule(unassigned, false));

        // with filter on name, version and module type, sorting defined by
        // Pagerequest
        assertThat(softwareModuleManagement.findAllOrderBySetAssignmentAndModuleNameAscModuleVersionAsc(
                PageRequest.of(0, 500, Sort.by(Direction.DESC, "name")), set.getId(), "%found%", testType.getId())
                .getContent())
                .as("Found modules with given name, given module type, the assigned ones first, ordered by name DESC")
                .containsExactly(new AssignedSoftwareModule(two, true), new AssignedSoftwareModule(one, true),
                        new AssignedSoftwareModule(unassigned, false));

        // with filter on module type only
        assertThat(softwareModuleManagement
                .findAllOrderBySetAssignmentAndModuleNameAscModuleVersionAsc(PAGE, set.getId(), null, testType.getId())
                .getContent()).as("Found modules with given module type and the assigned ones first")
                .containsExactly(new AssignedSoftwareModule(one, true), new AssignedSoftwareModule(two, true),
                        new AssignedSoftwareModule(differentName, true), new AssignedSoftwareModule(unassigned, false));

        // without any filter
        assertThat(softwareModuleManagement
                .findAllOrderBySetAssignmentAndModuleNameAscModuleVersionAsc(PAGE, set.getId(), null, null)
                .getContent()).as("Found modules with the assigned ones first")
                .containsExactly(new AssignedSoftwareModule(one, true), new AssignedSoftwareModule(two, true),
                        new AssignedSoftwareModule(differentName, true), new AssignedSoftwareModule(four, true),
                        new AssignedSoftwareModule(unassigned, false));
    }

    @Test
    @Description("Checks that number of modules is returned as expected based on given filters.")
    public void countSoftwareModuleByFilters() {
        // test meta data
        final SoftwareModuleType testType = softwareModuleTypeManagement
                .create(entityFactory.softwareModuleType().create().key("thetype").name("thename").maxAssignments(100));
        DistributionSetType testDsType = distributionSetTypeManagement
                .create(entityFactory.distributionSetType().create().key("key").name("name"));

        distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(testDsType.getId(),
                Collections.singletonList(osType.getId()));
        testDsType = distributionSetTypeManagement.assignOptionalSoftwareModuleTypes(testDsType.getId(),
                Collections.singletonList(testType.getId()));

        // found in test
        testdataFactory.createSoftwareModule("thetype", "unassignedfound", false);
        final SoftwareModule one = testdataFactory.createSoftwareModule("thetype", "bfound", false);
        final SoftwareModule two = testdataFactory.createSoftwareModule("thetype", "cfound", false);
        final SoftwareModule differentName = testdataFactory.createSoftwareModule("thetype", "d", false);

        // ignored
        final SoftwareModule deleted = testdataFactory.createSoftwareModule("thetype", "deleted", false);
        final SoftwareModule four = testdataFactory.createSoftwareModuleOs("e");

        distributionSetManagement
                .create(entityFactory.distributionSet().create().name("set").version("1").type(testDsType).modules(
                        List.of(one.getId(), two.getId(), deleted.getId(), four.getId(), differentName.getId())));
        softwareModuleManagement.delete(deleted.getId());

        // test
        assertThat(softwareModuleManagement.countByTextAndType("%found%", testType.getId()))
                .as("Number of modules with given name or version and type").isEqualTo(3);
        assertThat(softwareModuleManagement.countByTextAndType(null, testType.getId()))
                .as("Number of modules with given type").isEqualTo(4);
        assertThat(softwareModuleManagement.countByTextAndType(null, null)).as("Number of modules overall")
                .isEqualTo(5);
    }

    @Test
    @Description("Verfies that all undeleted software modules are found in the repository.")
    public void countSoftwareModuleTypesAll() {
        testdataFactory.createSoftwareModuleOs();

        // one soft deleted
        final SoftwareModule deleted = testdataFactory.createSoftwareModuleApp();
        testdataFactory.createDistributionSet(Collections.singletonList(deleted));
        softwareModuleManagement.delete(deleted.getId());

        assertThat(softwareModuleManagement.count()).as("Number of undeleted modules").isEqualTo(1);
        assertThat(softwareModuleRepository.count()).as("Number of all modules").isEqualTo(2);
    }

    @Test
    @Description("Verfies that software modules are returned that are assigned to given DS.")
    public void findSoftwareModuleByAssignedTo() {
        // test modules
        final SoftwareModule one = testdataFactory.createSoftwareModuleOs();
        testdataFactory.createSoftwareModuleOs("notassigned");

        // one soft deleted
        final SoftwareModule deleted = testdataFactory.createSoftwareModuleApp();
        final DistributionSet set = distributionSetManagement.create(entityFactory.distributionSet().create()
                .name("set").version("1").modules(Arrays.asList(one.getId(), deleted.getId())));
        softwareModuleManagement.delete(deleted.getId());

        assertThat(softwareModuleManagement.findByAssignedTo(PAGE, set.getId()).getContent())
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

        final SoftwareModuleMetadataCreate swMetadata1 = entityFactory.softwareModuleMetadata().create(ah.getId())
                .key(knownKey1).value(knownValue1);

        final SoftwareModuleMetadataCreate swMetadata2 = entityFactory.softwareModuleMetadata().create(ah.getId())
                .key(knownKey2).value(knownValue2);

        final List<SoftwareModuleMetadata> softwareModuleMetadata = softwareModuleManagement
                .createMetaData(Arrays.asList(swMetadata1, swMetadata2));

        final SoftwareModule changedLockRevisionModule = softwareModuleManagement.get(ah.getId()).get();
        assertThat(changedLockRevisionModule.getOptLockRevision()).isEqualTo(2);

        assertThat(softwareModuleMetadata).hasSize(2);
        assertThat(softwareModuleMetadata.get(0)).isNotNull();
        assertThat(softwareModuleMetadata.get(0).getValue()).isEqualTo(knownValue1);
        assertThat(((JpaSoftwareModuleMetadata) softwareModuleMetadata.get(0)).getId().getKey()).isEqualTo(knownKey1);
        assertThat(softwareModuleMetadata.get(0).getEntityId()).isEqualTo(ah.getId());
    }

    @Test
    @Description("Verifies the enforcement of the metadata quota per software module.")
    public void createSoftwareModuleMetadataUntilQuotaIsExceeded() {

        // add meta data one by one
        final SoftwareModule module = testdataFactory.createSoftwareModuleApp("m1");
        final int maxMetaData = quotaManagement.getMaxMetaDataEntriesPerSoftwareModule();
        for (int i = 0; i < maxMetaData; ++i) {
            softwareModuleManagement.createMetaData(
                    entityFactory.softwareModuleMetadata().create(module.getId()).key("k" + i).value("v" + i));
        }

        // quota exceeded
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata()
                        .create(module.getId()).key("k" + maxMetaData).value("v" + maxMetaData)));

        // add multiple meta data entries at once
        final SoftwareModule module2 = testdataFactory.createSoftwareModuleApp("m2");
        final List<SoftwareModuleMetadataCreate> create = new ArrayList<>();
        for (int i = 0; i < maxMetaData + 1; ++i) {
            create.add(entityFactory.softwareModuleMetadata().create(module2.getId()).key("k" + i).value("v" + i));
        }
        // quota exceeded
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> softwareModuleManagement.createMetaData(create));

        // add some meta data entries
        final SoftwareModule module3 = testdataFactory.createSoftwareModuleApp("m3");
        final int firstHalf = Math.round((maxMetaData) / 2.f);
        for (int i = 0; i < firstHalf; ++i) {
            softwareModuleManagement.createMetaData(
                    entityFactory.softwareModuleMetadata().create(module3.getId()).key("k" + i).value("v" + i));
        }
        // add too many data entries
        final int secondHalf = maxMetaData - firstHalf;
        final List<SoftwareModuleMetadataCreate> create2 = new ArrayList<>();
        for (int i = 0; i < secondHalf + 1; ++i) {
            create2.add(entityFactory.softwareModuleMetadata().create(module3.getId()).key("kk" + i).value("vv" + i));
        }
        // quota exceeded
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> softwareModuleManagement.createMetaData(create2));

    }

    @Test
    @Description("Checks that metadata for a software module cannot be created for an existing key.")
    public void createSoftwareModuleMetadataFailsIfKeyExists() {

        final String knownKey1 = "myKnownKey1";
        final String knownValue1 = "myKnownValue1";
        final SoftwareModule ah = testdataFactory.createSoftwareModuleApp();

        softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata().create(ah.getId()).key(knownKey1)
                .value(knownValue1).targetVisible(true));

        assertThatExceptionOfType(EntityAlreadyExistsException.class)
                .isThrownBy(() -> softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata()
                        .create(ah.getId()).key(knownKey1).value(knownValue1).targetVisible(true)))
                .withMessageContaining("Metadata").withMessageContaining(knownKey1);

        final String knownKey2 = "myKnownKey2";

        softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata().create(ah.getId()).key(knownKey2)
                .value(knownValue1).targetVisible(false));

        assertThatExceptionOfType(EntityAlreadyExistsException.class)
                .isThrownBy(() -> softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata()
                        .create(ah.getId()).key(knownKey2).value(knownValue1).targetVisible(true)))
                .withMessageContaining("Metadata").withMessageContaining(knownKey2);
    }

    @Test
    @WithUser(allSpPermissions = true)
    @Description("Checks that metadata for a software module can be updated.")
    public void updateSoftwareModuleMetadata() {
        final String knownKey = "myKnownKey";
        final String knownValue = "myKnownValue";
        final String knownUpdateValue = "myNewUpdatedValue";

        // create a base software module
        final SoftwareModule ah = testdataFactory.createSoftwareModuleApp();
        // initial opt lock revision must be 1
        assertThat(ah.getOptLockRevision()).isEqualTo(1);

        // create an software module meta data entry
        final SoftwareModuleMetadata softwareModuleMetadata = softwareModuleManagement.createMetaData(
                entityFactory.softwareModuleMetadata().create(ah.getId()).key(knownKey).value(knownValue));
        assertThat(softwareModuleMetadata.isTargetVisible()).isFalse();
        assertThat(softwareModuleMetadata.getValue()).isEqualTo(knownValue);

        // base software module should have now the opt lock revision one
        // because we are modifying the base software module
        SoftwareModule changedLockRevisionModule = softwareModuleManagement.get(ah.getId()).get();
        assertThat(changedLockRevisionModule.getOptLockRevision()).isEqualTo(2);

        // update the software module metadata
        final SoftwareModuleMetadata updated = softwareModuleManagement.updateMetaData(entityFactory
                .softwareModuleMetadata().update(ah.getId(), knownKey).value(knownUpdateValue).targetVisible(true));

        // we are updating the sw metadata so also modifying the base software
        // module so opt lock revision must be two
        changedLockRevisionModule = softwareModuleManagement.get(ah.getId()).get();
        assertThat(changedLockRevisionModule.getOptLockRevision()).isEqualTo(3);

        // verify updated meta data contains the updated value
        assertThat(updated).isNotNull();
        assertThat(updated.getValue()).isEqualTo(knownUpdateValue);
        assertThat(updated.isTargetVisible()).isTrue();
        assertThat(((JpaSoftwareModuleMetadata) updated).getId().getKey()).isEqualTo(knownKey);
        assertThat(updated.getEntityId()).isEqualTo(ah.getId());
    }

    @Test
    @Description("Verifies that existing metadata can be deleted.")
    public void deleteSoftwareModuleMetadata() {
        final String knownKey1 = "myKnownKey1";
        final String knownValue1 = "myKnownValue1";

        final SoftwareModule swModule = testdataFactory.createSoftwareModuleApp();

        softwareModuleManagement.createMetaData(
                entityFactory.softwareModuleMetadata().create(swModule.getId()).key(knownKey1).value(knownValue1));

        assertThat(softwareModuleManagement.findMetaDataBySoftwareModuleId(PageRequest.of(0, 10), swModule.getId())
                .getContent()).as("Contains the created metadata element").allSatisfy(metadata -> {
                    assertThat(metadata.getSoftwareModule().getId()).isEqualTo(swModule.getId());
                    assertThat(metadata.getKey()).isEqualTo(knownKey1);
                    assertThat(metadata.getValue()).isEqualTo(knownValue1);
                });

        softwareModuleManagement.deleteMetaData(swModule.getId(), knownKey1);
        assertThat(softwareModuleManagement.findMetaDataBySoftwareModuleId(PageRequest.of(0, 10), swModule.getId())
                .getContent()).as("Metadata elements are").isEmpty();
    }

    @Test
    @Description("Locks a SM.")
    void lockSoftwareModule() {
        final SoftwareModule softwareModule = testdataFactory.createSoftwareModule("sm-1");
        assertThat(
                softwareModuleManagement.get(softwareModule.getId()).map(SoftwareModule::isLocked).orElse(true))
                .isFalse();
        softwareModuleManagement.lock(softwareModule.getId());
        assertThat(
                softwareModuleManagement.get(softwareModule.getId()).map(SoftwareModule::isLocked).orElse(false))
                .isTrue();
    }

    @Test
    @Description("Unlocks a SM.")
    void unlockSoftwareModule() {
        final SoftwareModule softwareModule = testdataFactory.createSoftwareModule("sm-1");
        softwareModuleManagement.lock(softwareModule.getId());
        assertThat(
                softwareModuleManagement.get(softwareModule.getId()).map(SoftwareModule::isLocked).orElse(false))
                .isTrue();
        softwareModuleManagement.unlock(softwareModule.getId());
        assertThat(
                softwareModuleManagement.get(softwareModule.getId()).map(SoftwareModule::isLocked).orElse(true))
                .isFalse();
    }

    @Test
    @Description("Artifacts of a locked SM can't be modified. Expected behaviour is to throw an exception and to do not modify them.")
    void lockSoftwareModuleApplied() {
        final SoftwareModule softwareModule = testdataFactory.createSoftwareModule("sm-1");
        artifactManagement.create(
                new ArtifactUpload(new ByteArrayInputStream(new byte[] {1}), softwareModule.getId(),
                        "artifact1", false, 1));
        final int artifactCount = softwareModuleManagement.get(softwareModule.getId()).get().getArtifacts().size();
        assertThat(artifactCount).isNotEqualTo(0);
        softwareModuleManagement.lock(softwareModule.getId());
        assertThat(
                softwareModuleManagement.get(softwareModule.getId()).map(SoftwareModule::isLocked).orElse(false))
                .isTrue();


        // try add
        assertThatExceptionOfType(LockedException.class)
                .as("Attempt to modify a locked SM artifacts should throw an exception")
                .isThrownBy(() -> artifactManagement.create(
                        new ArtifactUpload(new ByteArrayInputStream(new byte[] {2}), softwareModule.getId(),
                                "artifact2", false, 1)));
        assertThat(softwareModuleManagement.get(softwareModule.getId()).get().getArtifacts().size())
                .as("Artifacts shall not be added to a locked SM.")
                .isEqualTo(artifactCount);

        // try remove
        final long artifactId = softwareModuleManagement.get(softwareModule.getId()).get()
                .getArtifacts().stream().findFirst().get().getId();
        assertThatExceptionOfType(LockedException.class)
                .as("Attempt to modify a locked SM artifacts should throw an exception")
                .isThrownBy(() -> artifactManagement.delete(artifactId));
        assertThat(softwareModuleManagement.get(softwareModule.getId()).get().getArtifacts().size())
                .as("Artifact shall not be removed from a locked SM.")
                .isEqualTo(artifactCount);
        assertThat(artifactManagement.get(artifactId))
                .as("Artifact shall not be removed if belongs to a locked SM.")
                .isPresent();
    }

    @Test
    @Description("Verifies that non existing metadata find results in exception.")
    public void findSoftwareModuleMetadataFailsIfEntryDoesNotExist() {
        final String knownKey1 = "myKnownKey1";
        final String knownValue1 = "myKnownValue1";

        final SoftwareModule ah = testdataFactory.createSoftwareModuleApp();

        softwareModuleManagement.createMetaData(
                entityFactory.softwareModuleMetadata().create(ah.getId()).key(knownKey1).value(knownValue1));

        assertThat(softwareModuleManagement.getMetaDataBySoftwareModuleId(ah.getId(), "doesnotexist")).isNotPresent();
    }

    @Test
    @Description("Queries and loads the metadata related to a given software module.")
    public void findAllSoftwareModuleMetadataBySwId() {

        final SoftwareModule sw1 = testdataFactory.createSoftwareModuleApp();
        final int metadataCountSw1 = 8;

        final SoftwareModule sw2 = testdataFactory.createSoftwareModuleOs();
        final int metadataCountSw2 = 10;

        for (int index = 0; index < metadataCountSw1; index++) {
            softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata().create(sw1.getId())
                    .key("key" + index).value("value" + index).targetVisible(true));
        }

        for (int index = 0; index < metadataCountSw2; index++) {
            softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata().create(sw2.getId())
                    .key("key" + index).value("value" + index).targetVisible(false));
        }

        Page<SoftwareModuleMetadata> metadataSw1 = softwareModuleManagement
                .findMetaDataBySoftwareModuleId(PageRequest.of(0, 100), sw1.getId());

        Page<SoftwareModuleMetadata> metadataSw2 = softwareModuleManagement
                .findMetaDataBySoftwareModuleId(PageRequest.of(0, 100), sw2.getId());

        assertThat(metadataSw1.getNumberOfElements()).isEqualTo(metadataCountSw1);
        assertThat(metadataSw1.getTotalElements()).isEqualTo(metadataCountSw1);

        assertThat(metadataSw2.getNumberOfElements()).isEqualTo(metadataCountSw2);
        assertThat(metadataSw2.getTotalElements()).isEqualTo(metadataCountSw2);

        metadataSw1 = softwareModuleManagement.findMetaDataBySoftwareModuleIdAndTargetVisible(PageRequest.of(0, 100),
                sw1.getId());

        metadataSw2 = softwareModuleManagement.findMetaDataBySoftwareModuleIdAndTargetVisible(PageRequest.of(0, 100),
                sw2.getId());

        assertThat(metadataSw1.getNumberOfElements()).isEqualTo(metadataCountSw1);
        assertThat(metadataSw1.getTotalElements()).isEqualTo(metadataCountSw1);

        assertThat(metadataSw2.getNumberOfElements()).isZero();
        assertThat(metadataSw2.getTotalElements()).isZero();
    }
}
