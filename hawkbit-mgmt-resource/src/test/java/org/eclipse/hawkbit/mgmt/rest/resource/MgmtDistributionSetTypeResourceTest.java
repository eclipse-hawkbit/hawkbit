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
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
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

import com.google.common.collect.Lists;
import com.jayway.jsonpath.JsonPath;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Test for {@link MgmtDistributionSetTypeResource}.
 *
 */
@Features("Component Tests - Management API")
@Stories("Distribution Set Type Resource")
public class MgmtDistributionSetTypeResourceTest extends AbstractRestIntegrationTest {

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/distributionsettypes GET requests.")
    public void getDistributionSetTypes() throws Exception {

        DistributionSetType testType = distributionSetManagement.createDistributionSetType(
                entityFactory.generateDistributionSetType("test123", "TestName123", "Desc123"));
        testType = distributionSetManagement.updateDistributionSetType(testType.getId(), "Desc1234", null);

        // 4 types overall (2 hawkbit tenant default, 1 test default and 1
        // generated in this test)
        mvc.perform(get("/rest/v1/distributionsettypes").accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.[?(@.key==" + standardDsType.getKey() + ")]$..name",
                        contains(standardDsType.getName())))
                .andExpect(jsonPath("$.content.[?(@.key==" + standardDsType.getKey() + ")]$..description",
                        contains(standardDsType.getDescription())))
                .andExpect(jsonPath("$.content.[?(@.key==" + standardDsType.getKey() + ")]$..key",
                        contains(standardDsType.getKey())))
                .andExpect(jsonPath("$.content.[?(@.key==test123)]$..id", contains(testType.getId().intValue())))
                .andExpect(jsonPath("$.content.[?(@.key==test123)]$..name", contains("TestName123")))
                .andExpect(jsonPath("$.content.[?(@.key==test123)]$..description", contains("Desc1234")))
                .andExpect(jsonPath("$.content.[?(@.key==test123)]$..createdBy", contains("uploadTester")))
                .andExpect(jsonPath("$.content.[?(@.key==test123)]$..createdAt", contains(testType.getCreatedAt())))
                .andExpect(jsonPath("$.content.[?(@.key==test123)]$..lastModifiedBy", contains("uploadTester")))
                .andExpect(jsonPath("$.content.[?(@.key==test123)]$..lastModifiedAt",
                        contains(testType.getLastModifiedAt())))
                .andExpect(jsonPath("$.content.[?(@.key==test123)]$..key", contains("test123")))
                .andExpect(jsonPath("$.content.[?(@.key==test123)]$.._links.self.href",
                        contains("http://localhost/rest/v1/distributionsettypes/" + testType.getId())))
                .andExpect(jsonPath("$.content.[?(@.key==test123)]$.._links.mandatorymodules.href",
                        contains("http://localhost/rest/v1/distributionsettypes/" + testType.getId()
                                + "/mandatorymoduletypes")))
                .andExpect(jsonPath("$.content.[?(@.key==test123)]$.._links.optionalmodules.href", contains(
                        "http://localhost/rest/v1/distributionsettypes/" + testType.getId() + "/optionalmoduletypes")))
                .andExpect(jsonPath("$.total", equalTo(4)));
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/distributionsettypes GET requests with sorting by KEY.")
    public void getDistributionSetTypesSortedByKey() throws Exception {

        DistributionSetType testType = distributionSetManagement.createDistributionSetType(
                entityFactory.generateDistributionSetType("zzzzz", "TestName123", "Desc123"));
        testType = distributionSetManagement.updateDistributionSetType(testType.getId(), "Desc1234", null);

        // descending
        mvc.perform(get("/rest/v1/distributionsettypes").accept(MediaType.APPLICATION_JSON)
                .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "KEY:DESC")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.[0].id", equalTo(testType.getId().intValue())))
                .andExpect(jsonPath("$.content.[0].name", equalTo("TestName123")))
                .andExpect(jsonPath("$.content.[0].description", equalTo("Desc1234")))
                .andExpect(jsonPath("$.content.[0].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("$.content.[0].createdAt", equalTo(testType.getCreatedAt())))
                .andExpect(jsonPath("$.content.[0].lastModifiedBy", equalTo("uploadTester")))
                .andExpect(jsonPath("$.content.[0].lastModifiedAt", equalTo(testType.getLastModifiedAt())))
                .andExpect(jsonPath("$.content.[0].key", equalTo("zzzzz")))
                .andExpect(jsonPath("$.total", equalTo(DEFAULT_DS_TYPES + 1)));

        // ascending
        mvc.perform(get("/rest/v1/distributionsettypes").accept(MediaType.APPLICATION_JSON)
                .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "KEY:ASC")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.[3].id", equalTo(testType.getId().intValue())))
                .andExpect(jsonPath("$.content.[3].name", equalTo("TestName123")))
                .andExpect(jsonPath("$.content.[3].description", equalTo("Desc1234")))
                .andExpect(jsonPath("$.content.[3].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("$.content.[3].createdAt", equalTo(testType.getCreatedAt())))
                .andExpect(jsonPath("$.content.[3].lastModifiedBy", equalTo("uploadTester")))
                .andExpect(jsonPath("$.content.[3].lastModifiedAt", equalTo(testType.getLastModifiedAt())))
                .andExpect(jsonPath("$.content.[3].key", equalTo("zzzzz")))
                .andExpect(jsonPath("$.total", equalTo(DEFAULT_DS_TYPES + 1)));
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/distributionsettypes POST requests.")
    public void createDistributionSetTypes() throws JSONException, Exception {

        assertThat(distributionSetManagement.countDistributionSetTypesAll()).isEqualTo(DEFAULT_DS_TYPES);

        final List<DistributionSetType> types = new ArrayList<>();
        types.add(entityFactory.generateDistributionSetType("test1", "TestName1", "Desc1", Lists.newArrayList(osType),
                Lists.newArrayList(runtimeType)));
        types.add(entityFactory.generateDistributionSetType("test2", "TestName2", "Desc2", Collections.emptyList(),
                Lists.newArrayList(osType, runtimeType, appType)));
        types.add(entityFactory.generateDistributionSetType("test3", "TestName3", "Desc3", Lists.newArrayList(osType),
                Lists.newArrayList(runtimeType)));

        final MvcResult mvcResult = mvc
                .perform(post("/rest/v1/distributionsettypes/").content(JsonBuilder.distributionSetTypes(types))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("[0].name", equalTo("TestName1"))).andExpect(jsonPath("[0].key", equalTo("test1")))
                .andExpect(jsonPath("[0].description", equalTo("Desc1")))
                .andExpect(jsonPath("[0].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[1].name", equalTo("TestName2"))).andExpect(jsonPath("[1].key", equalTo("test2")))
                .andExpect(jsonPath("[1].description", equalTo("Desc2")))
                .andExpect(jsonPath("[1].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[2].name", equalTo("TestName3"))).andExpect(jsonPath("[2].key", equalTo("test3")))
                .andExpect(jsonPath("[2].description", equalTo("Desc3")))
                .andExpect(jsonPath("[2].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[2].createdAt", not(equalTo(0)))).andReturn();

        final DistributionSetType created1 = distributionSetManagement.findDistributionSetTypeByKey("test1");
        final DistributionSetType created2 = distributionSetManagement.findDistributionSetTypeByKey("test2");
        final DistributionSetType created3 = distributionSetManagement.findDistributionSetTypeByKey("test3");

        assertThat(created1.getMandatoryModuleTypes()).containsOnly(osType);
        assertThat(created1.getOptionalModuleTypes()).containsOnly(runtimeType);
        assertThat(created2.getOptionalModuleTypes()).containsOnly(osType, runtimeType, appType);
        assertThat(created3.getMandatoryModuleTypes()).containsOnly(osType, runtimeType);

        assertThat(
                JsonPath.compile("[0]_links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                        .isEqualTo("http://localhost/rest/v1/distributionsettypes/" + created1.getId());
        assertThat(
                JsonPath.compile("[1]_links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                        .isEqualTo("http://localhost/rest/v1/distributionsettypes/" + created2.getId());
        assertThat(
                JsonPath.compile("[2]_links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                        .isEqualTo("http://localhost/rest/v1/distributionsettypes/" + created3.getId());

        assertThat(JsonPath.compile("[0]_links.mandatorymodules.href")
                .read(mvcResult.getResponse().getContentAsString()).toString()).isEqualTo(
                        "http://localhost/rest/v1/distributionsettypes/" + created1.getId() + "/mandatorymoduletypes");
        assertThat(JsonPath.compile("[1]_links.mandatorymodules.href")
                .read(mvcResult.getResponse().getContentAsString()).toString()).isEqualTo(
                        "http://localhost/rest/v1/distributionsettypes/" + created2.getId() + "/mandatorymoduletypes");
        assertThat(JsonPath.compile("[2]_links.mandatorymodules.href")
                .read(mvcResult.getResponse().getContentAsString()).toString()).isEqualTo(
                        "http://localhost/rest/v1/distributionsettypes/" + created3.getId() + "/mandatorymoduletypes");

        assertThat(JsonPath.compile("[0]_links.optionalmodules.href").read(mvcResult.getResponse().getContentAsString())
                .toString()).isEqualTo(
                        "http://localhost/rest/v1/distributionsettypes/" + created1.getId() + "/optionalmoduletypes");
        assertThat(JsonPath.compile("[1]_links.optionalmodules.href").read(mvcResult.getResponse().getContentAsString())
                .toString()).isEqualTo(
                        "http://localhost/rest/v1/distributionsettypes/" + created2.getId() + "/optionalmoduletypes");
        assertThat(JsonPath.compile("[2]_links.optionalmodules.href").read(mvcResult.getResponse().getContentAsString())
                .toString()).isEqualTo(
                        "http://localhost/rest/v1/distributionsettypes/" + created3.getId() + "/optionalmoduletypes");

        assertThat(distributionSetManagement.countDistributionSetTypesAll()).isEqualTo(6);
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/distributionsettypes/{ID}/mandatorymoduletypes POST requests.")
    public void addMandatoryModuleToDistributionSetType() throws JSONException, Exception {
        DistributionSetType testType = distributionSetManagement.createDistributionSetType(
                entityFactory.generateDistributionSetType("test123", "TestName123", "Desc123"));
        assertThat(testType.getOptLockRevision()).isEqualTo(1);

        mvc.perform(post("/rest/v1/distributionsettypes/{dstID}/mandatorymoduletypes", testType.getId())
                .content("{\"id\":" + osType.getId() + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        testType = distributionSetManagement.findDistributionSetTypeById(testType.getId());
        assertThat(testType.getLastModifiedBy()).isEqualTo("uploadTester");
        assertThat(testType.getOptLockRevision()).isEqualTo(2);
        assertThat(testType.getMandatoryModuleTypes()).containsExactly(osType);
        assertThat(testType.getOptionalModuleTypes()).isEmpty();
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/distributionsettypes/{ID}/optionalmoduletypes POST requests.")
    public void addOptionalModuleToDistributionSetType() throws JSONException, Exception {
        DistributionSetType testType = distributionSetManagement.createDistributionSetType(
                entityFactory.generateDistributionSetType("test123", "TestName123", "Desc123"));
        assertThat(testType.getOptLockRevision()).isEqualTo(1);

        mvc.perform(post("/rest/v1/distributionsettypes/{dstID}/optionalmoduletypes", testType.getId())
                .content("{\"id\":" + osType.getId() + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        testType = distributionSetManagement.findDistributionSetTypeById(testType.getId());
        assertThat(testType.getLastModifiedBy()).isEqualTo("uploadTester");
        assertThat(testType.getOptLockRevision()).isEqualTo(2);
        assertThat(testType.getOptionalModuleTypes()).containsExactly(osType);
        assertThat(testType.getMandatoryModuleTypes()).isEmpty();

    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/distributionsettypes/{ID}/mandatorymoduletypes GET requests.")
    public void getMandatoryModulesOfDistributionSetType() throws JSONException, Exception {
        final DistributionSetType testType = generateTestType();

        mvc.perform(get("/rest/v1/distributionsettypes/{dstID}/mandatorymoduletypes", testType.getId())
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("[0].name", equalTo(osType.getName())))
                .andExpect(jsonPath("[0].description", equalTo(osType.getDescription())))
                .andExpect(jsonPath("[0].maxAssignments", equalTo(1))).andExpect(jsonPath("[0].key", equalTo("os")));
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/distributionsettypes/{ID}/optionalmoduletypes GET requests.")
    public void getOptionalModulesOfDistributionSetType() throws JSONException, Exception {
        final DistributionSetType testType = generateTestType();

        mvc.perform(get("/rest/v1/distributionsettypes/{dstID}/optionalmoduletypes", testType.getId())
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("[0].name", equalTo(appType.getName())))
                .andExpect(jsonPath("[0].description", equalTo(appType.getDescription())))
                .andExpect(jsonPath("[0].maxAssignments", equalTo(Integer.MAX_VALUE)))
                .andExpect(jsonPath("[0].key", equalTo("application")));
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/distributionsettypes/{ID}/mandatorymoduletypes/{ID} GET requests.")
    public void getMandatoryModuleOfDistributionSetType() throws JSONException, Exception {
        final DistributionSetType testType = generateTestType();

        mvc.perform(get("/rest/v1/distributionsettypes/{dstID}/mandatorymoduletypes/{smtId}", testType.getId(),
                osType.getId()).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("$.id", equalTo(osType.getId().intValue())))
                .andExpect(jsonPath("$.name", equalTo(osType.getName())))
                .andExpect(jsonPath("$.description", equalTo(osType.getDescription())))
                .andExpect(jsonPath("$.createdBy", equalTo(osType.getCreatedBy())))
                .andExpect(jsonPath("$.createdAt", equalTo(osType.getCreatedAt())))
                .andExpect(jsonPath("$.lastModifiedBy", equalTo(osType.getLastModifiedBy())))
                .andExpect(jsonPath("$.lastModifiedAt", equalTo(osType.getLastModifiedAt())));
    }

    private DistributionSetType generateTestType() {
        final DistributionSetType testType = distributionSetManagement
                .createDistributionSetType(entityFactory.generateDistributionSetType("test123", "TestName123",
                        "Desc123", Lists.newArrayList(osType), Lists.newArrayList(appType)));
        assertThat(testType.getOptLockRevision()).isEqualTo(1);
        assertThat(testType.getOptionalModuleTypes()).containsExactly(appType);
        assertThat(testType.getMandatoryModuleTypes()).containsExactly(osType);
        return testType;
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/distributionsettypes/{ID}/optionalmoduletypes/{ID} GET requests.")
    public void getOptionalModuleOfDistributionSetType() throws JSONException, Exception {
        final DistributionSetType testType = generateTestType();

        mvc.perform(get("/rest/v1/distributionsettypes/{dstID}/optionalmoduletypes/{smtId}", testType.getId(),
                appType.getId()).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("$.id", equalTo(appType.getId().intValue())))
                .andExpect(jsonPath("$.name", equalTo(appType.getName())))
                .andExpect(jsonPath("$.description", equalTo(appType.getDescription())))
                .andExpect(jsonPath("$.createdBy", equalTo(appType.getCreatedBy())))
                .andExpect(jsonPath("$.createdAt", equalTo(appType.getCreatedAt())))
                .andExpect(jsonPath("$.lastModifiedBy", equalTo(appType.getLastModifiedBy())))
                .andExpect(jsonPath("$.lastModifiedAt", equalTo(appType.getLastModifiedAt())));
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/distributionsettypes/{ID}/mandatorymoduletypes/{ID} DELETE requests.")
    public void removeMandatoryModuleToDistributionSetType() throws JSONException, Exception {
        DistributionSetType testType = generateTestType();

        mvc.perform(delete("/rest/v1/distributionsettypes/{dstID}/mandatorymoduletypes/{smtId}", testType.getId(),
                osType.getId()).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        testType = distributionSetManagement.findDistributionSetTypeById(testType.getId());
        assertThat(testType.getLastModifiedBy()).isEqualTo("uploadTester");
        assertThat(testType.getOptLockRevision()).isEqualTo(2);
        assertThat(testType.getOptionalModuleTypes()).containsExactly(appType);
        assertThat(testType.getMandatoryModuleTypes()).isEmpty();
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/distributionsettypes/{ID}/optionalmoduletypes/{ID} DELETE requests.")
    public void removeOptionalModuleToDistributionSetType() throws JSONException, Exception {
        DistributionSetType testType = generateTestType();

        mvc.perform(delete("/rest/v1/distributionsettypes/{dstID}/optionalmoduletypes/{smtId}", testType.getId(),
                appType.getId()).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        testType = distributionSetManagement.findDistributionSetTypeById(testType.getId());
        assertThat(testType.getLastModifiedBy()).isEqualTo("uploadTester");
        assertThat(testType.getOptLockRevision()).isEqualTo(2);
        assertThat(testType.getOptionalModuleTypes()).isEmpty();
        assertThat(testType.getMandatoryModuleTypes()).containsExactly(osType);
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/distributionsettypes/{ID} GET requests.")
    public void getDistributionSetType() throws Exception {

        DistributionSetType testType = distributionSetManagement.createDistributionSetType(
                entityFactory.generateDistributionSetType("test123", "TestName123", "Desc123"));
        testType = distributionSetManagement.updateDistributionSetType(testType.getId(), "Desc1234", null);

        mvc.perform(get("/rest/v1/distributionsettypes/{dstId}", testType.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", equalTo("TestName123")))
                .andExpect(jsonPath("$.description", equalTo("Desc1234")))
                .andExpect(jsonPath("$.createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("$.createdAt", equalTo(testType.getCreatedAt())))
                .andExpect(jsonPath("$.lastModifiedBy", equalTo("uploadTester")));
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/DistributionSetTypes/{ID} DELETE requests (hard delete scenario).")
    public void deleteDistributionSetTypeUnused() throws Exception {
        final DistributionSetType testType = distributionSetManagement.createDistributionSetType(
                entityFactory.generateDistributionSetType("test123", "TestName123", "Desc123"));

        assertThat(distributionSetManagement.countDistributionSetTypesAll()).isEqualTo(DEFAULT_DS_TYPES + 1);

        mvc.perform(delete("/rest/v1/distributionsettypes/{dsId}", testType.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        assertThat(distributionSetManagement.countDistributionSetTypesAll()).isEqualTo(DEFAULT_DS_TYPES);
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/DistributionSetTypes/{ID} DELETE requests (soft delete scenario).")
    public void deleteDistributionSetTypeUsed() throws Exception {
        final DistributionSetType testType = distributionSetManagement.createDistributionSetType(
                entityFactory.generateDistributionSetType("test123", "TestName123", "Desc123"));
        distributionSetManagement.createDistributionSet(
                entityFactory.generateDistributionSet("sdfsd", "dsfsdf", "sdfsdf", testType, null, false));

        assertThat(distributionSetManagement.countDistributionSetTypesAll()).isEqualTo(DEFAULT_DS_TYPES + 1);
        assertThat(distributionSetManagement.countDistributionSetsAll()).isEqualTo(1);

        mvc.perform(delete("/rest/v1/distributionsettypes/{smId}", testType.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        assertThat(distributionSetManagement.countDistributionSetsAll()).isEqualTo(1);
        assertThat(distributionSetManagement.countDistributionSetTypesAll()).isEqualTo(DEFAULT_DS_TYPES);
    }

    @Test
    @Description("Checks the correct behaviour of /rest/v1/distributionsettypes/{ID} PUT requests.")
    public void updateDistributionSetTypeOnlyDescriptionAndNameUntouched() throws Exception {
        final DistributionSetType testType = distributionSetManagement.createDistributionSetType(
                entityFactory.generateDistributionSetType("test123", "TestName123", "Desc123"));

        final String body = new JSONObject().put("id", testType.getId()).put("description", "foobardesc")
                .put("name", "nameShouldNotBeChanged").toString();

        mvc.perform(put("/rest/v1/distributionsettypes/{smId}", testType.getId()).content(body)
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(testType.getId().intValue())))
                .andExpect(jsonPath("$.description", equalTo("foobardesc")))
                .andExpect(jsonPath("$.name", equalTo("TestName123"))).andReturn();
    }

    @Test
    @Description("Checks the correct behaviour of /rest/v1/distributionsettypes GET requests with paging.")
    public void getDistributionSetTypesWithoutAddtionalRequestParameters() throws Exception {

        // 3 types overall (2 hawkbit tenant default, 1 test default
        final int types = 3;
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(types)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(types)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(types)));
    }

    @Test
    @Description("Checks the correct behaviour of /rest/v1/distributionsettypes GET requests with paging.")
    public void getDistributionSetTypesWithPagingLimitRequestParameter() throws Exception {

        final int types = DEFAULT_DS_TYPES;
        final int limitSize = 1;
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING)
                .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(limitSize)))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(types)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(limitSize)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(limitSize)));
    }

    @Test
    @Description("Checks the correct behaviour of /rest/v1/distributionsettypes GET requests with paging.")
    public void getDistributionSetTypesWithPagingLimitAndOffsetRequestParameter() throws Exception {

        final int types = DEFAULT_DS_TYPES;
        final int offsetParam = 2;
        final int expectedSize = types - offsetParam;
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING)
                .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(offsetParam))
                .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(types)))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(types)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(expectedSize)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(expectedSize)));
    }

    @Test
    @Description("Ensures that the server is behaving as expected on invalid requests (wrong media type, wrong ID etc.).")
    public void invalidRequestsOnDistributionSetTypesResource() throws Exception {
        final DistributionSetType testType = distributionSetManagement.createDistributionSetType(
                entityFactory.generateDistributionSetType("test123", "TestName123", "Desc123"));

        final SoftwareModuleType testSmType = softwareManagement.createSoftwareModuleType(
                entityFactory.generateSoftwareModuleType("test123", "TestName123", "Desc123", "col123", 5));

        // DST does not exist
        mvc.perform(get("/rest/v1/distributionsettypes/12345678")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
        mvc.perform(get("/rest/v1/distributionsettypes/12345678/mandatorymoduletypes"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());
        mvc.perform(get("/rest/v1/distributionsettypes/12345678/optionalmoduletypes"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        mvc.perform(delete("/rest/v1/distributionsettypes/12345678")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // Module types incorrect
        mvc.perform(get("/rest/v1/distributionsettypes/" + standardDsType.getId() + "/mandatorymoduletypes"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        mvc.perform(get("/rest/v1/distributionsettypes/" + standardDsType.getId() + "/mandatorymoduletypes/565765656"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());
        mvc.perform(get("/rest/v1/distributionsettypes/" + standardDsType.getId() + "/optionalmoduletypes/565765656"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        mvc.perform(get(
                "/rest/v1/distributionsettypes/" + standardDsType.getId() + "/mandatorymoduletypes/" + osType.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        mvc.perform(get("/rest/v1/distributionsettypes/" + standardDsType.getId() + "/mandatorymoduletypes/"
                + testSmType.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        mvc.perform(get(
                "/rest/v1/distributionsettypes/" + standardDsType.getId() + "/optionalmoduletypes/" + osType.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        mvc.perform(get(
                "/rest/v1/distributionsettypes/" + standardDsType.getId() + "/optionalmoduletypes/" + appType.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        mvc.perform(get("/rest/v1/distributionsettypes/" + standardDsType.getId() + "/optionalmoduletypes/"
                + testSmType.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // Modules types at creation time invalid

        final DistributionSetType testNewType = entityFactory.generateDistributionSetType("test123", "TestName123",
                "Desc123",
                Lists.newArrayList(
                        entityFactory.generateSoftwareModuleType("foo", "bar", "test", "col", Integer.MAX_VALUE)),
                Collections.emptyList());

        mvc.perform(post("/rest/v1/distributionsettypes")
                .content(JsonBuilder.distributionSetTypes(Lists.newArrayList(testNewType)))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isUnsupportedMediaType());

        // bad request - no content
        mvc.perform(post("/rest/v1/distributionsettypes").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        // bad request - bad content
        mvc.perform(post("/rest/v1/distributionsettypes").content("sdfjsdlkjfskdjf".getBytes())
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        // Missing mandatory field name
        mvc.perform(post("/rest/v1/distributionsettypes").content("[{\"description\":\"Desc123\",\"key\":\"test123\"}]")
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        final DistributionSetType toLongName = entityFactory.generateDistributionSetType("test123",
                RandomStringUtils.randomAscii(80), "Desc123");
        mvc.perform(post("/rest/v1/distributionsettypes")
                .content(JsonBuilder.distributionSetTypes(Lists.newArrayList(toLongName)))
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        // not allowed methods
        mvc.perform(put("/rest/v1/distributionsettypes")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(delete("/rest/v1/distributionsettypes")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(put("/rest/v1/distributionsettypes/" + standardDsType.getId() + "/mandatorymoduletypes"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());
        mvc.perform(put(
                "/rest/v1/distributionsettypes/" + standardDsType.getId() + "/optionalmoduletypes/" + osType.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());
    }

    @Test
    @Description("Search erquest of software module types.")
    public void searchDistributionSetTypeRsql() throws Exception {
        final DistributionSetType testType = distributionSetManagement.createDistributionSetType(
                entityFactory.generateDistributionSetType("test123", "TestName123", "Desc123"));
        final DistributionSetType testType2 = distributionSetManagement.createDistributionSetType(
                entityFactory.generateDistributionSetType("test1234", "TestName1234", "Desc123"));

        final String rsqlFindLikeDs1OrDs2 = "name==TestName123,name==TestName1234";

        mvc.perform(get("/rest/v1/distributionsettypes?q=" + rsqlFindLikeDs1OrDs2)).andDo(MockMvcResultPrinter.print())
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
