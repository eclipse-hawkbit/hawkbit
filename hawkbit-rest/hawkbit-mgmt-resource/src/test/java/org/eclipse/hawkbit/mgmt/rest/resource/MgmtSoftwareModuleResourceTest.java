/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.mgmt.json.model.artifact.MgmtArtifact;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRepresentationMode;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.Constants;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.FileSizeQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.StorageQuotaExceededException;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.ArtifactUpload;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.test.util.HashGeneratorUtils;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.eclipse.hawkbit.rest.json.model.ExceptionInfo;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.hamcrest.Matchers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RestController;

import com.jayway.jsonpath.JsonPath;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Tests for {@link MgmtSoftwareModuleResource} {@link RestController}.
 */
@Feature("Component Tests - Management API")
@Story("Software Module Resource")
@TestPropertySource(properties = { "hawkbit.server.security.dos.maxArtifactSize=100000",
        "hawkbit.server.security.dos.maxArtifactStorage=500000" })
class MgmtSoftwareModuleResourceTest extends AbstractManagementApiIntegrationTest {

    @BeforeEach
    public void assertPreparationOfRepo() {
        assertThat(softwareModuleManagement.findAll(PAGE)).as("no softwaremodule should be founded").isEmpty();
    }

    @Test
    @Description("Tests the update of software module metadata. It is verfied that only the selected fields for the update are really updated and the modification values are filled (i.e. updated by and at).")
    @WithUser(principal = "smUpdateTester", allSpPermissions = true)
    void updateSoftwareModuleOnlyDescriptionAndVendorNameUntouched() throws Exception {
        final String knownSWName = "name1";
        final String knownSWVersion = "version1";
        final String knownSWDescription = "description1";
        final String knownSWVendor = "vendor1";

        final String updateVendor = "newVendor1";
        final String updateDescription = "newDescription1";

        final SoftwareModule sm = softwareModuleManagement.create(entityFactory.softwareModule()
                .create()
                .type(osType)
                .name(knownSWName)
                .version(knownSWVersion)
                .description(knownSWDescription)
                .vendor(knownSWVendor));

        assertThat(sm.getName()).as("Wrong name of the software module").isEqualTo(knownSWName);

        final String body = new JSONObject().put("vendor", updateVendor)
                .put("description", updateDescription)
                .put("name", "nameShouldNotBeChanged")
                .toString();

        // ensures that we are not to fast so that last modified is not set correctly
        Awaitility.await()
                .atMost(Duration.ofMillis(100))
                .pollInterval(10L, TimeUnit.MILLISECONDS)
                .until(() -> sm.getLastModifiedAt() > 0L && sm.getLastModifiedBy() != null);

        mvc.perform(put("/rest/v1/softwaremodules/{smId}", sm.getId()).content(body)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(sm.getId().intValue())))
                .andExpect(jsonPath("$.vendor", equalTo(updateVendor)))
                .andExpect(jsonPath("$.lastModifiedBy", equalTo("smUpdateTester")))
                .andExpect(jsonPath("$.lastModifiedAt", not(equalTo(sm.getLastModifiedAt()))))
                .andExpect(jsonPath("$.description", equalTo(updateDescription)))
                .andExpect(jsonPath("$.name", equalTo(knownSWName)))
                .andExpect(jsonPath("$.locked", equalTo(false)))
                .andReturn();

