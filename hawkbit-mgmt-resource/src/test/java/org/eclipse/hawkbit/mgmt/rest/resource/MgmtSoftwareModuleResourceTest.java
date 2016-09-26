/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.mgmt.json.model.artifact.MgmtArtifact;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.test.util.HashGeneratorUtils;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.eclipse.hawkbit.rest.AbstractRestIntegrationTestWithMongoDB;
import org.eclipse.hawkbit.rest.json.model.ExceptionInfo;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;
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
public class MgmtSoftwareModuleResourceTest extends AbstractRestIntegrationTestWithMongoDB {

    @Before
    public void assertPreparationOfRepo() {
        assertThat(softwareManagement.findSoftwareModulesAll(pageReq)).as("no softwaremodule should be founded")
                .hasSize(0);
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

        softwareManagement
                .createSoftwareModule(entityFactory.generateSoftwareModule(appType, "agent-hub", "1.0.1", null, ""));
        softwareManagement.createSoftwareModule(
                entityFactory.generateSoftwareModule(runtimeType, "oracle-jre", "1.7.2", null, ""));
        softwareManagement
                .createSoftwareModule(entityFactory.generateSoftwareModule(osType, "poky", "3.0.2", null, ""));

        SoftwareModule sm = entityFactory.generateSoftwareModule(osType, knownSWName, knownSWVersion,
                knownSWDescription, knownSWVendor);
        sm = softwareManagement.createSoftwareModule(sm);

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

    }

    @Test
    @Description("Tests the uppload of an artifact binary. The upload is executed and the content checked in the repository for completenes.")
    public void uploadArtifact() throws Exception {
        SoftwareModule sm = entityFactory.generateSoftwareModule(osType, "name 1", "version 1", null, null);
        sm = softwareManagement.createSoftwareModule(sm);

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
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.type", equalTo("local"))).andExpect(jsonPath("$.hashes.md5", equalTo(md5sum)))
                .andExpect(jsonPath("$.hashes.sha1", equalTo(sha1sum)))
                .andExpect(jsonPath("$.size", equalTo(random.length)))
                .andExpect(jsonPath("$.providedFilename", equalTo("origFilename"))).andReturn();

        // check rest of response compared to DB
        final MgmtArtifact artResult = ResourceUtility
                .convertArtifactResponse(mvcResult.getResponse().getContentAsString());
        final Long artId = ((LocalArtifact) softwareManagement.findSoftwareModuleWithDetails(sm.getId()).getArtifacts()
                .get(0)).getId();
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
        assertThat(artifactManagement.countLocalArtifactsAll()).as("Wrong artifact size").isEqualTo(1);

        // binary
        assertTrue("Wrong artifact content",
                IOUtils.contentEquals(new ByteArrayInputStream(random),
                        artifactManagement
                                .loadLocalArtifactBinary((LocalArtifact) softwareManagement
                                        .findSoftwareModuleWithDetails(sm.getId()).getArtifacts().get(0))
                                .getFileInputStream()));

        // hashes
        assertThat(artifactManagement.findLocalArtifactByFilename("origFilename").get(0).getSha1Hash())
                .as("Wrong sha1 hash").isEqualTo(HashGeneratorUtils.generateSHA1(random));

        assertThat(artifactManagement.findLocalArtifactByFilename("origFilename").get(0).getMd5Hash())
                .as("Wrong md5 hash").isEqualTo(HashGeneratorUtils.generateMD5(random));

        // metadata
        assertThat(((LocalArtifact) softwareManagement.findSoftwareModuleWithDetails(sm.getId()).getArtifacts().get(0))
                .getFilename()).as("wrong metadata of the filename").isEqualTo("origFilename");
    }

