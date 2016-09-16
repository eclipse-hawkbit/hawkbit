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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.eclipse.hawkbit.rest.AbstractRestIntegrationTest;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONException;
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
public class MgmtSoftwareModuleTypeResourceTest extends AbstractRestIntegrationTest {

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/softwaremoduletypes GET requests.")
    public void getSoftwareModuleTypes() throws Exception {
        SoftwareModuleType testType = softwareManagement.createSoftwareModuleType(
                entityFactory.generateSoftwareModuleType("test123", "TestName123", "Desc123", 5));
        testType.setDescription("Desc1234");
        testType = softwareManagement.updateSoftwareModuleType(testType);

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

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/softwaremoduletypes GET requests with sorting by MAXASSIGNMENTS field.")
    public void getSoftwareModuleTypesSortedByMaxAssignments() throws Exception {
        SoftwareModuleType testType = softwareManagement.createSoftwareModuleType(
                entityFactory.generateSoftwareModuleType("test123", "TestName123", "Desc123", 5));
        testType.setDescription("Desc1234");
        testType = softwareManagement.updateSoftwareModuleType(testType);

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
    public void createSoftwareModuleTypesInvalidAssignmentBadRequest() throws JSONException, Exception {

        final List<SoftwareModuleType> types = new ArrayList<>();
        types.add(entityFactory.generateSoftwareModuleType("test-1", "TestName-1", "Desc-1", -1));

        mvc.perform(post("/rest/v1/softwaremoduletypes/").content(JsonBuilder.softwareModuleTypes(types))
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        types.clear();
        types.add(entityFactory.generateSoftwareModuleType("test0", "TestName0", "Desc0", 0));

        mvc.perform(post("/rest/v1/softwaremoduletypes/").content(JsonBuilder.softwareModuleTypes(types))
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/softwaremoduletypes POST requests.")
    public void createSoftwareModuleTypes() throws JSONException, Exception {

        final List<SoftwareModuleType> types = new ArrayList<>();
        types.add(entityFactory.generateSoftwareModuleType("test1", "TestName1", "Desc1", 1));
        types.add(entityFactory.generateSoftwareModuleType("test2", "TestName2", "Desc2", 2));
        types.add(entityFactory.generateSoftwareModuleType("test3", "TestName3", "Desc3", 3));

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

        final SoftwareModuleType created1 = softwareManagement.findSoftwareModuleTypeByKey("test1");
        final SoftwareModuleType created2 = softwareManagement.findSoftwareModuleTypeByKey("test2");
        final SoftwareModuleType created3 = softwareManagement.findSoftwareModuleTypeByKey("test3");

        assertThat(
                JsonPath.compile("[0]_links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                        .isEqualTo("http://localhost/rest/v1/softwaremoduletypes/" + created1.getId());
        assertThat(
                JsonPath.compile("[1]_links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                        .isEqualTo("http://localhost/rest/v1/softwaremoduletypes/" + created2.getId());
        assertThat(
                JsonPath.compile("[2]_links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                        .isEqualTo("http://localhost/rest/v1/softwaremoduletypes/" + created3.getId());

        assertThat(softwareManagement.countSoftwareModuleTypesAll()).isEqualTo(6);
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/softwaremoduletypes/{ID} GET requests.")
    public void getSoftwareModuleType() throws Exception {
        SoftwareModuleType testType = softwareManagement.createSoftwareModuleType(
                entityFactory.generateSoftwareModuleType("test123", "TestName123", "Desc123", 5));
        testType.setDescription("Desc1234");
        testType = softwareManagement.updateSoftwareModuleType(testType);

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
        final SoftwareModuleType testType = softwareManagement.createSoftwareModuleType(
                entityFactory.generateSoftwareModuleType("test123", "TestName123", "Desc123", 5));

        assertThat(softwareManagement.countSoftwareModuleTypesAll()).isEqualTo(4);

        mvc.perform(delete("/rest/v1/softwaremoduletypes/{smId}", testType.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        assertThat(softwareManagement.countSoftwareModuleTypesAll()).isEqualTo(3);
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/softwaremoduletypes/{ID} DELETE requests (soft delete scenario).")
    public void deleteSoftwareModuleTypeUsed() throws Exception {
        final SoftwareModuleType testType = softwareManagement.createSoftwareModuleType(
                entityFactory.generateSoftwareModuleType("test123", "TestName123", "Desc123", 5));
        softwareManagement.createSoftwareModule(
                entityFactory.generateSoftwareModule(testType, "name", "version", "description", "vendor"));

        assertThat(softwareManagement.countSoftwareModuleTypesAll()).isEqualTo(4);

        mvc.perform(delete("/rest/v1/softwaremoduletypes/{smId}", testType.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        assertThat(softwareManagement.countSoftwareModuleTypesAll()).isEqualTo(3);
    }

    @Test
    @Description("Checks the correct behaviour of /rest/v1/softwaremoduletypes/{ID} PUT requests.")
    public void updateSoftwareModuleTypeOnlyDescriptionAndNameUntouched() throws Exception {
        final SoftwareModuleType testType = softwareManagement.createSoftwareModuleType(
                entityFactory.generateSoftwareModuleType("test123", "TestName123", "Desc123", 5));

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
        final SoftwareModuleType testType = softwareManagement.createSoftwareModuleType(
                entityFactory.generateSoftwareModuleType("test123", "TestName123", "Desc123", 5));

        final List<SoftwareModuleType> types = new ArrayList<>();
        types.add(testType);

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
        final SoftwareModuleType testType = softwareManagement.createSoftwareModuleType(
                entityFactory.generateSoftwareModuleType("test123", "TestName123", "Desc123", 5));
        final SoftwareModuleType testType2 = softwareManagement.createSoftwareModuleType(
                entityFactory.generateSoftwareModuleType("test1234", "TestName1234", "Desc123", 5));

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
            final SoftwareModule softwareModule = entityFactory.generateSoftwareModule(osType, str, str, str, str);

            softwareManagement.createSoftwareModule(softwareModule);
            character++;
        }
    }

}