        final SoftwareModule updatedSm = softwareModuleManagement.get(sm.getId()).get();
        assertThat(updatedSm.getName()).isEqualTo(knownSWName);
        assertThat(updatedSm.getVendor()).isEqualTo(updateVendor);
        assertThat(updatedSm.getLastModifiedBy()).isEqualTo("smUpdateTester");
        assertThat(updatedSm.getDescription()).isEqualTo(updateDescription);

    }

    @Test
    @Description("Tests the update of the deletion flag. It is verified that the software module can't be marked as deleted through update operation.")
    @WithUser(principal = "smUpdateTester", allSpPermissions = true)
    void updateSoftwareModuleDeletedFlag() throws Exception {
        final String knownSWName = "name1";
        final String knownSWVersion = "version1";

        final SoftwareModule sm = softwareModuleManagement.create(
                entityFactory.softwareModule().create().type(osType).name(knownSWName).version(knownSWVersion));

        assertThat(sm.isDeleted()).as("Created software module should not be deleted").isFalse();

        final String body = new JSONObject().put("deleted", true).toString();

        // ensures that we are not to fast so that last modified is not set correctly
        Awaitility.await()
                .atMost(Duration.ofMillis(100))
                .pollInterval(10L, TimeUnit.MILLISECONDS)
                .until(() -> sm.getLastModifiedAt() > 0L && sm.getLastModifiedBy() != null);

        mvc.perform(put("/rest/v1/softwaremodules/{smId}", sm.getId()).content(body)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(sm.getId().intValue())))
                .andExpect(jsonPath("$.lastModifiedBy", equalTo("smUpdateTester")))
                .andExpect(jsonPath("$.lastModifiedAt", equalTo(sm.getLastModifiedAt())))
                .andExpect(jsonPath("$.deleted", equalTo(false)));

        final SoftwareModule updatedSm = softwareModuleManagement.get(sm.getId()).get();
        assertThat(sm.getLastModifiedBy()).isEqualTo("smUpdateTester");
        assertThat(sm.getLastModifiedAt()).isEqualTo(sm.getLastModifiedAt());
        assertThat(sm.isDeleted()).isFalse();

    }

    @Test
    @Description("Tests the lock. It is verified that the software module can be marked as locked through update operation.")
    @WithUser(principal = "smUpdateTester", allSpPermissions = true)
    void lockSoftwareModule() throws Exception {
        final SoftwareModule sm = softwareModuleManagement.create(
                entityFactory.softwareModule().create().type(osType).name("name1").version("version1"));
        assertThat(sm.isLocked()).as("Created software module should not be locked").isFalse();
        // ensures that we are not to fast so that last modified is not set correctly
        Awaitility.await()
                .atMost(Duration.ofMillis(100))
                .pollInterval(10L, TimeUnit.MILLISECONDS)
                .until(() -> sm.getLastModifiedAt() > 0L && sm.getLastModifiedBy() != null);

        // lock
        final String body = new JSONObject().put("locked", true).toString();
        final ResultActions resultActions =
                mvc.perform(put("/rest/v1/softwaremodules/{smId}", sm.getId()).content(body)
                        .contentType(MediaType.APPLICATION_JSON));

        final SoftwareModule updatedSm = softwareModuleManagement.get(sm.getId()).get();
        assertThat(updatedSm.getLastModifiedBy()).isEqualTo("smUpdateTester");
        assertThat(updatedSm.isLocked()).isTrue();

        resultActions
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(sm.getId().intValue())))
                .andExpect(jsonPath("$.lastModifiedBy", equalTo("smUpdateTester")))
                .andExpect(jsonPath("$.lastModifiedAt", equalTo(updatedSm.getLastModifiedAt())))
                .andExpect(jsonPath("$.locked", equalTo(true)));
    }

    @Test
    @Description("Tests the unlock.")
    @WithUser(principal = "smUpdateTester", allSpPermissions = true)
    void unlockSoftwareModule() throws Exception {
        final SoftwareModule sm = softwareModuleManagement.create(
                entityFactory.softwareModule().create().type(osType).name("name1").version("version1"));
        softwareModuleManagement.lock(sm.getId());
        assertThat(softwareModuleManagement.get(sm.getId())
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, sm.getId())).isLocked())
                .as("Software module is locked")
                .isTrue();
        // ensures that we are not to fast so that last modified is not set correctly
        Awaitility.await()
                .atMost(Duration.ofMillis(100))
                .pollInterval(10L, TimeUnit.MILLISECONDS)
                .until(() -> sm.getLastModifiedAt() > 0L && sm.getLastModifiedBy() != null);

        // unlock
        final String body = new JSONObject().put("locked", false).toString();
        final ResultActions resultActions =
                mvc.perform(put("/rest/v1/softwaremodules/{smId}", sm.getId()).content(body)
                        .contentType(MediaType.APPLICATION_JSON));

        final SoftwareModule updatedSm = softwareModuleManagement.get(sm.getId()).get();
        assertThat(updatedSm.getLastModifiedBy()).isEqualTo("smUpdateTester");
        assertThat(updatedSm.isLocked()).isFalse(); // not unlocked

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(sm.getId().intValue())))
                .andExpect(jsonPath("$.lastModifiedBy", equalTo("smUpdateTester")))
                .andExpect(jsonPath("$.lastModifiedAt", equalTo(updatedSm.getLastModifiedAt())))
                .andExpect(jsonPath("$.locked", equalTo(false)));
    }

    @Test
    @Description("Tests the upload of an artifact binary. The upload is executed and the content checked in the repository for completeness.")
    void uploadArtifact() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        // create test file
        final byte[] random = randomBytes(5 * 1024);
        final String md5sum = HashGeneratorUtils.generateMD5(random);
        final String sha1sum = HashGeneratorUtils.generateSHA1(random);
        final String sha256sum = HashGeneratorUtils.generateSHA256(random);
        final MockMultipartFile file = new MockMultipartFile("file", "origFilename", null, random);

        // upload
        final MvcResult mvcResult = mvc.perform(
                        multipart("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()).file(file)
                                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.hashes.md5", equalTo(md5sum)))
                .andExpect(jsonPath("$.hashes.sha1", equalTo(sha1sum)))
                .andExpect(jsonPath("$.hashes.sha256", equalTo(sha256sum)))
                .andExpect(jsonPath("$.size", equalTo(random.length)))
                .andExpect(jsonPath("$.providedFilename", equalTo("origFilename")))
                .andReturn();

        // check rest of response compared to DB
        final MgmtArtifact artResult = ResourceUtility.convertArtifactResponse(
                mvcResult.getResponse().getContentAsString());
        final Long artId = softwareModuleManagement.get(sm.getId()).get().getArtifacts().get(0).getId();
        assertThat(artResult.getArtifactId()).as("Wrong artifact id").isEqualTo(artId);
        assertThat(JsonPath.compile("$._links.self.href")
                .read(mvcResult.getResponse().getContentAsString())
                .toString()).as("Link contains no self url")
                .isEqualTo("http://localhost/rest/v1/softwaremodules/" + sm.getId() + "/artifacts/" + artId);
        assertThat(JsonPath.compile("$._links.download.href")
                .read(mvcResult.getResponse().getContentAsString())
                .toString()).as("response contains no download url ")
                .isEqualTo(
                        "http://localhost/rest/v1/softwaremodules/" + sm.getId() + "/artifacts/" + artId + "/download");

        assertArtifact(sm, random);
    }

    @Test
    @Description("Verifies that artifacts which exceed the configured maximum size cannot be uploaded.")
    void uploadArtifactFailsIfTooLarge() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModule("quota", "quota", false);
        final long maxSize = quotaManagement.getMaxArtifactSize();

        // create a file which exceeds the configured maximum size
        final byte[] randomBytes = randomBytes(Math.toIntExact(maxSize) + 1024);

        final MockMultipartFile file = new MockMultipartFile("file", "origFilename", null, randomBytes);

        // try to upload
        mvc.perform(multipart("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()).file(file)
                        .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.exceptionClass", equalTo(FileSizeQuotaExceededException.class.getName())))
                .andExpect(jsonPath("$.errorCode", equalTo(SpServerError.SP_FILE_SIZE_QUOTA_EXCEEDED.getKey())));
    }

    @Test
    @Description("Verifies that artifact with invalid filename cannot be uploaded to prevent cross site scripting.")
    void uploadArtifactFailsIfFilenameInvalide() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModule("quota", "quota", false);
        final String illegalFilename = "<img src=ernw onerror=alert(1)>.xml";

        final byte[] randomBytes = randomBytes(5 * 1024);
        final MockMultipartFile file = new MockMultipartFile("file", illegalFilename, null, randomBytes);

        mvc.perform(multipart("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()).file(file)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid characters in string")));
    }

    private void assertArtifact(final SoftwareModule sm, final byte[] random) throws IOException {
        // check result in db...
        // repo
        assertThat(artifactManagement.count()).as("Wrong artifact size").isEqualTo(1);

        // binary
        try (final InputStream fileInputStream = artifactManagement
                .loadArtifactBinary(softwareModuleManagement.get(sm.getId()).get().getArtifacts().get(0).getSha1Hash(),
                        sm.getId(), sm.isEncrypted())
                .get().getFileInputStream()) {
            assertTrue(IOUtils.contentEquals(new ByteArrayInputStream(random), fileInputStream),
                    "Wrong artifact content");
        }

        // hashes
        assertThat(artifactManagement.getByFilename("origFilename").get().getSha1Hash()).as("Wrong sha1 hash")
                .isEqualTo(HashGeneratorUtils.generateSHA1(random));

        assertThat(artifactManagement.getByFilename("origFilename").get().getMd5Hash()).as("Wrong md5 hash")
                .isEqualTo(HashGeneratorUtils.generateMD5(random));

        assertThat(artifactManagement.getByFilename("origFilename").get().getSha256Hash()).as("Wrong sha256 hash")
                .isEqualTo(HashGeneratorUtils.generateSHA256(random));

        // metadata
        assertThat(softwareModuleManagement.get(sm.getId()).get().getArtifacts().get(0).getFilename())
                .as("wrong metadata of the filename").isEqualTo("origFilename");
    }

    @Test
    @Description("Verifies that the system does not accept empty artifact uploads. Expected response: BAD REQUEST")
    void emptyUploadArtifact() throws Exception {
        assertThat(softwareModuleManagement.findAll(PAGE)).isEmpty();
        assertThat(artifactManagement.count()).isZero();

        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        final MockMultipartFile file = new MockMultipartFile("file", "orig", null, new byte[0]);

        mvc.perform(multipart("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()).file(file)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Verifies that the system does not accept identical artifacts uploads for the same software module. Expected response: CONFLICT")
    void duplicateUploadArtifact() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        final byte[] random = randomBytes(5 * 1024);
        final String md5sum = HashGeneratorUtils.generateMD5(random);
        final String sha1sum = HashGeneratorUtils.generateSHA1(random);
        final String sha256sum = HashGeneratorUtils.generateSHA256(random);
        final MockMultipartFile file = new MockMultipartFile("file", "orig", null, random);

        mvc.perform(multipart("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()).file(file)
                        .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.hashes.md5", equalTo(md5sum)))
                .andExpect(jsonPath("$.hashes.sha1", equalTo(sha1sum)))
                .andExpect(jsonPath("$.hashes.sha256", equalTo(sha256sum)))
                .andExpect(jsonPath("$.providedFilename", equalTo("orig"))).andExpect(status().isCreated());

        mvc.perform(multipart("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()).file(file))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isConflict());
    }

    @Test
    @Description("verifies that option to upload artifacts with a custom defined by metadata, i.e. not the file name of the binary itself.")
    void uploadArtifactWithCustomName() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();
        assertThat(artifactManagement.count()).isZero();

        // create test file
        final byte[] random = randomBytes(5 * 1024);
        final MockMultipartFile file = new MockMultipartFile("file", "origFilename", null, random);

        // upload
        mvc.perform(multipart("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()).file(file)
                        .param("filename", "customFilename"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$.providedFilename", equalTo("customFilename")))
                .andExpect(status().isCreated());

        // check result in db...
        // repo
        assertThat(artifactManagement.count()).isEqualTo(1);

        // hashes
        assertThat(artifactManagement.getByFilename("customFilename")).as("Local artifact is wrong").isPresent();
    }

    @Test
    @Description("Trying to create a SM from already marked as deleted type - should get as response 400 Bad Request")
    public void createSMFromAlreadyMarkedAsDeletedType() throws Exception {
        final String SM_TYPE = "someSmType";
        final SoftwareModule sm = testdataFactory.createSoftwareModule(SM_TYPE);
        final DistributionSetType t = testdataFactory.findOrCreateDistributionSetType(
            "testKey", "testType", Collections.singletonList(sm.getType()),
            Collections.singletonList(sm.getType()));
        final DistributionSetType type = testdataFactory.findOrCreateDistributionSetType("testKey", "testType");
        final DistributionSet ds = testdataFactory.createDistributionSet("name", "version", type, Collections.singletonList(sm));
        final Target target = testdataFactory.createTarget("test");

        assignDistributionSet(ds, target);
        //delete sm type
        softwareModuleTypeManagement.delete(sm.getType().getId());

        //check if it is marked as deleted
        final Optional<SoftwareModuleType> opt = softwareModuleTypeManagement.getByKey(SM_TYPE);
        if (opt.isEmpty()) {
            throw new AssertionError("The Optional object of software module type should not be empty!");
        }
        final SoftwareModuleType smType = opt.get();
        Assert.isTrue(smType.isDeleted(), "Software Module Type not marked as deleted!");

        //check if we'll get bad request if we try to create module from the deleted type
        final MvcResult mvcResult = mvc.perform(post("/rest/v1/softwaremodules")
                .content("[{\"description\":\"someDescription\",\"key\":\"someTestKey\", \"type\":\"" + SM_TYPE + "\"}]")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andReturn();

        final ExceptionInfo exceptionInfo = ResourceUtility.convertException(mvcResult.getResponse().getContentAsString());
        assertEquals("jakarta.validation.ValidationException", exceptionInfo.getExceptionClass());
        assertTrue(exceptionInfo.getMessage().contains("Software Module Type already deleted"));
    }

    @Test
    @Description("Verifies that the system refuses upload of an artifact where the provided hash sums do not match. Expected result: BAD REQUEST")
    void uploadArtifactWithHashCheck() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();
        assertThat(artifactManagement.count()).isZero();

        // create test file
        final byte[] random = randomBytes(5 * 1024);
        final String md5sum = HashGeneratorUtils.generateMD5(random);
        final String sha1sum = HashGeneratorUtils.generateSHA1(random);
        final String sha256sum = HashGeneratorUtils.generateSHA256(random);
        final MockMultipartFile file = new MockMultipartFile("file", "origFilename", null, random);

        // upload
        // wrong sha1
        MvcResult mvcResult = mvc
                .perform(multipart("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()).file(file)
                        .param("md5sum", md5sum).param("sha1sum", "afsdff").param("sha256sum", sha256sum))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andReturn();

        // check error result
        ExceptionInfo exceptionInfo = ResourceUtility.convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getErrorCode()).as("Exception contains wrong error code")
                .isEqualTo(SpServerError.SP_ARTIFACT_UPLOAD_FAILED_SHA1_MATCH.getKey());

        // wrong sha256
        mvcResult = mvc
                .perform(multipart("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()).file(file)
                        .param("md5sum", md5sum).param("sha1sum", sha1sum).param("sha256sum", "jdshfsd"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andReturn();

        // check error result
        exceptionInfo = ResourceUtility.convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getErrorCode()).as("Exception contains wrong error code")
                .isEqualTo(SpServerError.SP_ARTIFACT_UPLOAD_FAILED_SHA256_MATCH.getKey());

        // wrong md5
        mvcResult = mvc
                .perform(multipart("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()).file(file)
                        .param("md5sum", "sdfsdfs").param("sha1sum", sha1sum).param("sha256sum", sha256sum))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andReturn();

        // check error result
        exceptionInfo = ResourceUtility.convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getErrorCode()).as("Exception contains wrong error code")
                .isEqualTo(SpServerError.SP_ARTIFACT_UPLOAD_FAILED_MD5_MATCH.getKey());

        mvc.perform(multipart("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()).file(file)
                .param("md5sum", md5sum).param("sha1sum", sha1sum)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated());

        assertArtifact(sm, random);

    }

    @Test
    @Description("Verifies that only a limited number of artifacts can be uploaded for one software module.")
    void uploadArtifactsUntilQuotaExceeded() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();
        final long maxArtifacts = quotaManagement.getMaxArtifactsPerSoftwareModule();

        for (int i = 0; i < maxArtifacts; ++i) {
            // create test file
            final byte[] random = randomBytes(5 * 1024);
            final String md5sum = HashGeneratorUtils.generateMD5(random);
            final String sha1sum = HashGeneratorUtils.generateSHA1(random);
            final String sha256sum = HashGeneratorUtils.generateSHA256(random);
            final MockMultipartFile file = new MockMultipartFile("file", "origFilename" + i, null, random);

            // upload
            mvc.perform(multipart("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()).file(file)
                    .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                    .andExpect(status().isCreated()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.hashes.md5", equalTo(md5sum)))
                    .andExpect(jsonPath("$.hashes.sha1", equalTo(sha1sum)))
                    .andExpect(jsonPath("$.hashes.sha256", equalTo(sha256sum)))
                    .andExpect(jsonPath("$.size", equalTo(random.length)))
                    .andExpect(jsonPath("$.providedFilename", equalTo("origFilename" + i))).andReturn();
        }

        // upload one more file to cause the quota to be exceeded
        final byte[] random = randomBytes(5 * 1024);
        final MockMultipartFile file = new MockMultipartFile("file", "origFilename_final", null, random);

        // upload
        mvc.perform(multipart("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()).file(file)
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.exceptionClass", equalTo(AssignmentQuotaExceededException.class.getName())))
                .andExpect(jsonPath("$.errorCode", equalTo(SpServerError.SP_QUOTA_EXCEEDED.getKey())));

    }

    @Test
    @Description("Verifies that artifacts can only be added as long as the artifact storage quota is not exceeded.")
    void uploadArtifactsUntilStorageQuotaExceeded() throws Exception {

        final long storageLimit = quotaManagement.getMaxArtifactStorage();

        // choose an artifact size which does not violate the max file size
        final int artifactSize = Math.toIntExact(quotaManagement.getMaxArtifactSize() / 10);
        final int numArtifacts = Math.toIntExact(storageLimit / artifactSize);

        for (int i = 0; i < numArtifacts; ++i) {
            // create test file
            final byte[] random = randomBytes(artifactSize);
            final String md5sum = HashGeneratorUtils.generateMD5(random);
            final String sha1sum = HashGeneratorUtils.generateSHA1(random);
            final String sha256sum = HashGeneratorUtils.generateSHA256(random);
            final MockMultipartFile file = new MockMultipartFile("file", "origFilename" + i, null, random);

            // upload
            final SoftwareModule sm = testdataFactory.createSoftwareModuleOs("sm" + i);
            mvc.perform(multipart("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()).file(file)
                    .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                    .andExpect(status().isCreated()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.hashes.md5", equalTo(md5sum)))
                    .andExpect(jsonPath("$.hashes.sha1", equalTo(sha1sum)))
                    .andExpect(jsonPath("$.hashes.sha256", equalTo(sha256sum)))
                    .andExpect(jsonPath("$.size", equalTo(random.length)))
                    .andExpect(jsonPath("$.providedFilename", equalTo("origFilename" + i))).andReturn();
        }

        // upload one more file to cause the quota to be exceeded
        final byte[] random = randomBytes(artifactSize);
        final MockMultipartFile file = new MockMultipartFile("file", "origFilename_final", null, random);

        // upload
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs("sm" + numArtifacts);
        mvc.perform(multipart("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()).file(file)
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.exceptionClass", equalTo(StorageQuotaExceededException.class.getName())))
                .andExpect(jsonPath("$.errorCode", equalTo(SpServerError.SP_STORAGE_QUOTA_EXCEEDED.getKey())));

    }

    @Test
    @Description("Tests binary download of an artifact including verfication that the downloaded binary is consistent and that the etag header is as expected identical to the SHA1 hash of the file.")
    void downloadArtifact() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        final int artifactSize = 5 * 1024;
        final byte[] random = randomBytes(artifactSize);

        final Artifact artifact = artifactManagement.create(
                new ArtifactUpload(new ByteArrayInputStream(random), sm.getId(), "file1", false, artifactSize));
        final Artifact artifact2 = artifactManagement.create(
                new ArtifactUpload(new ByteArrayInputStream(random), sm.getId(), "file2", false, artifactSize));

        downloadAndVerify(sm, random, artifact);
        downloadAndVerify(sm, random, artifact2);

        assertThat(softwareModuleManagement.findAll(PAGE)).as("Softwaremodule size is wrong").hasSize(1);
        assertThat(artifactManagement.count()).isEqualTo(2);
    }

    private void downloadAndVerify(final SoftwareModule sm, final byte[] random, final Artifact artifact)
            throws Exception {
        final MvcResult result = mvc
                .perform(
                        get("/rest/v1/softwaremodules/{smId}/artifacts/{artId}/download", sm.getId(), artifact.getId()))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(header().string("ETag", artifact.getSha1Hash())).andReturn();

        assertTrue(Arrays.equals(result.getResponse().getContentAsByteArray(), random), "Wrong response content");
    }

    @Test
    @Description("Verifies the listing of one defined artifact assigned to a given software module. That includes the artifact metadata and download links.")
    void getArtifact() throws Exception {
        // prepare data for test
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        final int artifactSize = 5 * 1024;
        final byte random[] = randomBytes(artifactSize);

        final Artifact artifact = artifactManagement
                .create(new ArtifactUpload(new ByteArrayInputStream(random), sm.getId(), "file1", false, artifactSize));

        // perform test
        mvc.perform(get("/rest/v1/softwaremodules/{smId}/artifacts/{artId}", sm.getId(), artifact.getId())
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id", equalTo(artifact.getId().intValue())))
                .andExpect(jsonPath("$.size", equalTo(random.length)))
                .andExpect(jsonPath("$.hashes.md5", equalTo(artifact.getMd5Hash())))
                .andExpect(jsonPath("$.hashes.sha1", equalTo(artifact.getSha1Hash())))
                .andExpect(jsonPath("$.hashes.sha256", equalTo(artifact.getSha256Hash())))
                .andExpect(jsonPath("$.providedFilename", equalTo("file1")))
                .andExpect(jsonPath("$._links.download.href",
                        equalTo("http://localhost/rest/v1/softwaremodules/%s/artifacts/%s/download"
                                .formatted(sm.getId(), artifact.getId()))))
                .andExpect(jsonPath("$._links.self.href",
                        equalTo("http://localhost/rest/v1/softwaremodules/%s/artifacts/%s".formatted(sm.getId(),
                                artifact.getId()))));
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    @Description("Verifies the listing of one defined artifact assigned to a given software module. That includes the artifact metadata and cdn download links.")
    void getArtifactWithUseArtifactUrlHandlerParameter(final boolean useArtifactUrlHandler) throws Exception {
        // prepare data for test
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        final int artifactSize = 5 * 1024;
        final byte random[] = randomBytes(artifactSize);

        final Artifact artifact = artifactManagement
                .create(new ArtifactUpload(new ByteArrayInputStream(random), sm.getId(), "file1", false, artifactSize));

        // perform test
        mvc.perform(get("/rest/v1/softwaremodules/{smId}/artifacts/{artId}", sm.getId(), artifact.getId())
                .param("useartifacturlhandler", String.valueOf(useArtifactUrlHandler))
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id", equalTo(artifact.getId().intValue())))
                .andExpect(jsonPath("$.size", equalTo(random.length)))
                .andExpect(jsonPath("$.hashes.md5", equalTo(artifact.getMd5Hash())))
                .andExpect(jsonPath("$.hashes.sha1", equalTo(artifact.getSha1Hash())))
                .andExpect(jsonPath("$.hashes.sha256", equalTo(artifact.getSha256Hash())))
                .andExpect(jsonPath("$.providedFilename", equalTo("file1")))
                .andExpect(jsonPath("$._links.download.href", useArtifactUrlHandler
                        ? equalTo("http://download-cdn.com/artifacts/%s/download".formatted(artifact.getFilename()))
                        : equalTo("http://localhost/rest/v1/softwaremodules/%s/artifacts/%s/download"
                                .formatted(sm.getId(), artifact.getId()))))
                .andExpect(jsonPath("$._links.self.href", equalTo(
                        "http://localhost/rest/v1/softwaremodules/%s/artifacts/%s".formatted(sm.getId(),
                                artifact.getId()))));
    }

    @Test
    @Description("Verifies the listing of an artifact that belongs to a soft deleted software module.")
    void getArtifactSoftDeleted() throws Exception {
        // prepare data for test
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs("softDeleted");
        final Artifact artifact = testdataFactory.createArtifacts(sm.getId()).get(0);
        testdataFactory.createDistributionSet(Collections.singletonList(sm));
        softwareModuleManagement.delete(sm.getId());

        // perform test
        mvc.perform(get("/rest/v1/softwaremodules/{smId}/artifacts/{artId}", sm.getId(), artifact.getId()).accept(
                        MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id", equalTo(artifact.getId().intValue())))
                .andExpect(jsonPath("$.size", equalTo((int) artifact.getSize())))
                .andExpect(jsonPath("$.hashes.md5", equalTo(artifact.getMd5Hash())))
                .andExpect(jsonPath("$.hashes.sha1", equalTo(artifact.getSha1Hash())))
                .andExpect(jsonPath("$.hashes.sha256", equalTo(artifact.getSha256Hash())))
                .andExpect(jsonPath("$.providedFilename", equalTo(artifact.getFilename())))
                .andExpect(jsonPath("$._links", Matchers.not(Matchers.hasKey("download"))))
                .andExpect(jsonPath("$._links.self.href",
                        equalTo(String.format("http://localhost/rest/v1/softwaremodules/%d/artifacts/%d", sm.getId(),
                                artifact.getId()))));
    }

    @Test
    @Description("Verifies the listing of all artifacts assigned to a software module. That includes the artifact metadata.")
    void getArtifacts() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        final int artifactSize = 5 * 1024;
        final byte[] random = randomBytes(artifactSize);

        final Artifact artifact = artifactManagement.create(
                new ArtifactUpload(new ByteArrayInputStream(random), sm.getId(), "file1", false, artifactSize));
        final Artifact artifact2 = artifactManagement.create(
                new ArtifactUpload(new ByteArrayInputStream(random), sm.getId(), "file2", false, artifactSize));

        mvc.perform(get("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.[0].id", equalTo(artifact.getId().intValue())))
                .andExpect(jsonPath("$.[0].size", equalTo(random.length)))
                .andExpect(jsonPath("$.[0].hashes.md5", equalTo(artifact.getMd5Hash())))
                .andExpect(jsonPath("$.[0].hashes.sha1", equalTo(artifact.getSha1Hash())))
                .andExpect(jsonPath("$.[0].hashes.sha256", equalTo(artifact.getSha256Hash())))
                .andExpect(jsonPath("$.[0].providedFilename", equalTo("file1")))
                .andExpect(jsonPath("$.[0]._links.self.href",
                        equalTo("http://localhost/rest/v1/softwaremodules/" + sm.getId() + "/artifacts/"
                                + artifact.getId())))
                .andExpect(jsonPath("$.[1].id", equalTo(artifact2.getId().intValue())))
                .andExpect(jsonPath("$.[1].hashes.md5", equalTo(artifact2.getMd5Hash())))
                .andExpect(jsonPath("$.[1].hashes.sha1", equalTo(artifact2.getSha1Hash())))
                .andExpect(jsonPath("$.[1].hashes.sha256", equalTo(artifact2.getSha256Hash())))
                .andExpect(jsonPath("$.[1].providedFilename", equalTo("file2")))
                .andExpect(jsonPath("$.[1]._links.self.href", equalTo(
                        "http://localhost/rest/v1/softwaremodules/" + sm.getId() + "/artifacts/" + artifact2.getId())));
    }

    @Test
    @Description("Handles the GET request of retrieving all meta data of artifacts assigned to a software module (in full representation mode including a download URL by the artifact provider).")
    public void getArtifactsWithParameters() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        final byte[] random = RandomStringUtils.random(5).getBytes();

        artifactManagement.create(new ArtifactUpload(new ByteArrayInputStream(random), sm.getId(), "file1", false, 0));

        mvc.perform(
                        get(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING + "/{softwareModuleId}/artifacts", sm.getId())
                                .param("representation", MgmtRepresentationMode.FULL.toString())
                                .param("useartifacturlhandler", Boolean.TRUE.toString()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON));
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    @Description("Verifies the listing of all artifacts assigned to a software module. That includes the artifact metadata and download links.")
    void getArtifactsWithUseArtifactUrlHandlerParameter(final boolean useArtifactUrlHandler) throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        final int artifactSize = 5 * 1024;
        final byte[] random = randomBytes(artifactSize);

        final Artifact artifact = artifactManagement
                .create(new ArtifactUpload(new ByteArrayInputStream(random), sm.getId(), "file1", false, artifactSize));
        final Artifact artifact2 = artifactManagement
                .create(new ArtifactUpload(new ByteArrayInputStream(random), sm.getId(), "file2", false, artifactSize));

        mvc.perform(get("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId())
                .param("representation", MgmtRepresentationMode.FULL.toString())
                .param("useartifacturlhandler", String.valueOf(useArtifactUrlHandler))
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.[0].id", equalTo(artifact.getId().intValue())))
                .andExpect(jsonPath("$.[0].size", equalTo(random.length)))
                .andExpect(jsonPath("$.[0].hashes.md5", equalTo(artifact.getMd5Hash())))
                .andExpect(jsonPath("$.[0].hashes.sha1", equalTo(artifact.getSha1Hash())))
                .andExpect(jsonPath("$.[0].hashes.sha256", equalTo(artifact.getSha256Hash())))
                .andExpect(jsonPath("$.[0].providedFilename", equalTo("file1")))
                .andExpect(jsonPath("$.[0]._links.download.href", useArtifactUrlHandler
                        ? equalTo("http://download-cdn.com/artifacts/%s/download".formatted(artifact.getFilename()))
                        : equalTo("http://localhost/rest/v1/softwaremodules/%s/artifacts/%s/download"
                                .formatted(sm.getId(), artifact.getId()))))
                .andExpect(jsonPath("$.[0]._links.self.href",
                        equalTo("http://localhost/rest/v1/softwaremodules/" + sm.getId() + "/artifacts/"
                                + artifact.getId())))
                .andExpect(jsonPath("$.[1].id", equalTo(artifact2.getId().intValue())))
                .andExpect(jsonPath("$.[1].hashes.md5", equalTo(artifact2.getMd5Hash())))
                .andExpect(jsonPath("$.[1].hashes.sha1", equalTo(artifact2.getSha1Hash())))
                .andExpect(jsonPath("$.[1].hashes.sha256", equalTo(artifact2.getSha256Hash())))
                .andExpect(jsonPath("$.[1].providedFilename", equalTo("file2")))
                .andExpect(jsonPath("$.[1]._links.download.href", useArtifactUrlHandler
                        ? equalTo("http://download-cdn.com/artifacts/%s/download".formatted(artifact2.getFilename()))
                        : equalTo("http://localhost/rest/v1/softwaremodules/%s/artifacts/%s/download"
                                .formatted(sm.getId(), artifact2.getId()))))
                .andExpect(jsonPath("$.[1]._links.self.href",
                        equalTo("http://localhost/rest/v1/softwaremodules/%s/artifacts/%s".formatted(sm.getId(),
                                artifact2.getId()))));
    }

    @Test
    @Description("Verifies that the system refuses unsupported request types and answers as defined to them, e.g. NOT FOUND on a non existing resource. Or a HTTP POST for updating a resource results in METHOD NOT ALLOWED etc.")
    void invalidRequestsOnArtifactResource() throws Exception {

        final int artifactSize = 5 * 1024;
        final byte[] random = randomBytes(artifactSize);
        final MockMultipartFile file = new MockMultipartFile("file", "orig", null, random);

        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        final SoftwareModule smSoftDeleted = testdataFactory.createSoftwareModuleOs("softDeleted");
        final Artifact artifactSoftDeleted = testdataFactory.createArtifacts(smSoftDeleted.getId()).get(0);
        testdataFactory.createDistributionSet(Arrays.asList(smSoftDeleted));
        softwareModuleManagement.delete(smSoftDeleted.getId());

        // no artifact available
        mvc.perform(get("/rest/v1/softwaremodules/{smId}/artifacts/1234567/download", sm.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        mvc.perform(delete("/rest/v1/softwaremodules/{smId}/artifacts/1234567", sm.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // SM does not exist
        artifactManagement
                .create(new ArtifactUpload(new ByteArrayInputStream(random), sm.getId(), "file1", false, artifactSize));
        mvc.perform(get("/rest/v1/softwaremodules/1234567890/artifacts")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        mvc.perform(multipart("/rest/v1/softwaremodules/1234567890/artifacts").file(file))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // bad request - no content
        mvc.perform(multipart("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        // not allowed methods
        mvc.perform(put("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(delete("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        // SM soft deleted
        mvc.perform(get("/rest/v1/softwaremodules/{smId}/artifacts/{artifactId}/download", smSoftDeleted.getId(),
                artifactSoftDeleted.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isGone());
    }

    @Test
    @Description("Tests the deletion of an artifact including verification that the artifact is actually erased in the repository and removed from the software module.")
    void deleteArtifact() throws Exception {
        // Create 1 SM
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        final int artifactSize = 5 * 1024;
        final byte random[] = RandomStringUtils.random(artifactSize).getBytes();

        // Create 2 artifacts
        final Artifact artifact = artifactManagement.create(
                new ArtifactUpload(new ByteArrayInputStream(random), sm.getId(), "file1", false, artifactSize));
        artifactManagement.create(
                new ArtifactUpload(new ByteArrayInputStream(random), sm.getId(), "file2", false, artifactSize));

        // check repo before delete
        assertThat(softwareModuleManagement.findAll(PAGE)).hasSize(1);

        assertThat(softwareModuleManagement.get(sm.getId()).get().getArtifacts()).hasSize(2);
        assertThat(artifactManagement.count()).isEqualTo(2);

        // delete
        mvc.perform(delete("/rest/v1/softwaremodules/{smId}/artifacts/{artId}", sm.getId(), artifact.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // check that only one artifact is still alive and still assigned
        assertThat(softwareModuleManagement.findAll(PAGE)).as("After the sm should be marked as deleted").hasSize(1);
        assertThat(artifactManagement.count()).isEqualTo(1);
        assertThat(softwareModuleManagement.get(sm.getId()).get().getArtifacts())
                .as("After delete artifact should available for marked as deleted sm's").hasSize(1);

    }

    @Test
    @Description("Verifies that the system refuses unsupported request types and answers as defined to them, e.g. NOT FOUND on a non existing resource. Or a HTTP POST for updating a resource results in METHOD NOT ALLOWED etc.")
    void invalidRequestsOnSoftwareModulesResource() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        final List<SoftwareModule> modules = Arrays.asList(sm);

        // SM does not exist
        mvc.perform(get("/rest/v1/softwaremodules/12345678"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        mvc.perform(delete("/rest/v1/softwaremodules/12345678"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // bad request - no content
        mvc.perform(post("/rest/v1/softwaremodules").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        // bad request - bad content
        mvc.perform(post("/rest/v1/softwaremodules").content("sdfjsdlkjfskdjf".getBytes())
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        mvc.perform(post("/rest/v1/softwaremodules")
                .content("[{\"description\":\"Desc123\",\"key\":\"test123\", \"type\":\"os\"}]")
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        final SoftwareModule toLongName = entityFactory.softwareModule().create().type(osType)
                .name(RandomStringUtils.randomAlphanumeric(80)).build();
        mvc.perform(post("/rest/v1/softwaremodules").content(JsonBuilder.softwareModules(Arrays.asList(toLongName)))
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        // unsupported media type
        mvc.perform(post("/rest/v1/softwaremodules").content(JsonBuilder.softwareModules(modules))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isUnsupportedMediaType());

        final SoftwareModule swm = entityFactory.softwareModule().create().name("encryptedModule").type(osType)
                .version("version").vendor("vendor").description("description").encrypted(true).build();
        // artifact decryption is not supported
        mvc.perform(
                post("/rest/v1/softwaremodules").content(JsonBuilder.softwareModules(Collections.singletonList(swm)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        // not allowed methods
        mvc.perform(put("/rest/v1/softwaremodules")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(delete("/rest/v1/softwaremodules")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

    }

    @Test
    @Description("Test of modules retrieval without any parameters. Will return all modules in the system as defined by standard page size.")
    void getSoftwareModulesWithoutAdditionalRequestParameters() throws Exception {
        final int modules = 5;
        createSoftwareModulesAlphabetical(modules);
        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(modules)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(modules)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(modules)));
    }

    @Test
    @Description("Test of modules retrieval with paging limit parameter. Will return all modules in the system as defined by given page size.")
    void detSoftwareModulesWithPagingLimitRequestParameter() throws Exception {
        final int modules = 5;
        final int limitSize = 1;
        createSoftwareModulesAlphabetical(modules);
        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING).param(
                        MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(limitSize)))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(modules)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(limitSize)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(limitSize)));
    }

    @Test
    @Description("Test of modules retrieval with paging limit offset parameters. Will return all modules in the system as defined by given page size starting from given offset.")
    void getSoftwareModulesWithPagingLimitAndOffsetRequestParameter() throws Exception {
        final int modules = 5;
        final int offsetParam = 2;
        final int expectedSize = modules - offsetParam;
        createSoftwareModulesAlphabetical(modules);
        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING).param(
                                MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(offsetParam))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(modules)))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(modules)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(expectedSize)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(expectedSize)));
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Test retrieval of all software modules the user has access to.")
    void getSoftwareModules() throws Exception {
        final SoftwareModule os = testdataFactory.createSoftwareModuleOs();
        final SoftwareModule app = testdataFactory.createSoftwareModuleApp();

        mvc.perform(get("/rest/v1/softwaremodules").accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content.[?(@.id==" + os.getId() + ")].name", contains(os.getName())))
                .andExpect(jsonPath("$.content.[?(@.id==" + os.getId() + ")].version", contains(os.getVersion())))
                .andExpect(
                        jsonPath("$.content.[?(@.id==" + os.getId() + ")].description", contains(os.getDescription())))
                .andExpect(jsonPath("$.content.[?(@.id==" + os.getId() + ")].vendor", contains(os.getVendor())))
                .andExpect(jsonPath("$.content.[?(@.id==" + os.getId() + ")].type", contains("os")))
                .andExpect(jsonPath("$.content.[?(@.id==" + os.getId() + ")].createdBy", contains("uploadTester")))
                .andExpect(jsonPath("$.content.[?(@.id==" + os.getId() + ")].createdAt", contains(os.getCreatedAt())))
                .andExpect(jsonPath("$.content.[?(@.id==" + os.getId() + ")]._links.self.href",
                        contains("http://localhost/rest/v1/softwaremodules/" + os.getId())))
                .andExpect(jsonPath("$.content.[?(@.id==" + app.getId() + ")].name", contains(app.getName())))
                .andExpect(jsonPath("$.content.[?(@.id==" + app.getId() + ")].version", contains(app.getVersion())))
                .andExpect(jsonPath("$.content.[?(@.id==" + app.getId() + ")].description",
                        contains(app.getDescription())))
                .andExpect(jsonPath("$.content.[?(@.id==" + app.getId() + ")].vendor", contains(app.getVendor())))
                .andExpect(jsonPath("$.content.[?(@.id==" + app.getId() + ")].type", contains("application")))
                .andExpect(jsonPath("$.content.[?(@.id==" + app.getId() + ")].createdBy", contains("uploadTester")))
                .andExpect(jsonPath("$.content.[?(@.id==" + app.getId() + ")].createdAt", contains(app.getCreatedAt())))
                .andExpect(jsonPath("$.content.[?(@.id==" + app.getId() + ")]._links.self.href",
                        contains("http://localhost/rest/v1/softwaremodules/" + app.getId())));

        assertThat(softwareModuleManagement.findAll(PAGE)).as("Softwaremodule size is wrong").hasSize(2);
    }

    @Test
    @Description("Test the various filter parameters, e.g. filter by name or type of the module.")
    void getSoftwareModulesWithFilterParameters() throws Exception {
        final SoftwareModule os1 = testdataFactory.createSoftwareModuleOs("1");
        final SoftwareModule app1 = testdataFactory.createSoftwareModuleApp("1");
        testdataFactory.createSoftwareModuleOs("2");
        final SoftwareModule app2 = testdataFactory.createSoftwareModuleApp("2");

        assertThat(softwareModuleManagement.findAll(PAGE)).hasSize(4);

        // only by name, only one exists per name
        mvc.perform(get("/rest/v1/softwaremodules?q=name==" + os1.getName()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content.[?(@.id==" + os1.getId() + ")].name", contains(os1.getName())))
                .andExpect(jsonPath("$.content.[?(@.id==" + os1.getId() + ")].version", contains(os1.getVersion())))
                .andExpect(jsonPath("$.content.[?(@.id==" + os1.getId() + ")].description",
                        contains(os1.getDescription())))
                .andExpect(jsonPath("$.content.[?(@.id==" + os1.getId() + ")].vendor", contains(os1.getVendor())))
                .andExpect(jsonPath("$.content.[?(@.id==" + os1.getId() + ")].type", contains("os")))
                .andExpect(jsonPath("$.content", hasSize(1))).andExpect(jsonPath("$.total", equalTo(1)));

        // by type, 2 software modules per type exists
        mvc.perform(get("/rest/v1/softwaremodules?q=type==" + Constants.SMT_DEFAULT_APP_KEY)
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content.[?(@.id==" + app1.getId() + ")].name", contains(app1.getName())))
                .andExpect(jsonPath("$.content.[?(@.id==" + app1.getId() + ")].version", contains(app1.getVersion())))
                .andExpect(jsonPath("$.content.[?(@.id==" + app1.getId() + ")].description",
                        contains(app1.getDescription())))
                .andExpect(jsonPath("$.content.[?(@.id==" + app1.getId() + ")].vendor", contains(app1.getVendor())))
                .andExpect(jsonPath("$.content.[?(@.id==" + app1.getId() + ")].type",
                        contains(Constants.SMT_DEFAULT_APP_KEY)))
                .andExpect(jsonPath("$.content.[?(@.id==" + app2.getId() + ")].name", contains(app2.getName())))
                .andExpect(jsonPath("$.content.[?(@.id==" + app2.getId() + ")].version", contains(app2.getVersion())))
                .andExpect(jsonPath("$.content.[?(@.id==" + app2.getId() + ")].description",
                        contains(app2.getDescription())))
                .andExpect(jsonPath("$.content.[?(@.id==" + app2.getId() + ")].vendor", contains(app2.getVendor())))
                .andExpect(jsonPath("$.content.[?(@.id==" + app2.getId() + ")].type",
                        contains(Constants.SMT_DEFAULT_APP_KEY)))
                .andExpect(jsonPath("$.content", hasSize(2))).andExpect(jsonPath("$.total", equalTo(2)));

        // by type and version=2.0.0 -> only one result
        mvc.perform(get(
                "/rest/v1/softwaremodules?q=type==" + Constants.SMT_DEFAULT_APP_KEY + ";version==" + app1.getVersion())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content.[?(@.id==" + app1.getId() + ")].name", contains(app1.getName())))
                .andExpect(jsonPath("$.content.[?(@.id==" + app1.getId() + ")].version", contains(app1.getVersion())))
                .andExpect(jsonPath("$.content.[?(@.id==" + app1.getId() + ")].description",
                        contains(app1.getDescription())))
                .andExpect(jsonPath("$.content.[?(@.id==" + app1.getId() + ")].vendor", contains(app1.getVendor())))
                .andExpect(jsonPath("$.content.[?(@.id==" + app1.getId() + ")].type",
                        contains(Constants.SMT_DEFAULT_APP_KEY)))
                .andExpect(jsonPath("$.content", hasSize(1))).andExpect(jsonPath("$.total", equalTo(1)));
    }

    @Test
    @Description("Verifies that the system answers as defined in case of a wrong filter parameter syntax. Expected result: BAD REQUEST with error description.")
    void getSoftwareModulesWithSyntaxErrorFilterParameter() throws Exception {
        mvc.perform(get("/rest/v1/softwaremodules?q=wrongFIQLSyntax").accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", equalTo("hawkbit.server.error.rest.param.rsqlParamSyntax")));
    }

    @Test
    @Description("Verifies that the system answers as defined in case of a non existing field used in filter. Expected result: BAD REQUEST with error description.")
    void getSoftwareModulesWithUnknownFieldErrorFilterParameter() throws Exception {
        mvc.perform(get("/rest/v1/softwaremodules?q=wrongField==abc").accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", equalTo("hawkbit.server.error.rest.param.rsqlInvalidField")));
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Tests GET request on /rest/v1/softwaremodules/{smId}.")
    void getSoftwareModule() throws Exception {
        final SoftwareModule os = testdataFactory.createSoftwareModuleOs();

        mvc.perform(get("/rest/v1/softwaremodules/{smId}", os.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.name", equalTo(os.getName())))
                .andExpect(jsonPath("$.version", equalTo(os.getVersion())))
                .andExpect(jsonPath("$.description", equalTo(os.getDescription())))
                .andExpect(jsonPath("$.vendor", equalTo(os.getVendor())))
                .andExpect(jsonPath("$.type", equalTo(os.getType().getKey())))
                .andExpect(jsonPath("$.deleted", equalTo(os.isDeleted())))
                .andExpect(jsonPath("$.createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("$.createdAt", equalTo(os.getCreatedAt())))
                .andExpect(jsonPath("$._links.metadata.href",
                        equalTo("http://localhost/rest/v1/softwaremodules/" + os.getId()
                                + "/metadata?offset=0&limit=50")))
                .andExpect(jsonPath("$._links.type.href",
                        equalTo("http://localhost/rest/v1/softwaremoduletypes/" + osType.getId())))
                .andExpect(jsonPath("$._links.artifacts.href",
                        equalTo("http://localhost/rest/v1/softwaremodules/" + os.getId() + "/artifacts")));

        assertThat(softwareModuleManagement.findAll(PAGE)).as("Softwaremodule size is wrong").hasSize(1);
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Verifies that the create request actually results in the creation of the modules in the repository.")
    void createSoftwareModules() throws Exception {
        final SoftwareModule os = entityFactory.softwareModule()
                .create()
                .name("name1")
                .type(osType)
                .version("version1")
                .vendor("vendor1")
                .description("description1")
                .build();
        final SoftwareModule ah = entityFactory.softwareModule()
                .create()
                .name("name3")
                .type(appType)
                .version("version3")
                .vendor("vendor3")
                .description("description3")
                .build();

        final List<SoftwareModule> modules = Arrays.asList(os, ah);

        final long current = System.currentTimeMillis();

        final MvcResult mvcResult = mvc.perform(
                        post("/rest/v1/softwaremodules").accept(MediaType.APPLICATION_JSON_VALUE)
                                .content(JsonBuilder.softwareModules(modules))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("[0].name", equalTo("name1")))
                .andExpect(jsonPath("[0].version", equalTo("version1")))
                .andExpect(jsonPath("[0].description", equalTo("description1")))
                .andExpect(jsonPath("[0].vendor", equalTo("vendor1")))
                .andExpect(jsonPath("[0].type", equalTo("os")))
                .andExpect(jsonPath("[0].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[1].name", equalTo("name3")))
                .andExpect(jsonPath("[1].version", equalTo("version3")))
                .andExpect(jsonPath("[1].description", equalTo("description3")))
                .andExpect(jsonPath("[1].vendor", equalTo("vendor3")))
                .andExpect(jsonPath("[1].type", equalTo("application")))
                .andExpect(jsonPath("[1].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[1].createdAt", not(equalTo(0))))
                .andReturn();

        final SoftwareModule osCreated = softwareModuleManagement.getByNameAndVersionAndType("name1", "version1",
                osType.getId()).get();
        final SoftwareModule appCreated = softwareModuleManagement.getByNameAndVersionAndType("name3", "version3",
                appType.getId()).get();

        assertThat(JsonPath.compile("[0]._links.self.href")
                .read(mvcResult.getResponse().getContentAsString())
                .toString()).as("Response contains invalid self href")
                .isEqualTo("http://localhost/rest/v1/softwaremodules/" + osCreated.getId());

        assertThat(JsonPath.compile("[1]._links.self.href")
                .read(mvcResult.getResponse().getContentAsString())
                .toString()).as("Response contains links self href")
                .isEqualTo("http://localhost/rest/v1/softwaremodules/" + appCreated.getId());

        assertThat(softwareModuleManagement.findAll(PAGE)).as("Wrong softwaremodule size").hasSize(2);
        assertThat(softwareModuleManagement.findByType(PAGE, osType.getId()).getContent().get(0).getName()).as(
                "Softwaremoudle name is wrong").isEqualTo(os.getName());
        assertThat(softwareModuleManagement.findByType(PAGE, osType.getId()).getContent().get(0).getCreatedBy()).as(
                "Softwaremoudle created by is wrong").isEqualTo("uploadTester");
        assertThat(softwareModuleManagement.findByType(PAGE, osType.getId()).getContent().get(0).getCreatedAt()).as(
                "Softwaremoudle created at is wrong").isGreaterThanOrEqualTo(current);
        assertThat(softwareModuleManagement.findByType(PAGE, appType.getId()).getContent().get(0).getName()).as(
                "Softwaremoudle name is wrong").isEqualTo(ah.getName());
    }

    @Test
    @Description("Verifies successfull deletion of software modules that are not in use, i.e. assigned to a DS.")
    void deleteUnassignedSoftwareModule() throws Exception {

        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        final int artifactSize = 5 * 1024;
        final byte random[] = RandomStringUtils.random(artifactSize).getBytes();

        artifactManagement.create(
                new ArtifactUpload(new ByteArrayInputStream(random), sm.getId(), "file1", false, artifactSize));

        assertThat(softwareModuleManagement.findAll(PAGE)).as("Softwaremoudle size is wrong").hasSize(1);
        assertThat(artifactManagement.count()).isEqualTo(1);

        mvc.perform(delete("/rest/v1/softwaremodules/{smId}", sm.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        assertThat(softwareModuleManagement.findAll(PAGE)).as("After delete no softwarmodule should be available")
                .isEmpty();
        assertThat(artifactManagement.count()).isZero();
    }

    @Test
    @Description("Verifies successfull deletion of a software module that is in use, i.e. assigned to a DS which should result in movinf the module to the archive.")
    void deleteAssignedSoftwareModule() throws Exception {
        final DistributionSet ds1 = testdataFactory.createDistributionSet("a");

        final int artifactSize = 5 * 1024;
        final byte random[] = RandomStringUtils.random(artifactSize).getBytes();

        final Long appTypeSmId = ds1.findFirstModuleByType(appType).get().getId();

        artifactManagement.create(
                new ArtifactUpload(new ByteArrayInputStream(random), appTypeSmId, "file1", false, artifactSize));

        assertThat(softwareModuleManagement.count()).isEqualTo(3);
        assertThat(artifactManagement.count()).isEqualTo(1);

        mvc.perform(get("/rest/v1/softwaremodules/{smId}", appTypeSmId)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("$.deleted", equalTo(false)));

        mvc.perform(delete("/rest/v1/softwaremodules/{smId}", appTypeSmId)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        mvc.perform(get("/rest/v1/softwaremodules/{smId}", appTypeSmId)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("$.deleted", equalTo(true)));

        assertThat(softwareModuleManagement.count()).isEqualTo(2);
        assertThat(artifactManagement.count()).isEqualTo(1);
    }

    @Test
    @Description("Verifies the successful creation of metadata and the enforcement of the meta data quota.")
    void createMetadata() throws Exception {

        final String knownKey1 = "knownKey1";
        final String knownValue1 = "knownValue1";
        final String knownKey2 = "knownKey2";
        final String knownValue2 = "knownValue1";

        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        final JSONArray metaData1 = new JSONArray();
        metaData1.put(new JSONObject().put("key", knownKey1).put("value", knownValue1));
        metaData1.put(new JSONObject().put("key", knownKey2).put("value", knownValue2).put("targetVisible", true));

        mvc.perform(post("/rest/v1/softwaremodules/{swId}/metadata", sm.getId()).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(metaData1.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("[0].key", equalTo(knownKey1)))
                .andExpect(jsonPath("[0].value", equalTo(knownValue1)))
                .andExpect(jsonPath("[0].targetVisible", equalTo(false)))
                .andExpect(jsonPath("[1].key", equalTo(knownKey2)))
                .andExpect(jsonPath("[1].value", equalTo(knownValue2)))
                .andExpect(jsonPath("[1].targetVisible", equalTo(true)));

        assertThat(softwareModuleManagement.getMetaDataBySoftwareModuleId(sm.getId(), knownKey1))
                .as("Metadata key is wrong").get().extracting(SoftwareModuleMetadata::getValue).isEqualTo(knownValue1);
        assertThat(softwareModuleManagement.getMetaDataBySoftwareModuleId(sm.getId(), knownKey2))
                .as("Metadata key is wrong").get().extracting(SoftwareModuleMetadata::getValue).isEqualTo(knownValue2);

        // verify quota enforcement
        final int maxMetaData = quotaManagement.getMaxMetaDataEntriesPerSoftwareModule();

        final JSONArray metaData2 = new JSONArray();
        for (int i = 0; i < maxMetaData - metaData1.length() + 1; ++i) {
            metaData2.put(new JSONObject().put("key", knownKey1 + i).put("value", knownValue1 + i));
        }

        mvc.perform(post("/rest/v1/softwaremodules/{swId}/metadata", sm.getId()).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(metaData2.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isForbidden());

        // verify that the number of meta data entries has not changed
        // (we cannot use the PAGE constant here as it tries to sort by ID)
        assertThat(softwareModuleManagement
                .findMetaDataBySoftwareModuleId(PageRequest.of(0, Integer.MAX_VALUE), sm.getId()).getTotalElements())
                        .isEqualTo(metaData1.length());

    }

    @Test
    @Description(" Get a paged list of meta data for a software module.")
    public void getMetadata() throws Exception {
        final int totalMetadata = 4;
        final String knownKeyPrefix = "knownKey";
        final String knownValuePrefix = "knownValue";
        final SoftwareModule module = testdataFactory.createDistributionSet("one").findFirstModuleByType(osType).get();

        for (int index = 0; index < totalMetadata; index++) {
            softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata().create(module.getId())
                    .key(knownKeyPrefix + index).value(knownValuePrefix + index));
        }

        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING + "/{softwareModuleId}/metadata",
                        module.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON));
    }

    @Test
    @Description(" Get a paged list of meta data for a software module with defined page size and sorting by name descending and key starting with 'known'.")
    public void getMetadataWithParameters() throws Exception {
        final int totalMetadata = 4;
        final String knownKeyPrefix = "knownKey";
        final String knownValuePrefix = "knownValue";
        final SoftwareModule module = testdataFactory.createDistributionSet("one").findFirstModuleByType(osType).get();

        for (int index = 0; index < totalMetadata; index++) {
            softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata().create(module.getId())
                    .key(knownKeyPrefix + index).value(knownValuePrefix + index));
        }

        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING + "/{softwareModuleId}/metadata",
                        module.getId()).param("offset", "1").param("limit", "2").param("sort", "key:DESC").param("q",
                        "key==known*"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON));
    }

    @Test
    @Description(" Get a single meta data value for a meta data key." )
    public void getMetadataValue() throws Exception {

        // prepare and create metadata
        final String knownKey = "knownKey";
        final String knownValue = "knownValue";
        final SoftwareModule module = testdataFactory.createDistributionSet("one").findFirstModuleByType(osType).get();
        softwareModuleManagement.createMetaData(
                entityFactory.softwareModuleMetadata().create(module.getId()).key(knownKey).value(knownValue));

        mvc.perform(
                        get(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING + "/{softwareModuleId}/metadata/{metadataKey}",
                                module.getId(), knownKey))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    @Test
    @Description("Verifies the successful update of metadata based on given key.")
    void updateMetadataKey() throws Exception {
        // prepare and create metadata for update
        final String knownKey = "knownKey";
        final String knownValue = "knownValue";
        final String updateValue = "valueForUpdate";

        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();
        softwareModuleManagement.createMetaData(
                entityFactory.softwareModuleMetadata().create(sm.getId()).key(knownKey).value(knownValue));

        final JSONObject jsonObject = new JSONObject().put("key", knownKey)
                .put("value", updateValue)
                .put("targetVisible", true);

        mvc.perform(put("/rest/v1/softwaremodules/{swId}/metadata/{key}", sm.getId(), knownKey).accept(
                        MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON).content(jsonObject.toString()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("key", equalTo(knownKey)))
                .andExpect(jsonPath("value", equalTo(updateValue)));

        final SoftwareModuleMetadata assertDS = softwareModuleManagement.getMetaDataBySoftwareModuleId(sm.getId(),
                knownKey).get();
        assertThat(assertDS.getValue()).as("Metadata is wrong").isEqualTo(updateValue);
        assertThat(assertDS.isTargetVisible()).as("target visible is wrong").isTrue();
    }

    @Test
    @Description("Verifies the successful deletion of metadata entry.")
    void deleteMetadata() throws Exception {
        // prepare and create metadata for deletion
        final String knownKey = "knownKey";
        final String knownValue = "knownValue";

        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();
        softwareModuleManagement.createMetaData(
                entityFactory.softwareModuleMetadata().create(sm.getId()).key(knownKey).value(knownValue));

        mvc.perform(delete("/rest/v1/softwaremodules/{swId}/metadata/{key}", sm.getId(), knownKey))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        assertThat(softwareModuleManagement.getMetaDataBySoftwareModuleId(sm.getId(), knownKey)).isNotPresent();
    }

    @Test
    @Description("Ensures that module metadata deletion request to API on an entity that does not exist results in NOT_FOUND.")
    void deleteModuleMetadataThatDoesNotExistLeadsToNotFound() throws Exception {
        // prepare and create metadata for deletion
        final String knownKey = "knownKey";
        final String knownValue = "knownValue";

        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();
        softwareModuleManagement.createMetaData(
                entityFactory.softwareModuleMetadata().create(sm.getId()).key(knownKey).value(knownValue));

        mvc.perform(delete("/rest/v1/softwaremodules/{swId}/metadata/XXX", sm.getId(), knownKey))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        mvc.perform(delete("/rest/v1/softwaremodules/1234/metadata/{key}", knownKey))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        assertThat(softwareModuleManagement.getMetaDataBySoftwareModuleId(sm.getId(), knownKey)).isPresent();
    }

    @Test
    @Description("Ensures that module deletion request to API on an entity that does not exist results in NOT_FOUND.")
    void deleteSoftwareModuleThatDoesNotExistLeadsToNotFound() throws Exception {
        mvc.perform(delete("/rest/v1/softwaremodules/1234"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Verifies the successful search of a metadata entry based on value.")
    void searchSoftwareModuleMetadataRsql() throws Exception {
        final int totalMetadata = 10;
        final String knownKeyPrefix = "knownKey";
        final String knownValuePrefix = "knownValue";
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        for (int index = 0; index < totalMetadata; index++) {
            softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata()
                    .create(sm.getId())
                    .key(knownKeyPrefix + index)
                    .value(knownValuePrefix + index));
        }

        final String rsqlSearchValue1 = "value==knownValue1";

        mvc.perform(get("/rest/v1/softwaremodules/{swId}/metadata?q=" + rsqlSearchValue1, sm.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath("total", equalTo(1))).andExpect(jsonPath("content[0].key", equalTo("knownKey1")))
                .andExpect(jsonPath("content[0].value", equalTo("knownValue1")));
    }

    private void createSoftwareModulesAlphabetical(final int amount) {
        char character = 'a';
        for (int index = 0; index < amount; index++) {
            final String str = String.valueOf(character);
            softwareModuleManagement.create(entityFactory.softwareModule().create().type(osType).name(str)
                    .description(str).vendor(str).version(str));
            character++;
        }
    }

    private static byte[] randomBytes(final int len) {
        return RandomStringUtils.randomAlphanumeric(len).getBytes();
    }

}
