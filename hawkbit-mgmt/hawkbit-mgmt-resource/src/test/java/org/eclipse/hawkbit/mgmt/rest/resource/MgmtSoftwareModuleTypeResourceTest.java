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
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.jayway.jsonpath.JsonPath;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareModuleTypeRestApi;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Test for {@link MgmtSoftwareModuleTypeResource}.
 * <p/>
 * Feature: Component Tests - Management API<br/>
 * Story: Software Module Type Resource
 */
public class MgmtSoftwareModuleTypeResourceTest extends AbstractManagementApiIntegrationTest {

    /**
     * Checks the correct behaviour of /rest/v1/softwaremoduletypes GET requests.
     */
    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    public void getSoftwareModuleTypes() throws Exception {
        final SoftwareModuleType testType = createTestType();

        mvc.perform(get("/rest/v1/softwaremoduletypes").accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content.[?(@.key=='" + osType.getKey() + "')].name", contains(osType.getName())))
                .andExpect(jsonPath("$.content.[?(@.key=='" + osType.getKey() + "')].description",
                        contains(osType.getDescription())))
                .andExpect(jsonPath("$.content.[?(@.key=='" + osType.getKey() + "')].colour").doesNotExist())
                .andExpect(jsonPath("$.content.[?(@.key=='" + osType.getKey() + "')].maxAssignments", contains(1)))
                .andExpect(jsonPath("$.content.[?(@.key=='" + osType.getKey() + "')].key", contains("os")))
                .andExpect(jsonPath("$.content.[?(@.key=='" + runtimeType.getKey() + "')].name",
                        contains(runtimeType.getName())))
                .andExpect(jsonPath("$.content.[?(@.key=='" + runtimeType.getKey() + "')].description",
                        contains(runtimeType.getDescription())))
                .andExpect(jsonPath("$.content.[?(@.key=='" + runtimeType.getKey() + "')].maxAssignments", contains(1)))
                .andExpect(jsonPath("$.content.[?(@.key=='" + runtimeType.getKey() + "')].key", contains("runtime")))
                .andExpect(
                        jsonPath("$.content.[?(@.key=='" + appType.getKey() + "')].name", contains(appType.getName())))
                .andExpect(jsonPath("$.content.[?(@.key=='" + appType.getKey() + "')].description",
                        contains(appType.getDescription())))
                .andExpect(jsonPath("$.content.[?(@.key=='" + appType.getKey() + "')].colour").doesNotExist())
                .andExpect(jsonPath("$.content.[?(@.key=='" + appType.getKey() + "')].maxAssignments",
                        contains(Integer.MAX_VALUE)))
                .andExpect(jsonPath("$.content.[?(@.key=='" + appType.getKey() + "')].key", contains("application")))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].id", contains(testType.getId().intValue())))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].name", contains("TestName123")))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].description", contains("Desc1234")))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].colour", contains("colour")))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].createdBy", contains("uploadTester")))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].createdAt", contains(testType.getCreatedAt())))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].lastModifiedBy", contains("uploadTester")))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].lastModifiedAt",
                        contains(testType.getLastModifiedAt())))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].maxAssignments", contains(5)))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].key", contains("test123")))
                .andExpect(jsonPath("$.total", equalTo(4)));
    }

    /**
     * Handles the GET request of retrieving all software module types within SP with parameters. In this case the first 10 result in ascending order by name where the name starts with 'a'.
     */
    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    public void getSoftwareModuleTypesWithParameters() throws Exception {
        final SoftwareModuleType testType = testdataFactory.findOrCreateSoftwareModuleType("test123");
        softwareModuleTypeManagement
                .update(SoftwareModuleTypeManagement.Update.builder().id(testType.getId()).description("Desc1234").colour("rgb(106,178,83)")
                        .build());

        mvc.perform(get(MgmtSoftwareModuleTypeRestApi.SOFTWAREMODULETYPES_V1 + "?limit=10&sort=name:ASC&offset=0&q=name==a")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    /**
     * Checks the correct behaviour of /rest/v1/softwaremoduletypes GET requests with sorting by MAXASSIGNMENTS field.
     */
    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    public void getSoftwareModuleTypesSortedByMaxAssignments() throws Exception {
        final SoftwareModuleType testType = createTestType();

        // descending
        mvc.perform(get("/rest/v1/softwaremoduletypes").accept(MediaType.APPLICATION_JSON)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "MAXASSIGNMENTS:DESC"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content.[1].id", equalTo(testType.getId().intValue())))
                .andExpect(jsonPath("$.content.[1].name", equalTo("TestName123")))
                .andExpect(jsonPath("$.content.[1].description", equalTo("Desc1234")))
                .andExpect(jsonPath("$.content.[1].colour", equalTo("colour")))
                .andExpect(jsonPath("$.content.[1].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("$.content.[1].createdAt", equalTo(testType.getCreatedAt())))
                .andExpect(jsonPath("$.content.[1].lastModifiedBy", equalTo("uploadTester")))
                .andExpect(jsonPath("$.content.[1].lastModifiedAt", equalTo(testType.getLastModifiedAt())))
                .andExpect(jsonPath("$.content.[1].maxAssignments", equalTo(5)))
                .andExpect(jsonPath("$.content.[1].key", equalTo("test123")))
                .andExpect(jsonPath("$.total", equalTo(4)));

        // ascending
        mvc.perform(get("/rest/v1/softwaremoduletypes").accept(MediaType.APPLICATION_JSON)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "MAXASSIGNMENTS:ASC"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content.[2].id", equalTo(testType.getId().intValue())))
                .andExpect(jsonPath("$.content.[2].name", equalTo("TestName123")))
                .andExpect(jsonPath("$.content.[2].description", equalTo("Desc1234")))
                .andExpect(jsonPath("$.content.[2].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("$.content.[2].createdAt", equalTo(testType.getCreatedAt())))
                .andExpect(jsonPath("$.content.[2].lastModifiedBy", equalTo("uploadTester")))
                .andExpect(jsonPath("$.content.[2].lastModifiedAt", equalTo(testType.getLastModifiedAt())))
                .andExpect(jsonPath("$.content.[2].maxAssignments", equalTo(5)))
                .andExpect(jsonPath("$.content.[2].key", equalTo("test123")))
                .andExpect(jsonPath("$.total", equalTo(4)));
    }

    /**
     * Checks the correct behaviour of /rest/v1/softwaremoduletypes POST requests when max assignment is smaller than 1
     */
    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    public void createSoftwareModuleTypesInvalidAssignmentBadRequest() throws Exception {

        final List<SoftwareModuleTypeManagement.Create> types = new ArrayList<>();
        types.add(SoftwareModuleTypeManagement.Create.builder().key("test-1").name("TestName-1").maxAssignments(-1).build());

        mvc.perform(post("/rest/v1/softwaremoduletypes").content(toJson(types))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        types.clear();
        types.add(SoftwareModuleTypeManagement.Create.builder().key("test0").name("TestName0").maxAssignments(0).build());

        mvc.perform(post("/rest/v1/softwaremoduletypes").content(toJson(types))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    /**
     * Checks the correct behaviour of /rest/v1/softwaremoduletypes POST requests.
     */
    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    public void createSoftwareModuleTypes() throws Exception {

        final List<SoftwareModuleTypeManagement.Create> types = Arrays.asList(
                SoftwareModuleTypeManagement.Create.builder().key("test1").name("TestName1").description("Desc1")
                        .colour("col1‚").maxAssignments(1).build(),
                SoftwareModuleTypeManagement.Create.builder().key("test2").name("TestName2").description("Desc2")
                        .colour("col2‚").maxAssignments(2).build(),
                SoftwareModuleTypeManagement.Create.builder().key("test3").name("TestName3").description("Desc3")
                        .colour("col3‚").maxAssignments(3).build());

        final MvcResult mvcResult = mvc
                .perform(post("/rest/v1/softwaremoduletypes").content(toJson(types))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("[0].name", equalTo("TestName1")))
                .andExpect(jsonPath("[0].key", equalTo("test1")))
                .andExpect(jsonPath("[0].description", equalTo("Desc1")))
                .andExpect(jsonPath("[0].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[0].maxAssignments", equalTo(1)))
                .andExpect(jsonPath("[1].name", equalTo("TestName2")))
                .andExpect(jsonPath("[1].key", equalTo("test2")))
                .andExpect(jsonPath("[1].description", equalTo("Desc2")))
                .andExpect(jsonPath("[1].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[1].maxAssignments", equalTo(2)))
                .andExpect(jsonPath("[2].name", equalTo("TestName3")))
                .andExpect(jsonPath("[2].key", equalTo("test3")))
                .andExpect(jsonPath("[2].description", equalTo("Desc3")))
                .andExpect(jsonPath("[2].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[2].createdAt", not(equalTo(0))))
                .andExpect(jsonPath("[2].maxAssignments", equalTo(3)))
                .andReturn();

        final SoftwareModuleType created1 = softwareModuleTypeManagement.findByKey("test1").get();
        final SoftwareModuleType created2 = softwareModuleTypeManagement.findByKey("test2").get();
        final SoftwareModuleType created3 = softwareModuleTypeManagement.findByKey("test3").get();

        assertThat((Object) JsonPath.compile("[0]_links.self.href").read(mvcResult.getResponse().getContentAsString()))
                .hasToString("http://localhost/rest/v1/softwaremoduletypes/" + created1.getId());
        assertThat((Object) JsonPath.compile("[1]_links.self.href").read(mvcResult.getResponse().getContentAsString()))
                .hasToString("http://localhost/rest/v1/softwaremoduletypes/" + created2.getId());
        assertThat((Object) JsonPath.compile("[2]_links.self.href").read(mvcResult.getResponse().getContentAsString()))
                .hasToString("http://localhost/rest/v1/softwaremoduletypes/" + created3.getId());

        assertThat(softwareModuleTypeManagement.count()).isEqualTo(6);
    }

    /**
     * Checks the correct behaviour of /rest/v1/softwaremoduletypes/{ID} GET requests.
     */
    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    public void getSoftwareModuleType() throws Exception {
        final SoftwareModuleType testType = createTestType();

        mvc.perform(get("/rest/v1/softwaremoduletypes/{smtId}", testType.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.name", equalTo("TestName123")))
                .andExpect(jsonPath("$.description", equalTo("Desc1234")))
                .andExpect(jsonPath("$.colour", equalTo("colour")))
                .andExpect(jsonPath("$.maxAssignments", equalTo(5)))
                .andExpect(jsonPath("$.createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("$.createdAt", equalTo(testType.getCreatedAt())))
                .andExpect(jsonPath("$.lastModifiedBy", equalTo("uploadTester")))
                .andExpect(jsonPath("$.lastModifiedAt", equalTo(testType.getLastModifiedAt())))
                .andExpect(jsonPath("$.deleted", equalTo(testType.isDeleted())));
    }

    /**
     * Checks the correct behaviour of /rest/v1/softwaremoduletypes/{ID} DELETE requests (hard delete scenario).
     */
    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    public void deleteSoftwareModuleTypeUnused() throws Exception {
        final SoftwareModuleType testType = createTestType();

        assertThat(softwareModuleTypeManagement.count()).isEqualTo(4);

        mvc.perform(delete("/rest/v1/softwaremoduletypes/{smId}", testType.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        assertThat(softwareModuleTypeManagement.count()).isEqualTo(3);
    }

    /**
     * Ensures that module type deletion request to API on an entity that does not exist results in NOT_FOUND.
     */
    @Test
    void deleteSoftwareModuleTypeThatDoesNotExistLeadsToNotFound() throws Exception {
        mvc.perform(delete("/rest/v1/softwaremoduletypes/1234"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    /**
     * Checks the correct behaviour of /rest/v1/softwaremoduletypes/{ID} DELETE requests (soft delete scenario).
     */
    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    public void deleteSoftwareModuleTypeUsed() throws Exception {
        final SoftwareModuleType testType = createTestType();
        softwareModuleManagement.create(SoftwareModuleManagement.Create.builder().type(testType).name("name").version("version").build());

        assertThat(softwareModuleTypeManagement.count()).isEqualTo(4);

        mvc.perform(get("/rest/v1/softwaremoduletypes/{smtId}", testType.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted", equalTo(false)));

        mvc.perform(delete("/rest/v1/softwaremoduletypes/{smtId}", testType.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        mvc.perform(get("/rest/v1/softwaremoduletypes/{smtId}", testType.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted", equalTo(true)));

        assertThat(softwareModuleTypeManagement.count()).isEqualTo(3);
    }

    /**
     * Checks the correct behaviour of /rest/v1/softwaremoduletypes/{ID} PUT requests.
     */
    @Test
    void updateSoftwareModuleTypeColourDescriptionAndNameUntouched() throws Exception {
        final SoftwareModuleType testType = createTestType();

        final String body = new JSONObject().put("id", testType.getId()).put("description", "foobardesc")
                .put("colour", "updatedColour").put("name", "nameShouldNotBeChanged").toString();

        mvc.perform(put("/rest/v1/softwaremoduletypes/{smId}", testType.getId()).content(body)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(testType.getId().intValue())))
                .andExpect(jsonPath("$.description", equalTo("foobardesc")))
                .andExpect(jsonPath("$.colour", equalTo("updatedColour")))
                .andExpect(jsonPath("$.name", equalTo("TestName123")))
                .andReturn();

    }

    /**
     * Tests the update of the deletion flag. It is verfied that the software module type can't be marked as deleted through update operation.
     */
    @Test
    void updateSoftwareModuleTypeDeletedFlag() throws Exception {
        SoftwareModuleType testType = createTestType();

        final String body = new JSONObject().put("id", testType.getId()).put("deleted", true).toString();

        mvc.perform(put("/rest/v1/softwaremoduletypes/{smtId}", testType.getId()).content(body)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(testType.getId().intValue())))
                .andExpect(jsonPath("$.lastModifiedAt", equalTo(testType.getLastModifiedAt())))
                .andExpect(jsonPath("$.deleted", equalTo(false)));

        testType = softwareModuleTypeManagement.find(testType.getId()).get();
        assertThat(testType.getLastModifiedAt()).isEqualTo(testType.getLastModifiedAt());
        assertThat(testType.isDeleted()).isFalse();
    }

    /**
     * Checks the correct behaviour of /rest/v1/softwaremoduletypes GET requests with paging.
     */
    @Test
    void getSoftwareModuleTypesWithoutAddtionalRequestParameters() throws Exception {
        final int types = 3;
        mvc.perform(get(MgmtSoftwareModuleTypeRestApi.SOFTWAREMODULETYPES_V1))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(types)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(types)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(types)));
    }

    /**
     * Checks the correct behaviour of /rest/v1/softwaremoduletypes GET requests with paging.
     */
    @Test
    void getSoftwareModuleTypesWithPagingLimitRequestParameter() throws Exception {
        final int types = 3;
        final int limitSize = 1;
        mvc.perform(get(MgmtSoftwareModuleTypeRestApi.SOFTWAREMODULETYPES_V1)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(limitSize)))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(types)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(limitSize)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(limitSize)));
    }

    /**
     * Checks the correct behaviour of /rest/v1/softwaremoduletypes GET requests with paging.
     */
    @Test
    void getSoftwareModuleTypesWithPagingLimitAndOffsetRequestParameter() throws Exception {
        final int types = 3;
        final int offsetParam = 2;
        final int expectedSize = types - offsetParam;
        mvc.perform(get(MgmtSoftwareModuleTypeRestApi.SOFTWAREMODULETYPES_V1)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(offsetParam))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(types)))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(types)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(expectedSize)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(expectedSize)));
    }

    /**
     * Ensures that the server is behaving as expected on invalid requests (wrong media type, wrong ID etc.).
     */
    @Test
    void invalidRequestsOnSoftwareModuleTypesResource() throws Exception {
        final SoftwareModuleType testType = createTestType();

        final List<SoftwareModuleTypeManagement.Create> types = List.of(SoftwareModuleTypeManagement.Create.builder()
                .key(testType.getKey())
                .name(testType.getName())
                .build());

        // SM does not exist
        mvc.perform(get("/rest/v1/softwaremoduletypes/12345678"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        mvc.perform(delete("/rest/v1/softwaremoduletypes/12345678"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // bad request - no content
        mvc.perform(post("/rest/v1/softwaremoduletypes").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        // bad request - bad content
        mvc.perform(post("/rest/v1/softwaremoduletypes").content("sdfjsdlkjfskdjf".getBytes())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        mvc.perform(post("/rest/v1/softwaremoduletypes").content(
                                "[{\"description\":\"Desc123\",\"id\":9223372036854775807,\"key\":\"test123\",\"maxAssignments\":5}]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        final SoftwareModuleTypeManagement.Create toLongName = SoftwareModuleTypeManagement.Create.builder()
                .key("test123")
                .name(randomString(NamedEntity.NAME_MAX_SIZE + 1))
                .build();
        mvc.perform(post("/rest/v1/softwaremoduletypes").content(toJson(List.of(toLongName)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        // unsupported media type
        mvc.perform(post("/rest/v1/softwaremoduletypes").content(toJson(types))
                        .contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isUnsupportedMediaType());

        // not allowed methods
        mvc.perform(put("/rest/v1/softwaremoduletypes"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(delete("/rest/v1/softwaremoduletypes"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

    }

    /**
     * Search erquest of software module types.
     */
    @Test
    void searchSoftwareModuleTypeRsql() throws Exception {
        softwareModuleTypeManagement.create(SoftwareModuleTypeManagement.Create.builder().key("test123")
                .name("TestName123").description("Desc123").maxAssignments(5).build());
        softwareModuleTypeManagement.create(SoftwareModuleTypeManagement.Create.builder().key("test1234")
                .name("TestName1234").description("Desc1234").maxAssignments(5).build());

        final String rsqlFindLikeDs1OrDs2 = "name==TestName123,name==TestName1234";

        mvc.perform(get("/rest/v1/softwaremoduletypes?q=" + rsqlFindLikeDs1OrDs2))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("size", equalTo(2)))
                .andExpect(jsonPath("total", equalTo(2)))
                .andExpect(jsonPath("content[0].name", equalTo("TestName123")))
                .andExpect(jsonPath("content[1].name", equalTo("TestName1234")));

    }

    private SoftwareModuleType createTestType() {
        final SoftwareModuleType testType = softwareModuleTypeManagement.create(SoftwareModuleTypeManagement.Create.builder()
                .key("test123").name("TestName123").description("Desc123").colour("colour").maxAssignments(5).build());
        return softwareModuleTypeManagement
                .update(SoftwareModuleTypeManagement.Update.builder().id(testType.getId()).description("Desc1234").build());
    }
}