    @Test
    @Description("Verfies that the system does not accept empty artifact uploads. Expected response: BAD REQUEST")
    public void emptyUploadArtifact() throws Exception {
        assertThat(softwareManagement.findSoftwareModulesAll(pageReq)).hasSize(0);
        assertThat(artifactManagement.countLocalArtifactsAll()).isEqualTo(0);

        SoftwareModule sm = entityFactory.generateSoftwareModule(osType, "name 1", "version 1", null, null);
        sm = softwareManagement.createSoftwareModule(sm);

        final MockMultipartFile file = new MockMultipartFile("file", "orig", null, new byte[0]);

        mvc.perform(fileUpload("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()).file(file)
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Verfies that the system does not accept identical artifacts uploads for the same software module. Expected response: CONFLICT")
    public void duplicateUploadArtifact() throws Exception {
        SoftwareModule sm = entityFactory.generateSoftwareModule(osType, "name 1", "version 1", null, null);
        sm = softwareManagement.createSoftwareModule(sm);

        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();
        final String md5sum = HashGeneratorUtils.generateMD5(random);
        final String sha1sum = HashGeneratorUtils.generateSHA1(random);
        final MockMultipartFile file = new MockMultipartFile("file", "orig", null, random);

        mvc.perform(fileUpload("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()).file(file)
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.type", equalTo("local"))).andExpect(jsonPath("$.hashes.md5", equalTo(md5sum)))
                .andExpect(jsonPath("$.hashes.sha1", equalTo(sha1sum)))
                .andExpect(jsonPath("$.providedFilename", equalTo("orig"))).andExpect(status().isCreated());

        mvc.perform(fileUpload("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()).file(file))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isConflict());
    }

    @Test
    @Description("verfies that option to upload artifacts with a custom defined by metadata, i.e. not the file name of the binary itself.")
    public void uploadArtifactWithCustomName() throws Exception {
        SoftwareModule sm = entityFactory.generateSoftwareModule(osType, "name 1", "version 1", null, null);
        sm = softwareManagement.createSoftwareModule(sm);
        assertThat(artifactManagement.countLocalArtifactsAll()).isEqualTo(0);

        // create test file
        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();
        final MockMultipartFile file = new MockMultipartFile("file", "origFilename", null, random);

        // upload
        mvc.perform(fileUpload("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()).file(file)
                .param("filename", "customFilename").accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.providedFilename", equalTo("customFilename"))).andExpect(status().isCreated());

        // check result in db...
        // repo
        assertThat(artifactManagement.countLocalArtifactsAll()).isEqualTo(1);

        // hashes
        assertThat(artifactManagement.findLocalArtifactByFilename("customFilename")).as("Local artifact is wrong")
                .hasSize(1);
    }

    @Test
    @Description("Verfies that the system refuses upload of an artifact where the provided hash sums do not match. Expected result: BAD REQUEST")
    public void uploadArtifactWithHashCheck() throws Exception {
        SoftwareModule sm = entityFactory.generateSoftwareModule(osType, "name 1", "version 1", null, null);
        sm = softwareManagement.createSoftwareModule(sm);
        assertThat(artifactManagement.countLocalArtifactsAll()).isEqualTo(0);

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
        SoftwareModule sm = entityFactory.generateSoftwareModule(osType, "name 1", "version 1", null, null);
        sm = softwareManagement.createSoftwareModule(sm);

        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();

        final Artifact artifact = artifactManagement.createLocalArtifact(new ByteArrayInputStream(random), sm.getId(),
                "file1", false);
        final Artifact artifact2 = artifactManagement.createLocalArtifact(new ByteArrayInputStream(random), sm.getId(),
                "file2", false);

        final MvcResult result = mvc
                .perform(
                        get("/rest/v1/softwaremodules/{smId}/artifacts/{artId}/download", sm.getId(), artifact.getId()))
                .andExpect(header().string("ETag", artifact.getSha1Hash()))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM)).andReturn();

        assertTrue("Wrong response content", Arrays.equals(result.getResponse().getContentAsByteArray(), random));

        final MvcResult result2 = mvc
                .perform(get("/rest/v1/softwaremodules/{smId}/artifacts/{artId}/download", sm.getId(),
                        artifact2.getId()))
                .andExpect(header().string("ETag", artifact2.getSha1Hash()))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM)).andReturn();

        assertTrue("Response has wrong response content",
                Arrays.equals(result2.getResponse().getContentAsByteArray(), random));

        assertThat(softwareManagement.findSoftwareModulesAll(pageReq)).as("Softwaremodule size is wrong").hasSize(1);
        assertThat(artifactManagement.countLocalArtifactsAll()).isEqualTo(2);
    }

    @Test
    @Description("Verifies the listing of one defined artifact assigned to a given software module. That includes the artifact metadata and download links.")
    public void getArtifact() throws Exception {
        // prepare data for test
        SoftwareModule sm = entityFactory.generateSoftwareModule(osType, "name 1", "version 1", null, null);
        sm = softwareManagement.createSoftwareModule(sm);

        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();
        final Artifact artifact = artifactManagement.createLocalArtifact(new ByteArrayInputStream(random), sm.getId(),
                "file1", false);

        // perform test
        mvc.perform(get("/rest/v1/softwaremodules/{smId}/artifacts/{artId}", sm.getId(), artifact.getId())
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", equalTo(artifact.getId().intValue())))
                .andExpect(jsonPath("$.type", equalTo("local"))).andExpect(jsonPath("$.size", equalTo(random.length)))
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
        SoftwareModule sm = entityFactory.generateSoftwareModule(osType, "name 1", "version 1", null, null);
        sm = softwareManagement.createSoftwareModule(sm);

        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();

        final Artifact artifact = artifactManagement.createLocalArtifact(new ByteArrayInputStream(random), sm.getId(),
                "file1", false);
        final Artifact artifact2 = artifactManagement.createLocalArtifact(new ByteArrayInputStream(random), sm.getId(),
                "file2", false);

        mvc.perform(get("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.[0].id", equalTo(artifact.getId().intValue())))
                .andExpect(jsonPath("$.[0].type", equalTo("local")))
                .andExpect(jsonPath("$.[0].size", equalTo(random.length)))
                .andExpect(jsonPath("$.[0].hashes.md5", equalTo(artifact.getMd5Hash())))
                .andExpect(jsonPath("$.[0].hashes.sha1", equalTo(artifact.getSha1Hash())))
                .andExpect(jsonPath("$.[0].providedFilename", equalTo("file1")))
                .andExpect(jsonPath("$.[0]._links.download.href",
                        equalTo("http://localhost/rest/v1/softwaremodules/" + sm.getId() + "/artifacts/"
                                + artifact.getId() + "/download")))
                .andExpect(jsonPath("$.[0]._links.self.href",
                        equalTo("http://localhost/rest/v1/softwaremodules/" + sm.getId() + "/artifacts/"
                                + artifact.getId())))
                .andExpect(jsonPath("$.[1].id", equalTo(artifact2.getId().intValue())))
                .andExpect(jsonPath("$.[1].type", equalTo("local")))
                .andExpect(jsonPath("$.[1].hashes.md5", equalTo(artifact2.getMd5Hash())))
                .andExpect(jsonPath("$.[1].hashes.sha1", equalTo(artifact2.getSha1Hash())))
                .andExpect(jsonPath("$.[1].providedFilename", equalTo("file2")))
                .andExpect(jsonPath("$.[1]._links.download.href",
                        equalTo("http://localhost/rest/v1/softwaremodules/" + sm.getId() + "/artifacts/"
                                + artifact2.getId() + "/download")))
                .andExpect(jsonPath("$.[1]._links.self.href", equalTo(
                        "http://localhost/rest/v1/softwaremodules/" + sm.getId() + "/artifacts/" + artifact2.getId())));
    }

    @Test
    @Description("Verfies that the system refuses unsupported request types and answers as defined to them, e.g. NOT FOUND on a non existing resource. Or a HTTP POST for updating a resource results in METHOD NOT ALLOWED etc.")
    public void invalidRequestsOnArtifactResource() throws Exception {
        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();
        final MockMultipartFile file = new MockMultipartFile("file", "orig", null, random);

        SoftwareModule sm = entityFactory.generateSoftwareModule(osType, "name 1", "version 1", null, null);
        sm = softwareManagement.createSoftwareModule(sm);

        // no artifact available
        mvc.perform(get("/rest/v1/softwaremodules/{smId}/artifacts/1234567/download", sm.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        mvc.perform(delete("/rest/v1/softwaremodules/{smId}/artifacts/1234567", sm.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // SM does not exist
        artifactManagement.createLocalArtifact(new ByteArrayInputStream(random), sm.getId(), "file1", false);
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
        SoftwareModule sm = entityFactory.generateSoftwareModule(osType, "name 1", "version 1", null, null);
        sm = softwareManagement.createSoftwareModule(sm);

        final List<SoftwareModule> modules = Lists.newArrayList(sm);

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

        final SoftwareModule toLongName = entityFactory.generateSoftwareModule(osType,
                RandomStringUtils.randomAscii(80), "version 1", null, null);
        mvc.perform(
                post("/rest/v1/softwaremodules").content(JsonBuilder.softwareModules(Lists.newArrayList(toLongName)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

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
        SoftwareModule os = entityFactory.generateSoftwareModule(osType, "name1", "version1", "description1",
                "vendor1");
        os = softwareManagement.createSoftwareModule(os);

        SoftwareModule jvm = entityFactory.generateSoftwareModule(runtimeType, "name1", "version1", "description1",
                "vendor1");
        jvm = softwareManagement.createSoftwareModule(jvm);

        SoftwareModule ah = entityFactory.generateSoftwareModule(appType, "name1", "version1", "description1",
                "vendor1");
        ah = softwareManagement.createSoftwareModule(ah);

        mvc.perform(get("/rest/v1/softwaremodules").accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.[?(@.id==" + os.getId() + ")].name", contains("name1")))
                .andExpect(jsonPath("$.content.[?(@.id==" + os.getId() + ")].version", contains("version1")))
                .andExpect(jsonPath("$.content.[?(@.id==" + os.getId() + ")].description", contains("description1")))
                .andExpect(jsonPath("$.content.[?(@.id==" + os.getId() + ")].vendor", contains("vendor1")))
                .andExpect(jsonPath("$.content.[?(@.id==" + os.getId() + ")].type", contains("os")))
                .andExpect(jsonPath("$.content.[?(@.id==" + os.getId() + ")].createdBy", contains("uploadTester")))
                .andExpect(jsonPath("$.content.[?(@.id==" + os.getId() + ")].createdAt", contains(os.getCreatedAt())))
                .andExpect(jsonPath("$.content.[?(@.id==" + os.getId() + ")]._links.type.href",
                        contains("http://localhost/rest/v1/softwaremoduletypes/" + osType.getId())))
                .andExpect(jsonPath("$.content.[?(@.id==" + os.getId() + ")]._links.self.href",
                        contains("http://localhost/rest/v1/softwaremodules/" + os.getId())))
                .andExpect(jsonPath("$.content.[?(@.id==" + os.getId() + ")]._links.artifacts.href",
                        contains("http://localhost/rest/v1/softwaremodules/" + os.getId() + "/artifacts")))
                .andExpect(jsonPath("$.content.[?(@.id==" + os.getId() + ")]._links.metadata.href",
                        contains("http://localhost/rest/v1/softwaremodules/" + os.getId()
                                + "/metadata?offset=0&limit=50")))
                .andExpect(jsonPath("$.content.[?(@.id==" + jvm.getId() + ")].name", contains("name1")))
                .andExpect(jsonPath("$.content.[?(@.id==" + jvm.getId() + ")].version", contains("version1")))
                .andExpect(jsonPath("$.content.[?(@.id==" + jvm.getId() + ")].description", contains("description1")))
                .andExpect(jsonPath("$.content.[?(@.id==" + jvm.getId() + ")].vendor", contains("vendor1")))
                .andExpect(jsonPath("$.content.[?(@.id==" + jvm.getId() + ")].type", contains("runtime")))
                .andExpect(jsonPath("$.content.[?(@.id==" + jvm.getId() + ")].createdBy", contains("uploadTester")))
                .andExpect(jsonPath("$.content.[?(@.id==" + jvm.getId() + ")].createdAt", contains(jvm.getCreatedAt())))
                .andExpect(jsonPath("$.content.[?(@.id==" + jvm.getId() + ")]._links.type.href",
                        contains("http://localhost/rest/v1/softwaremoduletypes/" + runtimeType.getId())))
                .andExpect(jsonPath("$.content.[?(@.id==" + jvm.getId() + ")]._links.self.href",
                        contains("http://localhost/rest/v1/softwaremodules/" + jvm.getId())))
                .andExpect(jsonPath("$.content.[?(@.id==" + jvm.getId() + ")]._links.artifacts.href",
                        contains("http://localhost/rest/v1/softwaremodules/" + jvm.getId() + "/artifacts")))
                .andExpect(jsonPath("$.content.[?(@.id==" + jvm.getId() + ")]._links.metadata.href",
                        contains("http://localhost/rest/v1/softwaremodules/" + jvm.getId()
                                + "/metadata?offset=0&limit=50")))
                .andExpect(jsonPath("$.content.[?(@.id==" + ah.getId() + ")].name", contains("name1")))
                .andExpect(jsonPath("$.content.[?(@.id==" + ah.getId() + ")].version", contains("version1")))
                .andExpect(jsonPath("$.content.[?(@.id==" + ah.getId() + ")].description", contains("description1")))
                .andExpect(jsonPath("$.content.[?(@.id==" + ah.getId() + ")].vendor", contains("vendor1")))
                .andExpect(jsonPath("$.content.[?(@.id==" + ah.getId() + ")].type", contains("application")))
                .andExpect(jsonPath("$.content.[?(@.id==" + ah.getId() + ")].createdBy", contains("uploadTester")))
                .andExpect(jsonPath("$.content.[?(@.id==" + ah.getId() + ")].createdAt", contains(ah.getCreatedAt())))
                .andExpect(jsonPath("$.content.[?(@.id==" + ah.getId() + ")]._links.artifacts.href",
                        contains("http://localhost/rest/v1/softwaremodules/" + ah.getId() + "/artifacts")))
                .andExpect(jsonPath("$.content.[?(@.id==" + ah.getId() + ")]._links.metadata.href",
                        contains("http://localhost/rest/v1/softwaremodules/" + ah.getId()
                                + "/metadata?offset=0&limit=50")))
                .andExpect(jsonPath("$.content.[?(@.id==" + ah.getId() + ")]._links.type.href",
                        contains("http://localhost/rest/v1/softwaremoduletypes/" + appType.getId())))
                .andExpect(jsonPath("$.content.[?(@.id==" + ah.getId() + ")]._links.self.href",
                        contains("http://localhost/rest/v1/softwaremodules/" + ah.getId())));

        assertThat(softwareManagement.findSoftwareModulesAll(pageReq)).as("Softwaremodule size is wrong").hasSize(3);
    }

    @Test
    @Description("Test the various filter parameters, e.g. filter by name or type of the module.")
    public void getSoftwareModulesWithFilterParameters() throws Exception {
        SoftwareModule os1 = entityFactory.generateSoftwareModule(osType, "osName1", "1.0.0", "description1",
                "vendor1");
        os1 = softwareManagement.createSoftwareModule(os1);

        SoftwareModule jvm1 = entityFactory.generateSoftwareModule(runtimeType, "runtimeName1", "2.0.0", "description1",
                "vendor1");
        jvm1 = softwareManagement.createSoftwareModule(jvm1);

        SoftwareModule ah1 = entityFactory.generateSoftwareModule(appType, "appName1", "3.0.0", "description1",
                "vendor1");
        ah1 = softwareManagement.createSoftwareModule(ah1);

        SoftwareModule os2 = entityFactory.generateSoftwareModule(osType, "osName2", "1.0.1", "description2",
                "vendor2");
        os2 = softwareManagement.createSoftwareModule(os2);

        SoftwareModule jvm2 = entityFactory.generateSoftwareModule(runtimeType, "runtimeName2", "2.0.1", "description2",
                "vendor2");
        jvm2 = softwareManagement.createSoftwareModule(jvm2);

        SoftwareModule ah2 = entityFactory.generateSoftwareModule(appType, "appName2", "3.0.1", "description2",
                "vendor2");
        ah2 = softwareManagement.createSoftwareModule(ah2);

        assertThat(softwareManagement.findSoftwareModulesAll(pageReq)).hasSize(6);

        // only by name, only one exists per name
        mvc.perform(get("/rest/v1/softwaremodules?q=name==osName1").accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.[?(@.id==" + os1.getId() + ")].name", contains("osName1")))
                .andExpect(jsonPath("$.content.[?(@.id==" + os1.getId() + ")].version", contains("1.0.0")))
                .andExpect(jsonPath("$.content.[?(@.id==" + os1.getId() + ")].description", contains("description1")))
                .andExpect(jsonPath("$.content.[?(@.id==" + os1.getId() + ")].vendor", contains("vendor1")))
                .andExpect(jsonPath("$.content.[?(@.id==" + os1.getId() + ")].type", contains("os")))
                .andExpect(jsonPath("$.content", hasSize(1))).andExpect(jsonPath("$.total", equalTo(1)));

        // by type, 2 software modules per type exists
        mvc.perform(get("/rest/v1/softwaremodules?q=type==application").accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.[?(@.id==" + ah1.getId() + ")].name", contains("appName1")))
                .andExpect(jsonPath("$.content.[?(@.id==" + ah1.getId() + ")].version", contains("3.0.0")))
                .andExpect(jsonPath("$.content.[?(@.id==" + ah1.getId() + ")].description", contains("description1")))
                .andExpect(jsonPath("$.content.[?(@.id==" + ah1.getId() + ")].vendor", contains("vendor1")))
                .andExpect(jsonPath("$.content.[?(@.id==" + ah1.getId() + ")].type", contains("application")))
                .andExpect(jsonPath("$.content.[?(@.id==" + ah2.getId() + ")].name", contains("appName2")))
                .andExpect(jsonPath("$.content.[?(@.id==" + ah2.getId() + ")].version", contains("3.0.1")))
                .andExpect(jsonPath("$.content.[?(@.id==" + ah2.getId() + ")].description", contains("description2")))
                .andExpect(jsonPath("$.content.[?(@.id==" + ah2.getId() + ")].vendor", contains("vendor2")))
                .andExpect(jsonPath("$.content.[?(@.id==" + ah2.getId() + ")].type", contains("application")))
                .andExpect(jsonPath("$.content", hasSize(2))).andExpect(jsonPath("$.total", equalTo(2)));

        // by type and version=2.0.0 -> only one result
        mvc.perform(get("/rest/v1/softwaremodules?q=type==runtime;version==2.0.0").accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.[?(@.id==" + jvm1.getId() + ")].name", contains("runtimeName1")))
                .andExpect(jsonPath("$.content.[?(@.id==" + jvm1.getId() + ")].version", contains("2.0.0")))
                .andExpect(jsonPath("$.content.[?(@.id==" + jvm1.getId() + ")].description", contains("description1")))
                .andExpect(jsonPath("$.content.[?(@.id==" + jvm1.getId() + ")].vendor", contains("vendor1")))
                .andExpect(jsonPath("$.content", hasSize(1))).andExpect(jsonPath("$.total", equalTo(1)));

        // by type and version range >=2.0.0 -> 2 result
        mvc.perform(get("/rest/v1/softwaremodules?q=type==runtime;version=ge=2.0.0").accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.[?(@.id==" + jvm1.getId() + ")].name", contains("runtimeName1")))
                .andExpect(jsonPath("$.content.[?(@.id==" + jvm1.getId() + ")].version", contains("2.0.0")))
                .andExpect(jsonPath("$.content.[?(@.id==" + jvm1.getId() + ")].description", contains("description1")))
                .andExpect(jsonPath("$.content.[?(@.id==" + jvm1.getId() + ")].vendor", contains("vendor1")))
                .andExpect(jsonPath("$.content.[?(@.id==" + jvm2.getId() + ")].name", contains("runtimeName2")))
                .andExpect(jsonPath("$.content.[?(@.id==" + jvm2.getId() + ")].version", contains("2.0.1")))
                .andExpect(jsonPath("$.content.[?(@.id==" + jvm2.getId() + ")].description", contains("description2")))
                .andExpect(jsonPath("$.content.[?(@.id==" + jvm2.getId() + ")].vendor", contains("vendor2")))
                .andExpect(jsonPath("$.content", hasSize(2))).andExpect(jsonPath("$.total", equalTo(2)));
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
    public void getSoftareModule() throws Exception {
        SoftwareModule os = entityFactory.generateSoftwareModule(osType, "name1", "version1", "description1",
                "vendor1");
        os = softwareManagement.createSoftwareModule(os);

        mvc.perform(get("/rest/v1/softwaremodules/{smId}", os.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", equalTo("name1"))).andExpect(jsonPath("$.version", equalTo("version1")))
                .andExpect(jsonPath("$.description", equalTo("description1")))
                .andExpect(jsonPath("$.vendor", equalTo("vendor1"))).andExpect(jsonPath("$.type", equalTo("os")))
                .andExpect(jsonPath("$.createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("$.createdAt", equalTo(os.getCreatedAt())))
                .andExpect(jsonPath("$._links.metadata.href",
                        equalTo("http://localhost/rest/v1/softwaremodules/" + os.getId()
                                + "/metadata?offset=0&limit=50")))
                .andExpect(jsonPath("$._links.type.href",
                        equalTo("http://localhost/rest/v1/softwaremoduletypes/" + osType.getId())))
                .andExpect(jsonPath("$._links.artifacts.href",
                        equalTo("http://localhost/rest/v1/softwaremodules/" + os.getId() + "/artifacts")));

        SoftwareModule jvm = entityFactory.generateSoftwareModule(runtimeType, "name1", "version1", "description1",
                "vendor1");
        jvm = softwareManagement.createSoftwareModule(jvm);

        mvc.perform(get("/rest/v1/softwaremodules/{smId}", jvm.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", equalTo("name1"))).andExpect(jsonPath("$.version", equalTo("version1")))
                .andExpect(jsonPath("$.description", equalTo("description1")))
                .andExpect(jsonPath("$.vendor", equalTo("vendor1"))).andExpect(jsonPath("$.type", equalTo("runtime")))
                .andExpect(jsonPath("$.createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("$.createdAt", equalTo(jvm.getCreatedAt())))
                .andExpect(jsonPath("$._links.metadata.href",
                        equalTo("http://localhost/rest/v1/softwaremodules/" + jvm.getId()
                                + "/metadata?offset=0&limit=50")))
                .andExpect(jsonPath("$._links.type.href",
                        equalTo("http://localhost/rest/v1/softwaremoduletypes/" + runtimeType.getId())))
                .andExpect(jsonPath("$._links.artifacts.href",
                        equalTo("http://localhost/rest/v1/softwaremodules/" + jvm.getId() + "/artifacts")));

        SoftwareModule ah = entityFactory.generateSoftwareModule(appType, "name1", "version1", "description1",
                "vendor1");
        ah = softwareManagement.createSoftwareModule(ah);

        mvc.perform(get("/rest/v1/softwaremodules/{smId}", ah.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", equalTo("name1"))).andExpect(jsonPath("$.version", equalTo("version1")))
                .andExpect(jsonPath("$.description", equalTo("description1")))
                .andExpect(jsonPath("$.vendor", equalTo("vendor1")))
                .andExpect(jsonPath("$.type", equalTo("application")))
                .andExpect(jsonPath("$.createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("$.createdAt", equalTo(ah.getCreatedAt())))
                .andExpect(jsonPath("$._links.metadata.href",
                        equalTo("http://localhost/rest/v1/softwaremodules/" + ah.getId()
                                + "/metadata?offset=0&limit=50")))
                .andExpect(jsonPath("$._links.type.href",
                        equalTo("http://localhost/rest/v1/softwaremoduletypes/" + appType.getId())))
                .andExpect(jsonPath("$._links.artifacts.href",
                        equalTo("http://localhost/rest/v1/softwaremodules/" + ah.getId() + "/artifacts")));

        assertThat(softwareManagement.findSoftwareModulesAll(pageReq)).as("Softwaremodule size is wrong").hasSize(3);
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Verfies that the create request actually results in the creation of the modules in the repository.")
    public void createSoftwareModules() throws JSONException, Exception {
        final SoftwareModule os = entityFactory.generateSoftwareModule(osType, "name1", "version1", "description1",
                "vendor1");
        final SoftwareModule jvm = entityFactory.generateSoftwareModule(runtimeType, "name2", "version1",
                "description1", "vendor1");
        final SoftwareModule ah = entityFactory.generateSoftwareModule(appType, "name3", "version1", "description1",
                "vendor1");

        final List<SoftwareModule> modules = new ArrayList<>();
        modules.add(os);
        modules.add(jvm);
        modules.add(ah);

        final long current = System.currentTimeMillis();

        final MvcResult mvcResult = mvc
                .perform(post("/rest/v1/softwaremodules/").content(JsonBuilder.softwareModules(modules))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("[0].name", equalTo("name1")))
                .andExpect(jsonPath("[0].version", equalTo("version1")))
                .andExpect(jsonPath("[0].description", equalTo("description1")))
                .andExpect(jsonPath("[0].vendor", equalTo("vendor1"))).andExpect(jsonPath("[0].type", equalTo("os")))
                .andExpect(jsonPath("[0].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[1].name", equalTo("name2")))
                .andExpect(jsonPath("[1].version", equalTo("version1")))
                .andExpect(jsonPath("[1].description", equalTo("description1")))
                .andExpect(jsonPath("[1].vendor", equalTo("vendor1")))
                .andExpect(jsonPath("[1].type", equalTo("runtime")))
                .andExpect(jsonPath("[1].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[2].name", equalTo("name3")))
                .andExpect(jsonPath("[2].version", equalTo("version1")))
                .andExpect(jsonPath("[2].description", equalTo("description1")))
                .andExpect(jsonPath("[2].vendor", equalTo("vendor1")))
                .andExpect(jsonPath("[2].type", equalTo("application")))
                .andExpect(jsonPath("[2].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[2].createdAt", not(equalTo(0)))).andReturn();

        final SoftwareModule osCreated = softwareManagement.findSoftwareModuleByNameAndVersion("name1", "version1",
                osType);
        final SoftwareModule jvmCreated = softwareManagement.findSoftwareModuleByNameAndVersion("name2", "version1",
                runtimeType);
        final SoftwareModule ahCreated = softwareManagement.findSoftwareModuleByNameAndVersion("name3", "version1",
                appType);

        assertThat(
                JsonPath.compile("[0]_links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                        .as("Response contains invalid self href")
                        .isEqualTo("http://localhost/rest/v1/softwaremodules/" + osCreated.getId());
        assertThat(JsonPath.compile("[0]_links.artifacts.href").read(mvcResult.getResponse().getContentAsString())
                .toString()).as("Response contains invalid artifacts href")
                        .isEqualTo("http://localhost/rest/v1/softwaremodules/" + osCreated.getId() + "/artifacts");

        assertThat(
                JsonPath.compile("[1]_links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                        .as("Response contains invalid self href")
                        .isEqualTo("http://localhost/rest/v1/softwaremodules/" + jvmCreated.getId());
        assertThat(JsonPath.compile("[1]_links.artifacts.href").read(mvcResult.getResponse().getContentAsString())
                .toString()).as("Response contains invalid artfacts href")
                        .isEqualTo("http://localhost/rest/v1/softwaremodules/" + jvmCreated.getId() + "/artifacts");

        assertThat(
                JsonPath.compile("[2]_links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                        .as("Response contains links self href")
                        .isEqualTo("http://localhost/rest/v1/softwaremodules/" + ahCreated.getId());
        assertThat(JsonPath.compile("[2]_links.artifacts.href").read(mvcResult.getResponse().getContentAsString())
                .toString()).as("Response contains invalid artifacts href")
                        .isEqualTo("http://localhost/rest/v1/softwaremodules/" + ahCreated.getId() + "/artifacts");

        assertThat(softwareManagement.findSoftwareModulesAll(pageReq)).as("Wrong softwaremodule size").hasSize(3);
        assertThat(softwareManagement.findSoftwareModulesByType(pageReq, osType).getContent().get(0).getName())
                .as("Softwaremoudle name is wrong").isEqualTo(os.getName());
        assertThat(softwareManagement.findSoftwareModulesByType(pageReq, osType).getContent().get(0).getCreatedBy())
                .as("Softwaremoudle created by is wrong").isEqualTo("uploadTester");
        assertThat(softwareManagement.findSoftwareModulesByType(pageReq, osType).getContent().get(0).getCreatedAt())
                .as("Softwaremoudle created at is wrong").isGreaterThanOrEqualTo(current);
        assertThat(softwareManagement.findSoftwareModulesByType(pageReq, runtimeType).getContent().get(0).getName())
                .as("Softwaremoudle name is wrong").isEqualTo(jvm.getName());
        assertThat(softwareManagement.findSoftwareModulesByType(pageReq, appType).getContent().get(0).getName())
                .as("Softwaremoudle name is wrong").isEqualTo(ah.getName());
    }

    @Test
    @Description("Verifies successfull deletion of software modules that are not in use, i.e. assigned to a DS.")
    public void deleteUnassignedSoftwareModule() throws Exception {

        SoftwareModule sm = entityFactory.generateSoftwareModule(osType, "name 1", "version 1", null, null);
        sm = softwareManagement.createSoftwareModule(sm);

        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();

        artifactManagement.createLocalArtifact(new ByteArrayInputStream(random), sm.getId(), "file1", false);

        assertThat(softwareManagement.findSoftwareModulesAll(pageReq)).as("Softwaremoudle size is wrong").hasSize(1);
        assertThat(artifactManagement.countLocalArtifactsAll()).isEqualTo(1);

        mvc.perform(delete("/rest/v1/softwaremodules/{smId}", sm.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        assertThat(softwareManagement.findSoftwareModulesAll(pageReq))
                .as("After delete no softwarmodule should be available").isEmpty();
        assertThat(artifactManagement.countLocalArtifactsAll()).isEqualTo(0);
    }

    @Test
    @Description("Verifies successfull deletion of software modules that are in use, i.e. assigned to a DS which should result in movinf the module to the archive.")
    public void deleteAssignedSoftwareModule() throws Exception {
        final DistributionSet ds1 = testdataFactory.createDistributionSet("a");

        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();

        artifactManagement.createLocalArtifact(new ByteArrayInputStream(random),
                ds1.findFirstModuleByType(appType).getId(), "file1", false);

        assertThat(softwareManagement.findSoftwareModulesAll(pageReq)).hasSize(3);
        assertThat(artifactManagement.countLocalArtifactsAll()).isEqualTo(1);

        mvc.perform(delete("/rest/v1/softwaremodules/{smId}", ds1.findFirstModuleByType(appType).getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        mvc.perform(delete("/rest/v1/softwaremodules/{smId}", ds1.findFirstModuleByType(runtimeType).getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        mvc.perform(delete("/rest/v1/softwaremodules/{smId}", ds1.findFirstModuleByType(osType).getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        // all 3 are now marked as deleted
        assertThat(softwareManagement.findSoftwareModulesAll(pageReq).getNumber())
                .as("After delete no softwarmodule should be available").isEqualTo(0);
        assertThat(artifactManagement.countLocalArtifactsAll()).isEqualTo(1);
    }

    @Test
    @Description("Tests the deletion of an artifact including verfication that the artifact is actually erased in the repository and removed from the software module.")
    public void deleteArtifact() throws Exception {
        // Create 1 SM
        SoftwareModule sm = entityFactory.generateSoftwareModule(osType, "name 1", "version 1", null, null);
        sm = softwareManagement.createSoftwareModule(sm);

        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();

        // Create 2 artifacts
        final LocalArtifact artifact = artifactManagement.createLocalArtifact(new ByteArrayInputStream(random),
                sm.getId(), "file1", false);
        artifactManagement.createLocalArtifact(new ByteArrayInputStream(random), sm.getId(), "file2", false);

        // check repo before delete
        assertThat(softwareManagement.findSoftwareModulesAll(pageReq)).hasSize(1);

        assertThat(softwareManagement.findSoftwareModuleWithDetails(sm.getId()).getArtifacts()).hasSize(2);
        assertThat(artifactManagement.countLocalArtifactsAll()).isEqualTo(2);

        // delete
        mvc.perform(delete("/rest/v1/softwaremodules/{smId}/artifacts/{artId}", sm.getId(), artifact.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        // check that only one artifact is still alive and still assigned
        assertThat(softwareManagement.findSoftwareModulesAll(pageReq)).as("After the sm should be marked as deleted")
                .hasSize(1);
        assertThat(artifactManagement.countLocalArtifactsAll()).isEqualTo(1);
        assertThat(softwareManagement.findSoftwareModuleWithDetails(sm.getId()).getArtifacts())
                .as("After delete artifact should available for marked as deleted sm's").hasSize(1);

    }

    @Test
    @Description("Verfies the successfull creation of metadata.")
    public void createMetadata() throws Exception {

        final String knownKey1 = "knownKey1";
        final String knownValue1 = "knownValue1";
        final String knownKey2 = "knownKey1";
        final String knownValue2 = "knownValue1";

        final SoftwareModule sm = softwareManagement
                .createSoftwareModule(entityFactory.generateSoftwareModule(osType, "name 1", "version 1", null, null));

        final JSONArray jsonArray = new JSONArray();
        jsonArray.put(new JSONObject().put("key", knownKey1).put("value", knownValue1));
        jsonArray.put(new JSONObject().put("key", knownKey2).put("value", knownValue2));

        mvc.perform(post("/rest/v1/softwaremodules/{swId}/metadata", sm.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(jsonArray.toString())).andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("[0]key", equalTo(knownKey1))).andExpect(jsonPath("[0]value", equalTo(knownValue1)))
                .andExpect(jsonPath("[1]key", equalTo(knownKey2)))
                .andExpect(jsonPath("[1]value", equalTo(knownValue2)));

        final SoftwareModuleMetadata metaKey1 = softwareManagement.findSoftwareModuleMetadata(sm, knownKey1);
        final SoftwareModuleMetadata metaKey2 = softwareManagement.findSoftwareModuleMetadata(sm, knownKey2);

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

        final SoftwareModule sm = softwareManagement
                .createSoftwareModule(entityFactory.generateSoftwareModule(osType, "name 1", "version 1", null, null));
        softwareManagement
                .createSoftwareModuleMetadata(entityFactory.generateSoftwareModuleMetadata(sm, knownKey, knownValue));

        final JSONObject jsonObject = new JSONObject().put("key", knownKey).put("value", updateValue);

        mvc.perform(put("/rest/v1/softwaremodules/{swId}/metadata/{key}", sm.getId(), knownKey)
                .contentType(MediaType.APPLICATION_JSON).content(jsonObject.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("key", equalTo(knownKey))).andExpect(jsonPath("value", equalTo(updateValue)));

        final SoftwareModuleMetadata assertDS = softwareManagement.findSoftwareModuleMetadata(sm, knownKey);
        assertThat(assertDS.getValue()).as("Metadata is wrong").isEqualTo(updateValue);
    }

    @Test
    @Description("Verfies the successfull deletion of metadata entry.")
    public void deleteMetadata() throws Exception {
        // prepare and create metadata for deletion
        final String knownKey = "knownKey";
        final String knownValue = "knownValue";

        final SoftwareModule sm = softwareManagement
                .createSoftwareModule(entityFactory.generateSoftwareModule(osType, "name 1", "version 1", null, null));
        softwareManagement
                .createSoftwareModuleMetadata(entityFactory.generateSoftwareModuleMetadata(sm, knownKey, knownValue));

        mvc.perform(delete("/rest/v1/softwaremodules/{swId}/metadata/{key}", sm.getId(), knownKey))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        try {
            softwareManagement.findSoftwareModuleMetadata(sm, knownKey);
            fail("expected EntityNotFoundException but didn't throw");
        } catch (final EntityNotFoundException e) {
            // ok as expected
        }
    }

    @Test
    @Description("Verfies the successfull search of a metadata entry based on value.")
    public void searchSoftwareModuleMetadataRsql() throws Exception {
        final int totalMetadata = 10;
        final String knownKeyPrefix = "knownKey";
        final String knownValuePrefix = "knownValue";
        final SoftwareModule sm = softwareManagement
                .createSoftwareModule(entityFactory.generateSoftwareModule(osType, "name 1", "version 1", null, null));

        for (int index = 0; index < totalMetadata; index++) {
            softwareManagement.createSoftwareModuleMetadata(
                    entityFactory.generateSoftwareModuleMetadata(softwareManagement.findSoftwareModuleById(sm.getId()),
                            knownKeyPrefix + index, knownValuePrefix + index));
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
            final SoftwareModule softwareModule = entityFactory.generateSoftwareModule(osType, str, str, str, str);

            softwareManagement.createSoftwareModule(softwareModule);
            character++;
        }
    }
}
