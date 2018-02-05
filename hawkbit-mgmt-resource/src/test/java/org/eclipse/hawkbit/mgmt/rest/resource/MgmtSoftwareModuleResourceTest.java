/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.mgmt.json.model.artifact.MgmtArtifact;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.Constants;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.test.util.HashGeneratorUtils;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.eclipse.hawkbit.rest.json.model.ExceptionInfo;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.RestController;

import com.jayway.jsonpath.JsonPath;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Tests for {@link MgmtSoftwareModuleResource} {@link RestController}.
 *
 */
@Features("Component Tests - Management API")
@Stories("Software Module Resource")
public class MgmtSoftwareModuleResourceTest extends AbstractManagementApiIntegrationTest {

    @Before
    public void assertPreparationOfRepo() {
        assertThat(softwareModuleManagement.findAll(PAGE)).as("no softwaremodule should be founded").hasSize(0);
    }

    @Test
    @Description("Tests the update of software module metadata. It is verfied that only the selected fields for the update are really updated and the modification values are filled (i.e. updated by and at).")
    @WithUser(principal = "smUpdateTester", allSpPermissions = true)
    public void updateSoftwareModuleOnlyDescriptionAndVendorNameUntouched() throws Exception {
        final String knownSWName = "name1";
        final String knownSWVersion = "version1";
        final String knownSWDescription = "description1";
        final String knownSWVendor = "vendor1";

        final String updateVendor = "newVendor1";
        final String updateDescription = "newDescription1";

        SoftwareModule sm = softwareModuleManagement.create(entityFactory.softwareModule().create().type(osType)
                .name(knownSWName).version(knownSWVersion).description(knownSWDescription).vendor(knownSWVendor));

        assertThat(sm.getName()).as("Wrong name of the software module").isEqualTo(knownSWName);

        final String body = new JSONObject().put("vendor", updateVendor).put("description", updateDescription)
                .put("name", "nameShouldNotBeChanged").toString();

        // ensures that we are not to fast so that last modified is not set
        // correctly
        Thread.sleep(1);

        mvc.perform(put("/rest/v1/softwaremodules/{smId}", sm.getId()).content(body)
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(sm.getId().intValue())))
                .andExpect(jsonPath("$.vendor", equalTo(updateVendor)))
                .andExpect(jsonPath("$.lastModifiedBy", equalTo("smUpdateTester")))
                .andExpect(jsonPath("$.lastModifiedAt", not(equalTo(sm.getLastModifiedAt()))))
                .andExpect(jsonPath("$.description", equalTo(updateDescription)))
                .andExpect(jsonPath("$.name", equalTo(knownSWName))).andReturn();

