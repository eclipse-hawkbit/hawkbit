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

import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONObject;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Test for {@link MgmtSoftwareModuleTypeResource}.
 *
 */
@Features("Component Tests - Management API")
@Stories("Software Module Type Resource")
public class MgmtSoftwareModuleTypeResourceTest extends AbstractManagementApiIntegrationTest {

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/softwaremoduletypes GET requests.")
    public void getSoftwareModuleTypes() throws Exception {
        final SoftwareModuleType testType = createTestType();

        mvc.perform(get("/rest/v1/softwaremoduletypes").accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.content.[?(@.key==" + osType.getKey() + ")].name", contains(osType.getName())))
                .andExpect(jsonPath("$.content.[?(@.key==" + osType.getKey() + ")].description",
                        contains(osType.getDescription())))
                .andExpect(jsonPath("$.content.[?(@.key==" + osType.getKey() + ")].maxAssignments", contains(1)))
                .andExpect(jsonPath("$.content.[?(@.key==" + osType.getKey() + ")].key", contains("os")))
                .andExpect(jsonPath("$.content.[?(@.key==" + runtimeType.getKey() + ")].name",
                        contains(runtimeType.getName())))
                .andExpect(jsonPath("$.content.[?(@.key==" + runtimeType.getKey() + ")].description",
                        contains(runtimeType.getDescription())))
                .andExpect(jsonPath("$.content.[?(@.key==" + runtimeType.getKey() + ")].maxAssignments", contains(1)))
                .andExpect(jsonPath("$.content.[?(@.key==" + runtimeType.getKey() + ")].key", contains("runtime")))
                .andExpect(jsonPath("$.content.[?(@.key==" + appType.getKey() + ")].name", contains(appType.getName())))
                .andExpect(jsonPath("$.content.[?(@.key==" + appType.getKey() + ")].description",
                        contains(appType.getDescription())))
                .andExpect(jsonPath("$.content.[?(@.key==" + appType.getKey() + ")].maxAssignments",
                        contains(Integer.MAX_VALUE)))
                .andExpect(jsonPath("$.content.[?(@.key==" + appType.getKey() + ")].key", contains("application")))
                .andExpect(jsonPath("$.content.[?(@.key==test123)].id", contains(testType.getId().intValue())))
                .andExpect(jsonPath("$.content.[?(@.key==test123)].name", contains("TestName123")))
                .andExpect(jsonPath("$.content.[?(@.key==test123)].description", contains("Desc1234")))
                .andExpect(jsonPath("$.content.[?(@.key==test123)].createdBy", contains("uploadTester")))
                .andExpect(jsonPath("$.content.[?(@.key==test123)].createdAt", contains(testType.getCreatedAt())))
                .andExpect(jsonPath("$.content.[?(@.key==test123)].lastModifiedBy", contains("uploadTester")))
                .andExpect(jsonPath("$.content.[?(@.key==test123)].lastModifiedAt",
                        contains(testType.getLastModifiedAt())))
                .andExpect(jsonPath("$.content.[?(@.key==test123)].maxAssignments", contains(5)))
                .andExpect(jsonPath("$.content.[?(@.key==test123)].key", contains("test123")))
                .andExpect(jsonPath("$.total", equalTo(4)));
    }

    private SoftwareModuleType createTestType() {
        SoftwareModuleType testType = softwareModuleTypeManagement.create(entityFactory.softwareModuleType().create()
                .key("test123").name("TestName123").description("Desc123").maxAssignments(5));
        testType = softwareModuleTypeManagement
                .update(entityFactory.softwareModuleType().update(testType.getId()).description("Desc1234"));
        return testType;
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/softwaremoduletypes GET requests with sorting by MAXASSIGNMENTS field.")
    public void getSoftwareModuleTypesSortedByMaxAssignments() throws Exception {
        final SoftwareModuleType testType = createTestType();

        // descending
        mvc.perform(get("/rest/v1/softwaremoduletypes").accept(MediaType.APPLICATION_JSON)
                .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "MAXASSIGNMENTS:DESC"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.content.[1].id", equalTo(testType.getId().intValue())))
                .andExpect(jsonPath("$.content.[1].name", equalTo("TestName123")))
                .andExpect(jsonPath("$.content.[1].description", equalTo("Desc1234")))
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
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
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

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/softwaremoduletypes POST requests when max assignment is smaller than 1")
    public void createSoftwareModuleTypesInvalidAssignmentBadRequest() throws Exception {

        final List<SoftwareModuleType> types = new ArrayList<>();
        types.add(entityFactory.softwareModuleType().create().key("test-1").name("TestName-1").maxAssignments(-1)
                .build());

        mvc.perform(post("/rest/v1/softwaremoduletypes/").content(JsonBuilder.softwareModuleTypes(types))
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        types.clear();
        types.add(entityFactory.softwareModuleType().create().key("test0").name("TestName0").maxAssignments(0).build());

        mvc.perform(post("/rest/v1/softwaremoduletypes/").content(JsonBuilder.softwareModuleTypes(types))
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/softwaremoduletypes POST requests.")
    public void createSoftwareModuleTypes() throws Exception {

        final List<SoftwareModuleType> types = Arrays.asList(
                entityFactory.softwareModuleType().create().key("test1").name("TestName1").description("Desc1")
                        .colour("col1‚").maxAssignments(1).build(),
                entityFactory.softwareModuleType().create().key("test2").name("TestName2").description("Desc2")
                        .colour("col2‚").maxAssignments(2).build(),
                entityFactory.softwareModuleType().create().key("test3").name("TestName3").description("Desc3")
                        .colour("col3‚").maxAssignments(3).build());

        final MvcResult mvcResult = mvc
                .perform(post("/rest/v1/softwaremoduletypes/").content(JsonBuilder.softwareModuleTypes(types))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("[0].name", equalTo("TestName1"))).andExpect(jsonPath("[0].key", equalTo("test1")))
                .andExpect(jsonPath("[0].description", equalTo("Desc1")))
                .andExpect(jsonPath("[0].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[0].maxAssignments", equalTo(1)))
                .andExpect(jsonPath("[1].name", equalTo("TestName2"))).andExpect(jsonPath("[1].key", equalTo("test2")))
                .andExpect(jsonPath("[1].description", equalTo("Desc2")))
                .andExpect(jsonPath("[1].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[1].maxAssignments", equalTo(2)))
                .andExpect(jsonPath("[2].name", equalTo("TestName3"))).andExpect(jsonPath("[2].key", equalTo("test3")))
                .andExpect(jsonPath("[2].description", equalTo("Desc3")))
                .andExpect(jsonPath("[2].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[2].createdAt", not(equalTo(0))))
                .andExpect(jsonPath("[2].maxAssignments", equalTo(3))).andReturn();

        final SoftwareModuleType created1 = softwareModuleTypeManagement.getByKey("test1").get();
        final SoftwareModuleType created2 = softwareModuleTypeManagement.getByKey("test2").get();
        final SoftwareModuleType created3 = softwareModuleTypeManagement.getByKey("test3").get();

        assertThat(
                JsonPath.compile("[0]_links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                        .isEqualTo("http://localhost/rest/v1/softwaremoduletypes/" + created1.getId());
        assertThat(
                JsonPath.compile("[1]_links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                        .isEqualTo("http://localhost/rest/v1/softwaremoduletypes/" + created2.getId());
        assertThat(
                JsonPath.compile("[2]_links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                        .isEqualTo("http://localhost/rest/v1/softwaremoduletypes/" + created3.getId());

        assertThat(softwareModuleTypeManagement.count()).isEqualTo(6);
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/softwaremoduletypes/{ID} GET requests.")
    public void getSoftwareModuleType() throws Exception {
        final SoftwareModuleType testType = createTestType();

        mvc.perform(get("/rest/v1/softwaremoduletypes/{smtId}", testType.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.name", equalTo("TestName123")))
                .andExpect(jsonPath("$.description", equalTo("Desc1234")))
                .andExpect(jsonPath("$.maxAssignments", equalTo(5)))
                .andExpect(jsonPath("$.createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("$.createdAt", equalTo(testType.getCreatedAt())))
                .andExpect(jsonPath("$.lastModifiedBy", equalTo("uploadTester")))
                .andExpect(jsonPath("$.lastModifiedAt", equalTo(testType.getLastModifiedAt())));
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/softwaremoduletypes/{ID} DELETE requests (hard delete scenario).")
    public void deleteSoftwareModuleTypeUnused() throws Exception {
        final SoftwareModuleType testType = createTestType();

        assertThat(softwareModuleTypeManagement.count()).isEqualTo(4);

        mvc.perform(delete("/rest/v1/softwaremoduletypes/{smId}", testType.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        assertThat(softwareModuleTypeManagement.count()).isEqualTo(3);
    }

    @Test
    @Description("Ensures that module type deletion request to API on an entity that does not exist results in NOT_FOUND.")
    public void deleteSoftwareModuleTypeThatDoesNotExistLeadsToNotFound() throws Exception {
        mvc.perform(delete("/rest/v1/softwaremoduletypes/1234")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/softwaremoduletypes/{ID} DELETE requests (soft delete scenario).")
    public void deleteSoftwareModuleTypeUsed() throws Exception {
        final SoftwareModuleType testType = createTestType();
        softwareModuleManagement
                .create(entityFactory.softwareModule().create().type(testType).name("name").version("version"));

        assertThat(softwareModuleTypeManagement.count()).isEqualTo(4);

        mvc.perform(delete("/rest/v1/softwaremoduletypes/{smId}", testType.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        assertThat(softwareModuleTypeManagement.count()).isEqualTo(3);
    }

    @Test
    @Description("Checks the correct behaviour of /rest/v1/softwaremoduletypes/{ID} PUT requests.")
    public void updateSoftwareModuleTypeOnlyDescriptionAndNameUntouched() throws Exception {
        final SoftwareModuleType testType = createTestType();

        final String body = new JSONObject().put("id", testType.getId()).put("description", "foobardesc")
                .put("name", "nameShouldNotBeChanged").toString();

        mvc.perform(put("/rest/v1/softwaremoduletypes/{smId}", testType.getId()).content(body)
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(testType.getId().intValue())))
                .andExpect(jsonPath("$.description", equalTo("foobardesc")))
                .andExpect(jsonPath("$.name", equalTo("TestName123"))).andReturn();

    }

    @Test
    @Description("Checks the correct behaviour of /rest/v1/softwaremoduletypes GET requests with paging.")
    public void getSoftwareModuleTypesWithoutAddtionalRequestParameters() throws Exception {
        final int types = 3;
        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULETYPE_V1_REQUEST_MAPPING)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(types)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(types)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(types)));
    }

    @Test
    @Description("Checks the correct behaviour of /rest/v1/softwaremoduletypes GET requests with paging.")
    public void getSoftwareModuleTypesWithPagingLimitRequestParameter() throws Exception {
        final int types = 3;
        final int limitSize = 1;
        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULETYPE_V1_REQUEST_MAPPING)
                .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(limitSize)))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(types)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(limitSize)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(limitSize)));
    }

    @Test
    @Description("Checks the correct behaviour of /rest/v1/softwaremoduletypes GET requests with paging.")
    public void getSoftwareModuleTypesWithPagingLimitAndOffsetRequestParameter() throws Exception {
        final int types = 3;
        final int offsetParam = 2;
        final int expectedSize = types - offsetParam;
        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULETYPE_V1_REQUEST_MAPPING)
                .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(offsetParam))
                .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(types)))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(types)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(expectedSize)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(expectedSize)));
    }

    @Test
    @Description("Ensures that the server is behaving as expected on invalid requests (wrong media type, wrong ID etc.).")
    public void invalidRequestsOnSoftwaremoduleTypesResource() throws Exception {
        final SoftwareModuleType testType = createTestType();

        final List<SoftwareModuleType> types = Arrays.asList(testType);

        // SM does not exist
        mvc.perform(get("/rest/v1/softwaremoduletypes/12345678")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        mvc.perform(delete("/rest/v1/softwaremoduletypes/12345678")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // bad request - no content
        mvc.perform(post("/rest/v1/softwaremoduletypes").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        // bad request - bad content
        mvc.perform(post("/rest/v1/softwaremoduletypes").content("sdfjsdlkjfskdjf".getBytes())
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        mvc.perform(post("/rest/v1/softwaremoduletypes").content(
                "[{\"description\":\"Desc123\",\"id\":9223372036854775807,\"key\":\"test123\",\"maxAssignments\":5}]")
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        final SoftwareModuleType toLongName = entityFactory.softwareModuleType().create().key("test123")
                .name(RandomStringUtils.randomAlphanumeric(80)).build();
        mvc.perform(
                post("/rest/v1/softwaremoduletypes").content(JsonBuilder.softwareModuleTypes(Arrays.asList(toLongName)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        // unsupported media type
        mvc.perform(post("/rest/v1/softwaremoduletypes").content(JsonBuilder.softwareModuleTypes(types))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isUnsupportedMediaType());

        // not allowed methods
        mvc.perform(put("/rest/v1/softwaremoduletypes")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(delete("/rest/v1/softwaremoduletypes")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

    }

    @Test
    @Description("Search erquest of software module types.")
    public void searchSoftwareModuleTypeRsql() throws Exception {
        softwareModuleTypeManagement.create(entityFactory.softwareModuleType().create().key("test123")
                .name("TestName123").description("Desc123").maxAssignments(5));
        softwareModuleTypeManagement.create(entityFactory.softwareModuleType().create().key("test1234")
                .name("TestName1234").description("Desc1234").maxAssignments(5));

        final String rsqlFindLikeDs1OrDs2 = "name==TestName123,name==TestName1234";

        mvc.perform(get("/rest/v1/softwaremoduletypes?q=" + rsqlFindLikeDs1OrDs2)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("size", equalTo(2)))
                .andExpect(jsonPath("total", equalTo(2))).andExpect(jsonPath("content[0].name", equalTo("TestName123")))
                .andExpect(jsonPath("content[1].name", equalTo("TestName1234")));

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
