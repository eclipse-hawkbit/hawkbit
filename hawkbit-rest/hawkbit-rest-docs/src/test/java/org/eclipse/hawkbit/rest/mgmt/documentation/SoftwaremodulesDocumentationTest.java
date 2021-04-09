/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.mgmt.documentation;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.fileUpload;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.Constants;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.ArtifactUpload;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.rest.documentation.AbstractApiRestDocumentation;
import org.eclipse.hawkbit.rest.documentation.ApiModelPropertiesGeneric;
import org.eclipse.hawkbit.rest.documentation.MgmtApiModelProperties;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.payload.JsonFieldType;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Documentation generation for Management API for {@link SoftwareModule}.
 */
@Feature("Spring Rest Docs Tests - SoftwareModule")
@Story("Softwaremodule Resource")
public class SoftwaremodulesDocumentationTest extends AbstractApiRestDocumentation {

    @Override
    public String getResourceName() {
        return "softwaremodules";
    }

    @Test
    @Description("Handles the GET request of retrieving all softwaremodules within SP. Required Permission: "
            + SpPermission.READ_REPOSITORY)
    public void getSoftwareModules() throws Exception {

        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();
        softwareModuleManagement.update(entityFactory.softwareModule().update(sm.getId()).description("a description"));

        final SoftwareModule sm2 = testdataFactory.createSoftwareModuleApp();
        softwareModuleManagement
                .update(entityFactory.softwareModule().update(sm2.getId()).description("a description"));

        mockMvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andDo(this.document.document(responseFields(
                        fieldWithPath("total").description(ApiModelPropertiesGeneric.TOTAL_ELEMENTS),
                        fieldWithPath("size").type(JsonFieldType.NUMBER).description(ApiModelPropertiesGeneric.SIZE),
                        fieldWithPath("content").description(MgmtApiModelProperties.SM_LIST),
                        fieldWithPath("content[].id").description(ApiModelPropertiesGeneric.ITEM_ID),
                        fieldWithPath("content[].name").description(ApiModelPropertiesGeneric.NAME),
                        fieldWithPath("content[].description").description(ApiModelPropertiesGeneric.DESCRPTION),
                        fieldWithPath("content[].vendor").description(MgmtApiModelProperties.VENDOR),
                        fieldWithPath("content[].createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                        fieldWithPath("content[].createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                        fieldWithPath("content[].lastModifiedBy")
                                .description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY),
                        fieldWithPath("content[].lastModifiedAt")
                                .description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT),
                        fieldWithPath("content[].type").description(MgmtApiModelProperties.SM_TYPE),
                        fieldWithPath("content[].version").description(MgmtApiModelProperties.VERSION),
                        fieldWithPath("content[]._links.self").ignored())));
    }

    @Test
    @Description("Handles the GET request of retrieving all softwaremodules within SP with given page size and offset including sorting by version descending and filter down to all sets which name starts with 'SM'. Required Permission: "
            + SpPermission.READ_REPOSITORY)
    public void getSoftwareModulesWithParameters() throws Exception {

        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();
        softwareModuleManagement.update(entityFactory.softwareModule().update(sm.getId()).description("a description"));

        final SoftwareModule sm2 = testdataFactory.createSoftwareModuleApp();
        softwareModuleManagement
                .update(entityFactory.softwareModule().update(sm2.getId()).description("a description"));

        mockMvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING).param("offset", "1")
                .param("limit", "2").param("sort", "version:DESC").param("q", "name==SM*"))

                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andDo(this.document.document(getFilterRequestParamter()));
    }

    @Test
    @Description("Handles the POST request of creating new software modules within SP. The request body must always be a list of modules. Required Permission: "
            + SpPermission.CREATE_REPOSITORY)
    public void postSoftwareModules() throws Exception {
        final SoftwareModule os = entityFactory.softwareModule().create().type(osType).name("name1").version("version1")
                .description("description1").vendor("vendor1").build();
        final SoftwareModule ah = entityFactory.softwareModule().create().type(appType).name("name3")
                .version("version1").description("description1").vendor("vendor1").build();

        final List<SoftwareModule> modules = Arrays.asList(os, ah);

        mockMvc.perform(post(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING)
                .content(JsonBuilder.softwareModulesCreatableFieldsOnly(modules)).contentType(MediaTypes.HAL_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andDo(this.document.document(
                        requestFields(requestFieldWithPath("[]name").description(ApiModelPropertiesGeneric.NAME),
                                optionalRequestFieldWithPath("[]description")
                                        .description(ApiModelPropertiesGeneric.DESCRPTION),
                                requestFieldWithPath("[]version").description(MgmtApiModelProperties.VERSION),
                                optionalRequestFieldWithPath("[]vendor").description(MgmtApiModelProperties.VENDOR),
                                requestFieldWithPath("[]type").description(MgmtApiModelProperties.SMT_TYPE)),
                        responseFields(fieldWithPath("[].id").description(ApiModelPropertiesGeneric.ITEM_ID),
                                fieldWithPath("[].name").description(ApiModelPropertiesGeneric.NAME),
                                fieldWithPath("[].description").description(ApiModelPropertiesGeneric.DESCRPTION),
                                fieldWithPath("[].vendor").description(MgmtApiModelProperties.VENDOR),
                                fieldWithPath("[].deleted").description(ApiModelPropertiesGeneric.DELETED),
                                fieldWithPath("[].createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                                fieldWithPath("[].createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                                fieldWithPath("[].lastModifiedBy")
                                        .description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY),
                                fieldWithPath("[].lastModifiedAt")
                                        .description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT),
                                fieldWithPath("[].type").description(MgmtApiModelProperties.SM_TYPE),
                                fieldWithPath("[].version").description(MgmtApiModelProperties.VERSION),
                                fieldWithPath("[]._links.self").ignored())));
    }

    @Test
    @Description("Handles the DELETE request for a single softwaremodule within SP. Required Permission: "
            + SpPermission.DELETE_REPOSITORY)
    public void deleteSoftwareModule() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        this.mockMvc
                .perform(
                        delete(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING + "/{softwareModuleId}", sm.getId()))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(pathParameters(
                        parameterWithName("softwareModuleId").description(ApiModelPropertiesGeneric.ITEM_ID))));
    }

    @Test
    @Description("Handles the GET request of retrieving a single softwaremodule within SP. Required Permission: "
            + SpPermission.READ_REPOSITORY)
    public void getSoftwareModule() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();
        softwareModuleManagement.update(entityFactory.softwareModule().update(sm.getId()).description("a description"));

        mockMvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING + "/{softwareModuleId}", sm.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andDo(this.document.document(
                        pathParameters(
                                parameterWithName("softwareModuleId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        responseFields(fieldWithPath("id").description(ApiModelPropertiesGeneric.ITEM_ID),
                                fieldWithPath("name").description(ApiModelPropertiesGeneric.NAME),
                                fieldWithPath("description").description(ApiModelPropertiesGeneric.DESCRPTION),
                                fieldWithPath("createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                                fieldWithPath("createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                                fieldWithPath("lastModifiedBy").description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY),
                                fieldWithPath("lastModifiedAt").description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT),
                                fieldWithPath("vendor").description(MgmtApiModelProperties.VENDOR),
                                fieldWithPath("deleted").description(ApiModelPropertiesGeneric.DELETED),
                                fieldWithPath("type").description(MgmtApiModelProperties.SM_TYPE),
                                fieldWithPath("version").description(MgmtApiModelProperties.VERSION),
                                fieldWithPath("_links.self").ignored(),
                                fieldWithPath("_links.type").description(MgmtApiModelProperties.SM_TYPE),
                                fieldWithPath("_links.artifacts").description(MgmtApiModelProperties.ARTIFACT_LIST),
                                fieldWithPath("_links.metadata").description(MgmtApiModelProperties.META_DATA))));
    }

    @Test
    @Description("Handles the PUT request for a single softwaremodule within SP. Required Permission: "
            + SpPermission.UPDATE_REPOSITORY)
    public void putSoftwareModule() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();
        softwareModuleManagement.update(entityFactory.softwareModule().update(sm.getId()).description("a description"));

        final SoftwareModule update = entityFactory.softwareModule().create().name("another name")
                .vendor("another Vendor").description("a new description").type(Constants.SMT_DEFAULT_OS_KEY).build();

        mockMvc.perform(put(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING + "/{softwareModuleId}", sm.getId())
                .content(JsonBuilder.softwareModuleUpdatableFieldsOnly(update)).contentType(MediaTypes.HAL_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andDo(this.document.document(
                        pathParameters(
                                parameterWithName("softwareModuleId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        requestFields(optionalRequestFieldWithPath("vendor").description(MgmtApiModelProperties.VENDOR),
                                optionalRequestFieldWithPath("description")
                                        .description(ApiModelPropertiesGeneric.DESCRPTION)),
                        responseFields(fieldWithPath("id").description(ApiModelPropertiesGeneric.ITEM_ID),
                                fieldWithPath("name").description(ApiModelPropertiesGeneric.NAME),
                                fieldWithPath("description").description(ApiModelPropertiesGeneric.DESCRPTION),
                                fieldWithPath("createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                                fieldWithPath("createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                                fieldWithPath("lastModifiedBy").description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY),
                                fieldWithPath("lastModifiedAt").description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT),
                                fieldWithPath("type").description(MgmtApiModelProperties.SM_TYPE),
                                fieldWithPath("version").description(MgmtApiModelProperties.VERSION),
                                fieldWithPath("vendor").description(MgmtApiModelProperties.VENDOR),
                                fieldWithPath("deleted").description(ApiModelPropertiesGeneric.DELETED),
                                fieldWithPath("_links.self").ignored(),
                                fieldWithPath("_links.type").description(MgmtApiModelProperties.SM_TYPE),
                                fieldWithPath("_links.artifacts").description(MgmtApiModelProperties.ARTIFACT_LIST),
                                fieldWithPath("_links.metadata").description(MgmtApiModelProperties.META_DATA))));

    }

    @Test
    @Description("Handles the GET request of retrieving all meta data of artifacts assigned to a software module. Required Permission: "
            + SpPermission.READ_REPOSITORY)
    public void getArtifacts() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        final byte random[] = RandomStringUtils.random(5).getBytes();

        artifactManagement.create(new ArtifactUpload(new ByteArrayInputStream(random), sm.getId(), "file1", false, 0));

        mockMvc.perform(
                get(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING + "/{softwareModuleId}/artifacts", sm.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andDo(this.document.document(
                        pathParameters(
                                parameterWithName("softwareModuleId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        responseFields(fieldWithPath("[].id").description(ApiModelPropertiesGeneric.ITEM_ID),
                                fieldWithPath("[].size").description(MgmtApiModelProperties.ARTIFACT_SIZE),
                                fieldWithPath("[].createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                                fieldWithPath("[].createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                                fieldWithPath("[].lastModifiedBy")
                                        .description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY),
                                fieldWithPath("[].lastModifiedAt")
                                        .description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT),
                                fieldWithPath("[].hashes.md5").description(MgmtApiModelProperties.ARTIFACT_HASHES_MD5),
                                fieldWithPath("[].hashes.sha1")
                                        .description(MgmtApiModelProperties.ARTIFACT_HASHES_SHA1),
                                fieldWithPath("[].hashes.sha256")
                                        .description(MgmtApiModelProperties.ARTIFACT_HASHES_SHA256),
                                fieldWithPath("[].providedFilename")
                                        .description(MgmtApiModelProperties.ARTIFACT_PROVIDED_FILENAME),
                                fieldWithPath("[]._links.self").ignored())));
    }

    @Test
    @Description("Handles POST request for artifact upload. Required Permission: " + SpPermission.CREATE_REPOSITORY)
    public void postArtifact() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        // create test file
        final byte random[] = RandomStringUtils.random(5).getBytes();
        final MockMultipartFile file = new MockMultipartFile("file", "origFilename", null, random);

        mockMvc.perform(fileUpload(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING + "/{softwareModuleId}/artifacts", sm.getId()).file(file))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andDo(this.document.document(
                        pathParameters(
                                parameterWithName("softwareModuleId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        responseFields(fieldWithPath("size").description(MgmtApiModelProperties.ARTIFACT_SIZE),
                                fieldWithPath("id").description(ApiModelPropertiesGeneric.ITEM_ID),
                                fieldWithPath("createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                                fieldWithPath("createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                                fieldWithPath("lastModifiedBy").description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY),
                                fieldWithPath("lastModifiedAt").description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT),
                                fieldWithPath("_links.self").ignored(),
                                fieldWithPath("_links.download")
                                        .description(MgmtApiModelProperties.ARTIFACT_DOWNLOAD_LINK),
                                fieldWithPath("hashes.md5").description(MgmtApiModelProperties.ARTIFACT_HASHES_MD5),
                                fieldWithPath("hashes.sha1").description(MgmtApiModelProperties.ARTIFACT_HASHES_SHA1),
                                fieldWithPath("hashes.sha256")
                                        .description(MgmtApiModelProperties.ARTIFACT_HASHES_SHA256),
                                fieldWithPath("providedFilename")
                                        .description(MgmtApiModelProperties.ARTIFACT_PROVIDED_FILENAME)))

                );
    }

    @Test
    @Description("Handles POST request for artifact upload. Required Permission: " + SpPermission.CREATE_REPOSITORY)
    public void postArtifactWithParameters() throws Exception {

        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        final byte random[] = RandomStringUtils.random(5).getBytes();
        final MockMultipartFile file = new MockMultipartFile("file", "origFilename", null, random);

        mockMvc.perform(fileUpload(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING + "/{softwareModuleId}/artifacts",
                        sm.getId()).file(file).param("filename", "filename").param("file", "s")
                                .param("md5sum", "md5sum").param("sha1sum", "sha1sum"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(this.document.document(requestParameters(
                        parameterWithName("filename").description(MgmtApiModelProperties.ARTIFACT_PROVIDED_FILENAME),
                        parameterWithName("file").description(MgmtApiModelProperties.ARTIFACT_PROVIDED_FILE),
                        parameterWithName("md5sum").description(MgmtApiModelProperties.ARTIFACT_HASHES_MD5),
                        parameterWithName("sha1sum").description(MgmtApiModelProperties.ARTIFACT_HASHES_SHA1))));
    }

    @Test
    @Description("Handles the DELETE request for a single SoftwareModule within SP. Required Permission: "
            + SpPermission.DELETE_REPOSITORY)
    public void deleteArtifact() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        final byte random[] = RandomStringUtils.random(5).getBytes();

        final Artifact artifact = artifactManagement
                .create(new ArtifactUpload(new ByteArrayInputStream(random), sm.getId(), "file1", false, 0));

        mockMvc.perform(delete(
                MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING + "/{softwareModuleId}/artifacts/{artifactId}",
                sm.getId(), artifact.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(pathParameters(
                        parameterWithName("softwareModuleId").description(ApiModelPropertiesGeneric.ITEM_ID),
                        parameterWithName("artifactId").description(ApiModelPropertiesGeneric.ITEM_ID))));
    }

    @Test
    @Description("Handles the GET request of retrieving a single Artifact meta data request. Required Permission: "
            + SpPermission.READ_REPOSITORY)
    public void getArtifact() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        final byte random[] = RandomStringUtils.random(5).getBytes();

        final Artifact artifact = artifactManagement
                .create(new ArtifactUpload(new ByteArrayInputStream(random), sm.getId(), "file1", false, 0));

        mockMvc.perform(
                get(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING + "/{softwareModuleId}/artifacts/{artifactId}",
                        sm.getId(), artifact.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andDo(this.document.document(
                        pathParameters(
                                parameterWithName("softwareModuleId").description(ApiModelPropertiesGeneric.ITEM_ID),
                                parameterWithName("artifactId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        responseFields(fieldWithPath("id").description(ApiModelPropertiesGeneric.ITEM_ID),
                                fieldWithPath("size").description(MgmtApiModelProperties.ARTIFACT_SIZE),
                                fieldWithPath("createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                                fieldWithPath("createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                                fieldWithPath("lastModifiedBy").description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY),
                                fieldWithPath("lastModifiedAt").description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT),
                                fieldWithPath("hashes.md5").description(MgmtApiModelProperties.ARTIFACT_HASHES_MD5),
                                fieldWithPath("hashes.sha1").description(MgmtApiModelProperties.ARTIFACT_HASHES_SHA1),
                                fieldWithPath("hashes.sha256")
                                        .description(MgmtApiModelProperties.ARTIFACT_HASHES_SHA256),
                                fieldWithPath("providedFilename")
                                        .description(MgmtApiModelProperties.ARTIFACT_PROVIDED_FILENAME),
                                fieldWithPath("_links.self").ignored(), fieldWithPath("_links.download")
                                        .description(MgmtApiModelProperties.ARTIFACT_DOWNLOAD_LINK))));
    }

    @Test
    @Description("Handles the GET request for downloading an artifact. Required Permission: "
            + SpPermission.READ_REPOSITORY)
    public void getDownloadArtifact() throws Exception {

        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        final byte random[] = RandomStringUtils.random(5).getBytes();

        final Artifact artifact = artifactManagement
                .create(new ArtifactUpload(new ByteArrayInputStream(random), sm.getId(), "file1", false, 0));

        mockMvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING
                + "/{softwareModuleId}/artifacts/{artifactId}/download", sm.getId(), artifact.getId())
                        .accept(MediaType.APPLICATION_OCTET_STREAM))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andDo(this.document.document(pathParameters(
                        parameterWithName("softwareModuleId").description(ApiModelPropertiesGeneric.ITEM_ID),
                        parameterWithName("artifactId").description(ApiModelPropertiesGeneric.ITEM_ID))));
    }

    @Test
    @Description(" Get a paged list of meta data for a software module." + " Required Permission: "
            + SpPermission.READ_REPOSITORY)
    public void getMetadata() throws Exception {
        final int totalMetadata = 4;
        final String knownKeyPrefix = "knownKey";
        final String knownValuePrefix = "knownValue";
        final SoftwareModule module = testdataFactory.createDistributionSet("one").findFirstModuleByType(osType).get();

        for (int index = 0; index < totalMetadata; index++) {
            softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata().create(module.getId())
                    .key(knownKeyPrefix + index).value(knownValuePrefix + index));
        }

        mockMvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING + "/{softwareModuleId}/metadata",
                module.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andDo(this.document.document(
                        pathParameters(
                                parameterWithName("softwareModuleId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        responseFields(fieldWithPath("total").description(ApiModelPropertiesGeneric.TOTAL_ELEMENTS),
                                fieldWithPath("size").type(JsonFieldType.NUMBER)
                                        .description(ApiModelPropertiesGeneric.SIZE),
                                fieldWithPath("content").description(MgmtApiModelProperties.META_DATA),
                                fieldWithPath("content[].key").description(MgmtApiModelProperties.META_DATA_KEY),
                                fieldWithPath("content[].value").description(MgmtApiModelProperties.META_DATA_VALUE),
                                fieldWithPath("content[].targetVisible")
                                        .description(MgmtApiModelProperties.SM_META_DATA_TARGET_VISIBLE))));
    }

    @Test
    @Description(" Get a paged list of meta data for a software module with defined page size and sorting by name descending and key starting with 'known'."
            + " Required Permission: " + SpPermission.READ_REPOSITORY)
    public void getMetadataWithParameters() throws Exception {
        final int totalMetadata = 4;
        final String knownKeyPrefix = "knownKey";
        final String knownValuePrefix = "knownValue";
        final SoftwareModule module = testdataFactory.createDistributionSet("one").findFirstModuleByType(osType).get();

        for (int index = 0; index < totalMetadata; index++) {
            softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata().create(module.getId())
                    .key(knownKeyPrefix + index).value(knownValuePrefix + index));
        }

        mockMvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING + "/{softwareModuleId}/metadata",
                module.getId()).param("offset", "1").param("limit", "2").param("sort", "key:DESC").param("q",
                        "key==known*"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andDo(this.document.document(
                        pathParameters(
                                parameterWithName("softwareModuleId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        requestParameters(
                                parameterWithName("limit").attributes(key("type").value("query"))
                                        .description(ApiModelPropertiesGeneric.LIMIT),
                                parameterWithName("sort").description(ApiModelPropertiesGeneric.SORT),
                                parameterWithName("offset").description(ApiModelPropertiesGeneric.OFFSET),
                                parameterWithName("q").description(ApiModelPropertiesGeneric.FIQL))));
    }

    @Test
    @Description("Create a list of meta data entries" + " Required Permission: " + SpPermission.UPDATE_REPOSITORY)
    public void postMetadata() throws Exception {

        final SoftwareModule module = testdataFactory.createDistributionSet("one").findFirstModuleByType(osType).get();

        final String knownKey1 = "knownKey1";
        final String knownKey2 = "knownKey2";

        final String knownValue1 = "knownValue1";
        final String knownValue2 = "knownValue2";

        final JSONArray jsonArray = new JSONArray();
        jsonArray.put(new JSONObject().put("key", knownKey1).put("value", knownValue1));
        jsonArray.put(new JSONObject().put("key", knownKey2).put("value", knownValue2).put("targetVisible", true));

        mockMvc.perform(post(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING + "/{softwareModuleId}/metadata",
                module.getId()).contentType(MediaTypes.HAL_JSON).content(jsonArray.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andDo(this.document.document(
                        pathParameters(
                                parameterWithName("softwareModuleId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        requestFields(requestFieldWithPath("[]key").description(MgmtApiModelProperties.META_DATA_KEY),
                                requestFieldWithPath("[]value").description(MgmtApiModelProperties.META_DATA_VALUE),
                                requestFieldWithPath("[]targetVisible")
                                        .description(MgmtApiModelProperties.SM_META_DATA_TARGET_VISIBLE).optional()),

                        responseFields(fieldWithPath("[]key").description(MgmtApiModelProperties.META_DATA_KEY),
                                fieldWithPath("[]value").description(MgmtApiModelProperties.META_DATA_VALUE),
                                fieldWithPath("[]targetVisible")
                                        .description(MgmtApiModelProperties.SM_META_DATA_TARGET_VISIBLE)

                        )

                ));
    }

    @Test
    @Description(" Delete a single meta data." + " Required Permission: " + SpPermission.UPDATE_REPOSITORY)
    public void deleteMetadata() throws Exception {
        // prepare and create metadata for deletion
        final String knownKey = "knownKey";
        final String knownValue = "knownValue";

        final SoftwareModule module = testdataFactory.createDistributionSet("one").findFirstModuleByType(osType).get();
        softwareModuleManagement.createMetaData(
                entityFactory.softwareModuleMetadata().create(module.getId()).key(knownKey).value(knownValue));

        mockMvc.perform(delete(
                MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING + "/{softwareModuleId}/metadata/{{metadataKey}",
                module.getId(), knownKey)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(pathParameters(
                        parameterWithName("softwareModuleId").description(ApiModelPropertiesGeneric.ITEM_ID),
                        parameterWithName("{metadataKey").description(MgmtApiModelProperties.META_DATA_KEY))));
    }

    @Test
    @Description(" Get a single meta data value for a meta data key." + " Required Permission: "
            + SpPermission.READ_REPOSITORY)
    public void getMetadataValue() throws Exception {

        // prepare and create metadata
        final String knownKey = "knownKey";
        final String knownValue = "knownValue";
        final SoftwareModule module = testdataFactory.createDistributionSet("one").findFirstModuleByType(osType).get();
        softwareModuleManagement.createMetaData(
                entityFactory.softwareModuleMetadata().create(module.getId()).key(knownKey).value(knownValue));

        mockMvc.perform(
                get(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING + "/{softwareModuleId}/metadata/{metadataKey}",
                        module.getId(), knownKey))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(
                        pathParameters(
                                parameterWithName("softwareModuleId").description(ApiModelPropertiesGeneric.ITEM_ID),
                                parameterWithName("metadataKey").description(MgmtApiModelProperties.META_DATA_KEY)),
                        responseFields(fieldWithPath("key").description(MgmtApiModelProperties.META_DATA_KEY),
                                fieldWithPath("value").description(MgmtApiModelProperties.META_DATA_VALUE),
                                fieldWithPath("targetVisible")
                                        .description(MgmtApiModelProperties.SM_META_DATA_TARGET_VISIBLE))));
    }

    @Test
    @Description(" Update a single meta data value for speficic key." + " Required Permission: "
            + SpPermission.UPDATE_REPOSITORY)
    public void putMetadata() throws Exception {
        // prepare and create metadata for update
        final String knownKey = "knownKey";
        final String knownValue = "knownValue";
        final String updateValue = "valueForUpdate";

        final SoftwareModule module = testdataFactory.createDistributionSet("one").findFirstModuleByType(osType).get();
        softwareModuleManagement.createMetaData(
                entityFactory.softwareModuleMetadata().create(module.getId()).key(knownKey).value(knownValue));

        final JSONObject jsonObject = new JSONObject().put("key", knownKey).put("value", updateValue)
                .put("targetVisible", true);

        mockMvc.perform(
                put(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING + "/{softwareModuleId}/metadata/{metadataKey}",
                        module.getId(), knownKey).contentType(MediaTypes.HAL_JSON).content(jsonObject.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("softwareModuleId")
                                .description(ApiModelPropertiesGeneric.ITEM_ID),
                                parameterWithName("metadataKey").description(MgmtApiModelProperties.META_DATA_KEY)),
                        requestFields(requestFieldWithPath("key").description(MgmtApiModelProperties.META_DATA_KEY),
                                requestFieldWithPath("value").description(MgmtApiModelProperties.META_DATA_VALUE),
                                requestFieldWithPath("targetVisible")
                                        .description(MgmtApiModelProperties.SM_META_DATA_TARGET_VISIBLE)),
                        responseFields(fieldWithPath("key").description(MgmtApiModelProperties.META_DATA_KEY),
                                fieldWithPath("value").description(MgmtApiModelProperties.META_DATA_VALUE),
                                fieldWithPath("targetVisible")
                                        .description(MgmtApiModelProperties.SM_META_DATA_TARGET_VISIBLE))));
    }

}