        sm = softwareModuleManagement.get(sm.getId()).get();
        assertThat(sm.getName()).isEqualTo(knownSWName);
        assertThat(sm.getVendor()).isEqualTo(updateVendor);
        assertThat(sm.getLastModifiedBy()).isEqualTo("smUpdateTester");
        assertThat(sm.getDescription()).isEqualTo(updateDescription);

    }

    @Test
    @Description("Tests the uppload of an artifact binary. The upload is executed and the content checked in the repository for completenes.")
    public void uploadArtifact() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        // create test file
        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();
        final String md5sum = HashGeneratorUtils.generateMD5(random);
        final String sha1sum = HashGeneratorUtils.generateSHA1(random);
        final MockMultipartFile file = new MockMultipartFile("file", "origFilename", null, random);

        // upload
        final MvcResult mvcResult = mvc
                .perform(fileUpload("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()).file(file)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.hashes.md5", equalTo(md5sum)))
                .andExpect(jsonPath("$.hashes.sha1", equalTo(sha1sum)))
                .andExpect(jsonPath("$.size", equalTo(random.length)))
                .andExpect(jsonPath("$.providedFilename", equalTo("origFilename"))).andReturn();

        // check rest of response compared to DB
        final MgmtArtifact artResult = ResourceUtility
                .convertArtifactResponse(mvcResult.getResponse().getContentAsString());
        final Long artId = softwareModuleManagement.get(sm.getId()).get().getArtifacts().get(0).getId();
        assertThat(artResult.getArtifactId()).as("Wrong artifact id").isEqualTo(artId);
        assertThat(JsonPath.compile("$._links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                .as("Link contains no self url")
                .isEqualTo("http://localhost/rest/v1/softwaremodules/" + sm.getId() + "/artifacts/" + artId);
        assertThat(JsonPath.compile("$._links.download.href").read(mvcResult.getResponse().getContentAsString())
                .toString()).as("response contains no download url ").isEqualTo(
                        "http://localhost/rest/v1/softwaremodules/" + sm.getId() + "/artifacts/" + artId + "/download");

        assertArtifact(sm, random);
    }

    private void assertArtifact(final SoftwareModule sm, final byte[] random) throws IOException {
        // check result in db...
        // repo
        assertThat(artifactManagement.count()).as("Wrong artifact size").isEqualTo(1);

        // binary
        try (InputStream fileInputStream = artifactManagement
                .loadArtifactBinary(softwareModuleManagement.get(sm.getId()).get().getArtifacts().get(0).getSha1Hash())
                .get().getFileInputStream()) {
            assertTrue("Wrong artifact content",
                    IOUtils.contentEquals(new ByteArrayInputStream(random), fileInputStream));
        }

        // hashes
        assertThat(artifactManagement.getByFilename("origFilename").get().getSha1Hash()).as("Wrong sha1 hash")
                .isEqualTo(HashGeneratorUtils.generateSHA1(random));

        assertThat(artifactManagement.getByFilename("origFilename").get().getMd5Hash()).as("Wrong md5 hash")
                .isEqualTo(HashGeneratorUtils.generateMD5(random));

        // metadata
        assertThat(softwareModuleManagement.get(sm.getId()).get().getArtifacts().get(0).getFilename())
                .as("wrong metadata of the filename").isEqualTo("origFilename");
    }

    @Test
    @Description("Verfies that the system does not accept empty artifact uploads. Expected response: BAD REQUEST")
    public void emptyUploadArtifact() throws Exception {
        assertThat(softwareModuleManagement.findAll(PAGE)).hasSize(0);
        assertThat(artifactManagement.count()).isEqualTo(0);

        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        final MockMultipartFile file = new MockMultipartFile("file", "orig", null, new byte[0]);

        mvc.perform(fileUpload("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()).file(file)
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Verfies that the system does not accept identical artifacts uploads for the same software module. Expected response: CONFLICT")
    public void duplicateUploadArtifact() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();
        final String md5sum = HashGeneratorUtils.generateMD5(random);
        final String sha1sum = HashGeneratorUtils.generateSHA1(random);
        final MockMultipartFile file = new MockMultipartFile("file", "orig", null, random);

        mvc.perform(fileUpload("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()).file(file)
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.hashes.md5", equalTo(md5sum)))
                .andExpect(jsonPath("$.hashes.sha1", equalTo(sha1sum)))
                .andExpect(jsonPath("$.providedFilename", equalTo("orig"))).andExpect(status().isCreated());

        mvc.perform(fileUpload("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()).file(file))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isConflict());
    }

    @Test
    @Description("verfies that option to upload artifacts with a custom defined by metadata, i.e. not the file name of the binary itself.")
    public void uploadArtifactWithCustomName() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();
        assertThat(artifactManagement.count()).isEqualTo(0);

        // create test file
        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();
        final MockMultipartFile file = new MockMultipartFile("file", "origFilename", null, random);

        // upload
        mvc.perform(fileUpload("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()).file(file)
                .param("filename", "customFilename").accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.providedFilename", equalTo("customFilename"))).andExpect(status().isCreated());

        // check result in db...
        // repo
        assertThat(artifactManagement.count()).isEqualTo(1);

        // hashes
        assertThat(artifactManagement.getByFilename("customFilename")).as("Local artifact is wrong").isPresent();
    }

    @Test
    @Description("Verfies that the system refuses upload of an artifact where the provided hash sums do not match. Expected result: BAD REQUEST")
    public void uploadArtifactWithHashCheck() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();
        assertThat(artifactManagement.count()).isEqualTo(0);

        // create test file
        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();
        final String md5sum = HashGeneratorUtils.generateMD5(random);
        final String sha1sum = HashGeneratorUtils.generateSHA1(random);
        final MockMultipartFile file = new MockMultipartFile("file", "origFilename", null, random);

        // upload
        // wrong sha1
        MvcResult mvcResult = mvc
                .perform(fileUpload("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()).file(file)
                        .param("md5sum", md5sum).param("sha1sum", "afsdff"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andReturn();

        // check error result
        ExceptionInfo exceptionInfo = ResourceUtility.convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getErrorCode()).as("Exception contains wrong error code")
                .isEqualTo(SpServerError.SP_ARTIFACT_UPLOAD_FAILED_SHA1_MATCH.getKey());

        // wrong md5
        mvcResult = mvc
                .perform(fileUpload("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()).file(file)
                        .param("md5sum", "sdfsdfs").param("sha1sum", sha1sum))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andReturn();

        // check error result
        exceptionInfo = ResourceUtility.convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getErrorCode()).as("Exception contains wrong error code")
                .isEqualTo(SpServerError.SP_ARTIFACT_UPLOAD_FAILED_MD5_MATCH.getKey());

        mvc.perform(fileUpload("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()).file(file)
                .param("md5sum", md5sum).param("sha1sum", sha1sum)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated());

        assertArtifact(sm, random);

    }

    @Test
    @Description("Tests binary download of an artifact including verfication that the downloaded binary is consistent and that the etag header is as expected identical to the SHA1 hash of the file.")
    public void downloadArtifact() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();

        final Artifact artifact = artifactManagement.create(new ByteArrayInputStream(random), sm.getId(), "file1",
                false);
        final Artifact artifact2 = artifactManagement.create(new ByteArrayInputStream(random), sm.getId(), "file2",
                false);

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

        assertTrue("Wrong response content", Arrays.equals(result.getResponse().getContentAsByteArray(), random));
    }

    @Test
    @Description("Verifies the listing of one defined artifact assigned to a given software module. That includes the artifact metadata and download links.")
    public void getArtifact() throws Exception {
        // prepare data for test
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();
        final Artifact artifact = artifactManagement.create(new ByteArrayInputStream(random), sm.getId(), "file1",
                false);

        // perform test
        mvc.perform(get("/rest/v1/softwaremodules/{smId}/artifacts/{artId}", sm.getId(), artifact.getId())
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.id", equalTo(artifact.getId().intValue())))
                .andExpect(jsonPath("$.size", equalTo(random.length)))
                .andExpect(jsonPath("$.hashes.md5", equalTo(artifact.getMd5Hash())))
                .andExpect(jsonPath("$.hashes.sha1", equalTo(artifact.getSha1Hash())))
                .andExpect(jsonPath("$.providedFilename", equalTo("file1")))
                .andExpect(jsonPath("$._links.download.href",
                        equalTo("http://localhost/rest/v1/softwaremodules/" + sm.getId() + "/artifacts/"
                                + artifact.getId() + "/download")))
                .andExpect(jsonPath("$._links.self.href", equalTo(
                        "http://localhost/rest/v1/softwaremodules/" + sm.getId() + "/artifacts/" + artifact.getId())));
    }

    @Test
    @Description("Verifies the listing of all artifacts assigned to a software module. That includes the artifact metadata and download links.")
    public void getArtifacts() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();

        final Artifact artifact = artifactManagement.create(new ByteArrayInputStream(random), sm.getId(), "file1",
                false);
        final Artifact artifact2 = artifactManagement.create(new ByteArrayInputStream(random), sm.getId(), "file2",
                false);

        mvc.perform(get("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.[0].id", equalTo(artifact.getId().intValue())))
                .andExpect(jsonPath("$.[0].size", equalTo(random.length)))
                .andExpect(jsonPath("$.[0].hashes.md5", equalTo(artifact.getMd5Hash())))
                .andExpect(jsonPath("$.[0].hashes.sha1", equalTo(artifact.getSha1Hash())))
                .andExpect(jsonPath("$.[0].providedFilename", equalTo("file1")))
                .andExpect(jsonPath("$.[0]._links.self.href",
                        equalTo("http://localhost/rest/v1/softwaremodules/" + sm.getId() + "/artifacts/"
                                + artifact.getId())))
                .andExpect(jsonPath("$.[1].id", equalTo(artifact2.getId().intValue())))
                .andExpect(jsonPath("$.[1].hashes.md5", equalTo(artifact2.getMd5Hash())))
                .andExpect(jsonPath("$.[1].hashes.sha1", equalTo(artifact2.getSha1Hash())))
                .andExpect(jsonPath("$.[1].providedFilename", equalTo("file2")))
                .andExpect(jsonPath("$.[1]._links.self.href", equalTo(
                        "http://localhost/rest/v1/softwaremodules/" + sm.getId() + "/artifacts/" + artifact2.getId())));
    }

    @Test
    @Description("Verfies that the system refuses unsupported request types and answers as defined to them, e.g. NOT FOUND on a non existing resource. Or a HTTP POST for updating a resource results in METHOD NOT ALLOWED etc.")
    public void invalidRequestsOnArtifactResource() throws Exception {
        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();
        final MockMultipartFile file = new MockMultipartFile("file", "orig", null, random);

        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        // no artifact available
        mvc.perform(get("/rest/v1/softwaremodules/{smId}/artifacts/1234567/download", sm.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        mvc.perform(delete("/rest/v1/softwaremodules/{smId}/artifacts/1234567", sm.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // SM does not exist
        artifactManagement.create(new ByteArrayInputStream(random), sm.getId(), "file1", false);
        mvc.perform(get("/rest/v1/softwaremodules/1234567890/artifacts")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        mvc.perform(fileUpload("/rest/v1/softwaremodules/1234567890/artifacts").file(file))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // bad request - no content
        mvc.perform(fileUpload("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        // not allowed methods
        mvc.perform(put("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(delete("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @Description("Verfies that the system refuses unsupported request types and answers as defined to them, e.g. NOT FOUND on a non existing resource. Or a HTTP POST for updating a resource results in METHOD NOT ALLOWED etc.")
    public void invalidRequestsOnSoftwaremodulesResource() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        final List<SoftwareModule> modules = Arrays.asList(sm);

        // SM does not exist
        mvc.perform(get("/rest/v1/softwaremodules/12345678")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        mvc.perform(delete("/rest/v1/softwaremodules/12345678")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // bad request - no content
        mvc.perform(post("/rest/v1/softwaremodules").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

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

        // not allowed methods
        mvc.perform(put("/rest/v1/softwaremodules")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(delete("/rest/v1/softwaremodules")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

    }

    @Test
    @Description("Test of modules retrieval without any parameters. Will return all modules in the system as defined by standard page size.")
    public void getSoftwareModulesWithoutAddtionalRequestParameters() throws Exception {
        final int modules = 5;
        createSoftwareModulesAlphabetical(modules);
        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(modules)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(modules)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(modules)));
    }

    @Test
    @Description("Test of modules retrieval with paging limit parameter. Will return all modules in the system as defined by given page size.")
    public void detSoftwareModulesWithPagingLimitRequestParameter() throws Exception {
        final int modules = 5;
        final int limitSize = 1;
        createSoftwareModulesAlphabetical(modules);
        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING)
                .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(limitSize)))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(modules)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(limitSize)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(limitSize)));
    }

    @Test
    @Description("Test of modules retrieval with paging limit offset parameters. Will return all modules in the system as defined by given page size starting from given offset.")
    public void getSoftwareModulesWithPagingLimitAndOffsetRequestParameter() throws Exception {
        final int modules = 5;
        final int offsetParam = 2;
        final int expectedSize = modules - offsetParam;
        createSoftwareModulesAlphabetical(modules);
        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING)
                .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(offsetParam))
                .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(modules)))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(modules)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(expectedSize)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(expectedSize)));
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Test retrieval of all software modules the user has access to.")
    public void getSoftwareModules() throws Exception {
        final SoftwareModule os = testdataFactory.createSoftwareModuleOs();
        final SoftwareModule app = testdataFactory.createSoftwareModuleApp();

        mvc.perform(get("/rest/v1/softwaremodules").accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
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
    public void getSoftwareModulesWithFilterParameters() throws Exception {
        final SoftwareModule os1 = testdataFactory.createSoftwareModuleOs("1");
        final SoftwareModule app1 = testdataFactory.createSoftwareModuleApp("1");
        testdataFactory.createSoftwareModuleOs("2");
        final SoftwareModule app2 = testdataFactory.createSoftwareModuleApp("2");

        assertThat(softwareModuleManagement.findAll(PAGE)).hasSize(4);

        // only by name, only one exists per name
        mvc.perform(get("/rest/v1/softwaremodules?q=name==" + os1.getName()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
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
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
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
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
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
    @Description("Verfies that the system answers as defined in case of a wrong filter parameter syntax. Expected result: BAD REQUEST with error description.")
    public void getSoftwareModulesWithSyntaxErrorFilterParameter() throws Exception {
        mvc.perform(get("/rest/v1/softwaremodules?q=wrongFIQLSyntax").accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", equalTo("hawkbit.server.error.rest.param.rsqlParamSyntax")));
    }

    @Test
    @Description("Verfies that the system answers as defined in case of a wnon extsing field used in filter. Expected result: BAD REQUEST with error description.")
    public void getSoftwareModulesWithUnknownFieldErrorFilterParameter() throws Exception {
        mvc.perform(get("/rest/v1/softwaremodules?q=wrongField==abc").accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", equalTo("hawkbit.server.error.rest.param.rsqlInvalidField")));
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Tests GET request on /rest/v1/softwaremodules/{smId}.")
    public void getSoftwareModule() throws Exception {
        final SoftwareModule os = testdataFactory.createSoftwareModuleOs();

        mvc.perform(get("/rest/v1/softwaremodules/{smId}", os.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.name", equalTo(os.getName())))
                .andExpect(jsonPath("$.version", equalTo(os.getVersion())))
                .andExpect(jsonPath("$.description", equalTo(os.getDescription())))
                .andExpect(jsonPath("$.vendor", equalTo(os.getVendor())))
                .andExpect(jsonPath("$.type", equalTo(os.getType().getKey())))
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
    @Description("Verfies that the create request actually results in the creation of the modules in the repository.")
    public void createSoftwareModules() throws Exception {
        final SoftwareModule os = entityFactory.softwareModule().create().name("name1").type(osType).version("version1")
                .vendor("vendor1").description("description1").build();
        final SoftwareModule ah = entityFactory.softwareModule().create().name("name3").type(appType)
                .version("version3").vendor("vendor3").description("description3").build();

        final List<SoftwareModule> modules = Arrays.asList(os, ah);

        final long current = System.currentTimeMillis();

        final MvcResult mvcResult = mvc
                .perform(post("/rest/v1/softwaremodules/").accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
                        .content(JsonBuilder.softwareModules(modules)).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("[0].name", equalTo("name1")))
                .andExpect(jsonPath("[0].version", equalTo("version1")))
                .andExpect(jsonPath("[0].description", equalTo("description1")))
                .andExpect(jsonPath("[0].vendor", equalTo("vendor1"))).andExpect(jsonPath("[0].type", equalTo("os")))
                .andExpect(jsonPath("[0].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[1].name", equalTo("name3")))
                .andExpect(jsonPath("[1].version", equalTo("version3")))
                .andExpect(jsonPath("[1].description", equalTo("description3")))
                .andExpect(jsonPath("[1].vendor", equalTo("vendor3")))
                .andExpect(jsonPath("[1].type", equalTo("application")))
                .andExpect(jsonPath("[1].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[1].createdAt", not(equalTo(0)))).andReturn();

        final SoftwareModule osCreated = softwareModuleManagement
                .getByNameAndVersionAndType("name1", "version1", osType.getId()).get();
        final SoftwareModule appCreated = softwareModuleManagement
                .getByNameAndVersionAndType("name3", "version3", appType.getId()).get();

        assertThat(
                JsonPath.compile("[0]_links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                        .as("Response contains invalid self href")
                        .isEqualTo("http://localhost/rest/v1/softwaremodules/" + osCreated.getId());

        assertThat(
                JsonPath.compile("[1]_links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                        .as("Response contains links self href")
                        .isEqualTo("http://localhost/rest/v1/softwaremodules/" + appCreated.getId());

        assertThat(softwareModuleManagement.findAll(PAGE)).as("Wrong softwaremodule size").hasSize(2);
        assertThat(softwareModuleManagement.findByType(PAGE, osType.getId()).getContent().get(0).getName())
                .as("Softwaremoudle name is wrong").isEqualTo(os.getName());
        assertThat(softwareModuleManagement.findByType(PAGE, osType.getId()).getContent().get(0).getCreatedBy())
                .as("Softwaremoudle created by is wrong").isEqualTo("uploadTester");
        assertThat(softwareModuleManagement.findByType(PAGE, osType.getId()).getContent().get(0).getCreatedAt())
                .as("Softwaremoudle created at is wrong").isGreaterThanOrEqualTo(current);
        assertThat(softwareModuleManagement.findByType(PAGE, appType.getId()).getContent().get(0).getName())
                .as("Softwaremoudle name is wrong").isEqualTo(ah.getName());
    }

    @Test
    @Description("Verifies successfull deletion of software modules that are not in use, i.e. assigned to a DS.")
    public void deleteUnassignedSoftwareModule() throws Exception {

        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();

        artifactManagement.create(new ByteArrayInputStream(random), sm.getId(), "file1", false);

        assertThat(softwareModuleManagement.findAll(PAGE)).as("Softwaremoudle size is wrong").hasSize(1);
        assertThat(artifactManagement.count()).isEqualTo(1);

        mvc.perform(delete("/rest/v1/softwaremodules/{smId}", sm.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        assertThat(softwareModuleManagement.findAll(PAGE)).as("After delete no softwarmodule should be available")
                .isEmpty();
        assertThat(artifactManagement.count()).isEqualTo(0);
    }

    @Test
    @Description("Verifies successfull deletion of software modules that are in use, i.e. assigned to a DS which should result in movinf the module to the archive.")
    public void deleteAssignedSoftwareModule() throws Exception {
        final DistributionSet ds1 = testdataFactory.createDistributionSet("a");

        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();

        artifactManagement.create(new ByteArrayInputStream(random), ds1.findFirstModuleByType(appType).get().getId(),
                "file1", false);

        assertThat(softwareModuleManagement.findAll(PAGE)).hasSize(3);
        assertThat(artifactManagement.count()).isEqualTo(1);

        mvc.perform(delete("/rest/v1/softwaremodules/{smId}", ds1.findFirstModuleByType(appType).get().getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        mvc.perform(delete("/rest/v1/softwaremodules/{smId}", ds1.findFirstModuleByType(runtimeType).get().getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        mvc.perform(delete("/rest/v1/softwaremodules/{smId}", ds1.findFirstModuleByType(osType).get().getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        // all 3 are now marked as deleted
        assertThat(softwareModuleManagement.findAll(PAGE).getNumber())
                .as("After delete no softwarmodule should be available").isEqualTo(0);
        assertThat(artifactManagement.count()).isEqualTo(1);
    }

    @Test
    @Description("Tests the deletion of an artifact including verfication that the artifact is actually erased in the repository and removed from the software module.")
    public void deleteArtifact() throws Exception {
        // Create 1 SM
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();

        // Create 2 artifacts
        final Artifact artifact = artifactManagement.create(new ByteArrayInputStream(random), sm.getId(), "file1",
                false);
        artifactManagement.create(new ByteArrayInputStream(random), sm.getId(), "file2", false);

        // check repo before delete
        assertThat(softwareModuleManagement.findAll(PAGE)).hasSize(1);

        assertThat(softwareModuleManagement.get(sm.getId()).get().getArtifacts()).hasSize(2);
        assertThat(artifactManagement.count()).isEqualTo(2);

        // delete
        mvc.perform(delete("/rest/v1/softwaremodules/{smId}/artifacts/{artId}", sm.getId(), artifact.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        // check that only one artifact is still alive and still assigned
        assertThat(softwareModuleManagement.findAll(PAGE)).as("After the sm should be marked as deleted").hasSize(1);
        assertThat(artifactManagement.count()).isEqualTo(1);
        assertThat(softwareModuleManagement.get(sm.getId()).get().getArtifacts())
                .as("After delete artifact should available for marked as deleted sm's").hasSize(1);

    }

    @Test
    @Description("Verfies the successfull creation of metadata.")
    public void createMetadata() throws Exception {

        final String knownKey1 = "knownKey1";
        final String knownValue1 = "knownValue1";
        final String knownKey2 = "knownKey2";
        final String knownValue2 = "knownValue1";

        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        final JSONArray jsonArray = new JSONArray();
        jsonArray.put(new JSONObject().put("key", knownKey1).put("value", knownValue1));
        jsonArray.put(new JSONObject().put("key", knownKey2).put("value", knownValue2).put("targetVisible", true));

        mvc.perform(post("/rest/v1/softwaremodules/{swId}/metadata", sm.getId()).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(jsonArray.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("[0]key", equalTo(knownKey1))).andExpect(jsonPath("[0]value", equalTo(knownValue1)))
                .andExpect(jsonPath("[0]targetVisible", equalTo(false)))
                .andExpect(jsonPath("[1]key", equalTo(knownKey2))).andExpect(jsonPath("[1]value", equalTo(knownValue2)))
                .andExpect(jsonPath("[1]targetVisible", equalTo(true)));

        final SoftwareModuleMetadata metaKey1 = softwareModuleManagement
                .getMetaDataBySoftwareModuleId(sm.getId(), knownKey1).get();
        final SoftwareModuleMetadata metaKey2 = softwareModuleManagement
                .getMetaDataBySoftwareModuleId(sm.getId(), knownKey2).get();

        assertThat(metaKey1.getValue()).as("Metadata key is wrong").isEqualTo(knownValue1);
        assertThat(metaKey2.getValue()).as("Metadata key is wrong").isEqualTo(knownValue2);
    }

    @Test
    @Description("Verfies the successfull update of metadata based on given key.")
    public void updateMetadata() throws Exception {
        // prepare and create metadata for update
        final String knownKey = "knownKey";
        final String knownValue = "knownValue";
        final String updateValue = "valueForUpdate";

        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();
        softwareModuleManagement.createMetaData(
                entityFactory.softwareModuleMetadata().create(sm.getId()).key(knownKey).value(knownValue));

        final JSONObject jsonObject = new JSONObject().put("key", knownKey).put("value", updateValue)
                .put("targetVisible", true);

        mvc.perform(put("/rest/v1/softwaremodules/{swId}/metadata/{key}", sm.getId(), knownKey)
                .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
                .content(jsonObject.toString())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("key", equalTo(knownKey))).andExpect(jsonPath("value", equalTo(updateValue)));

        final SoftwareModuleMetadata assertDS = softwareModuleManagement
                .getMetaDataBySoftwareModuleId(sm.getId(), knownKey).get();
        assertThat(assertDS.getValue()).as("Metadata is wrong").isEqualTo(updateValue);
        assertThat(assertDS.isTargetVisible()).as("target visible is wrong").isTrue();
    }

    @Test
    @Description("Verfies the successfull deletion of metadata entry.")
    public void deleteMetadata() throws Exception {
        // prepare and create metadata for deletion
        final String knownKey = "knownKey";
        final String knownValue = "knownValue";

        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();
        softwareModuleManagement.createMetaData(
                entityFactory.softwareModuleMetadata().create(sm.getId()).key(knownKey).value(knownValue));

        mvc.perform(delete("/rest/v1/softwaremodules/{swId}/metadata/{key}", sm.getId(), knownKey))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        assertThat(softwareModuleManagement.getMetaDataBySoftwareModuleId(sm.getId(), knownKey)).isNotPresent();
    }

    @Test
    @Description("Ensures that module metadta deletion request to API on an entity that does not exist results in NOT_FOUND.")
    public void deleteModuleMetadataThatDoesNotExistLeadsToNotFound() throws Exception {
        // prepare and create metadata for deletion
        final String knownKey = "knownKey";
        final String knownValue = "knownValue";

        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();
        softwareModuleManagement.createMetaData(
                entityFactory.softwareModuleMetadata().create(sm.getId()).key(knownKey).value(knownValue));

        mvc.perform(delete("/rest/v1/softwaremodules/{swId}/metadata/XXX", sm.getId(), knownKey))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        mvc.perform(delete("/rest/v1/softwaremodules/1234/metadata/{key}", knownKey))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        assertThat(softwareModuleManagement.getMetaDataBySoftwareModuleId(sm.getId(), knownKey)).isPresent();
    }

    @Test
    @Description("Ensures that module deletion request to API on an entity that does not exist results in NOT_FOUND.")
    public void deleteSoftwareModuleThatDoesNotExistLeadsToNotFound() throws Exception {
        mvc.perform(delete("/rest/v1/softwaremodules/1234")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Verfies the successfull search of a metadata entry based on value.")
    public void searchSoftwareModuleMetadataRsql() throws Exception {
        final int totalMetadata = 10;
        final String knownKeyPrefix = "knownKey";
        final String knownValuePrefix = "knownValue";
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        for (int index = 0; index < totalMetadata; index++) {
            softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata().create(sm.getId())
                    .key(knownKeyPrefix + index).value(knownValuePrefix + index));
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
}
