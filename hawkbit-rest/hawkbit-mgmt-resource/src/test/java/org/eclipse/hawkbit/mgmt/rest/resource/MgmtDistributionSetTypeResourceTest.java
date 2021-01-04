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

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.util.Lists;
import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleTypeCreate;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;

/**
 * Test for {@link MgmtDistributionSetTypeResource}.
 *
 */
@Feature("Component Tests - Management API")
@Story("Distribution Set Type Resource")
public class MgmtDistributionSetTypeResourceTest extends AbstractManagementApiIntegrationTest {

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/distributionsettypes GET requests.")
    public void getDistributionSetTypes() throws Exception {

        DistributionSetType testType = distributionSetTypeManagement.create(entityFactory.distributionSetType().create()
                .key("test123").name("TestName123").description("Desc123").colour("col12"));
        testType = distributionSetTypeManagement
                .update(entityFactory.distributionSetType().update(testType.getId()).description("Desc1234"));

        // 4 types overall (2 hawkbit tenant default, 1 test default and 1
        // generated in this test)
        mvc.perform(get("/rest/v1/distributionsettypes").accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content.[?(@.key=='" + standardDsType.getKey() + "')].name",
                        contains(standardDsType.getName())))
                .andExpect(jsonPath("$.content.[?(@.key=='" + standardDsType.getKey() + "')].description",
                        contains(standardDsType.getDescription())))
                .andExpect(jsonPath("$.content.[?(@.key=='" + standardDsType.getKey() + "')].key",
                        contains(standardDsType.getKey())))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].id", contains(testType.getId().intValue())))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].name", contains("TestName123")))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].description", contains("Desc1234")))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].createdBy", contains("uploadTester")))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].createdAt", contains(testType.getCreatedAt())))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].lastModifiedBy", contains("uploadTester")))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].lastModifiedAt",
                        contains(testType.getLastModifiedAt())))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].key", contains("test123")))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')]._links.self.href",
                        contains("http://localhost/rest/v1/distributionsettypes/" + testType.getId())))
                .andExpect(jsonPath("$.total", equalTo(5)));
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/distributionsettypes GET requests with sorting by KEY.")
    public void getDistributionSetTypesSortedByKey() throws Exception {

        DistributionSetType testType = distributionSetTypeManagement.create(entityFactory.distributionSetType().create()
                .key("zzzzz").name("TestName123").description("Desc123").colour("col12"));
        testType = distributionSetTypeManagement
                .update(entityFactory.distributionSetType().update(testType.getId()).description("Desc1234"));

        // descending
        mvc.perform(get("/rest/v1/distributionsettypes").accept(MediaType.APPLICATION_JSON)
                .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "KEY:DESC")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
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
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content.[4].id", equalTo(testType.getId().intValue())))
                .andExpect(jsonPath("$.content.[4].name", equalTo("TestName123")))
                .andExpect(jsonPath("$.content.[4].description", equalTo("Desc1234")))
                .andExpect(jsonPath("$.content.[4].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("$.content.[4].createdAt", equalTo(testType.getCreatedAt())))
                .andExpect(jsonPath("$.content.[4].lastModifiedBy", equalTo("uploadTester")))
                .andExpect(jsonPath("$.content.[4].lastModifiedAt", equalTo(testType.getLastModifiedAt())))
                .andExpect(jsonPath("$.content.[4].key", equalTo("zzzzz")))
                .andExpect(jsonPath("$.total", equalTo(DEFAULT_DS_TYPES + 1)));
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/distributionsettypes POST requests.")
    public void createDistributionSetTypes() throws Exception {

        final List<DistributionSetType> types = createTestDistributionSetTestTypes();

        final MvcResult mvcResult = runPostDistributionSetType(types);

        verifyCreatedDistributionSetTypes(mvcResult);
    }

    @Step
    private void verifyCreatedDistributionSetTypes(final MvcResult mvcResult) throws UnsupportedEncodingException {
        final DistributionSetType created1 = distributionSetTypeManagement.getByKey("testKey1").get();
        final DistributionSetType created2 = distributionSetTypeManagement.getByKey("testKey2").get();
        final DistributionSetType created3 = distributionSetTypeManagement.getByKey("testKey3").get();

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

        assertThat(distributionSetTypeManagement.count()).isEqualTo(7);
    }

    @Step
    private MvcResult runPostDistributionSetType(final List<DistributionSetType> types) throws Exception {
        return mvc
                .perform(post("/rest/v1/distributionsettypes/").content(JsonBuilder.distributionSetTypes(types))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("[0].name", equalTo("TestName1")))
                .andExpect(jsonPath("[0].key", equalTo("testKey1")))
                .andExpect(jsonPath("[0].description", equalTo("Desc1")))
                .andExpect(jsonPath("[0].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[1].name", equalTo("TestName2")))
                .andExpect(jsonPath("[1].key", equalTo("testKey2")))
                .andExpect(jsonPath("[1].description", equalTo("Desc2")))
                .andExpect(jsonPath("[1].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[2].name", equalTo("TestName3")))
                .andExpect(jsonPath("[2].key", equalTo("testKey3")))
                .andExpect(jsonPath("[2].description", equalTo("Desc3")))
                .andExpect(jsonPath("[2].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[2].createdAt", not(equalTo(0)))).andReturn();
    }

    @Step
    private List<DistributionSetType> createTestDistributionSetTestTypes() {
        assertThat(distributionSetTypeManagement.count()).isEqualTo(DEFAULT_DS_TYPES);

        return Arrays.asList(
                entityFactory.distributionSetType().create().key("testKey1").name("TestName1").description("Desc1")
                        .colour("col").mandatory(Arrays.asList(osType.getId()))
                        .optional(Arrays.asList(runtimeType.getId())).build(),
                entityFactory.distributionSetType().create().key("testKey2").name("TestName2").description("Desc2")
                        .colour("col").optional(Arrays.asList(runtimeType.getId(), osType.getId(), appType.getId()))
                        .build(),
                entityFactory.distributionSetType().create().key("testKey3").name("TestName3").description("Desc3")
                        .colour("col").mandatory(Arrays.asList(runtimeType.getId(), osType.getId())).build());
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/distributionsettypes/{ID}/mandatorymoduletypes POST requests.")
    public void addMandatoryModuleToDistributionSetType() throws Exception {
        DistributionSetType testType = distributionSetTypeManagement.create(entityFactory.distributionSetType().create()
                .key("test123").name("TestName123").description("Desc123").colour("col12"));
        assertThat(testType.getOptLockRevision()).isEqualTo(1);

        mvc.perform(post("/rest/v1/distributionsettypes/{dstID}/mandatorymoduletypes", testType.getId())
                .content("{\"id\":" + osType.getId() + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        testType = distributionSetTypeManagement.get(testType.getId()).get();
        assertThat(testType.getLastModifiedBy()).isEqualTo("uploadTester");
        assertThat(testType.getOptLockRevision()).isEqualTo(2);
        assertThat(testType.getMandatoryModuleTypes()).containsExactly(osType);
        assertThat(testType.getOptionalModuleTypes()).isEmpty();
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/distributionsettypes/{ID}/optionalmoduletypes POST requests.")
    public void addOptionalModuleToDistributionSetType() throws Exception {
        DistributionSetType testType = distributionSetTypeManagement.create(entityFactory.distributionSetType().create()
                .key("test123").name("TestName123").description("Desc123").colour("col12"));
        assertThat(testType.getOptLockRevision()).isEqualTo(1);

        mvc.perform(post("/rest/v1/distributionsettypes/{dstID}/optionalmoduletypes", testType.getId())
                .content("{\"id\":" + osType.getId() + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        testType = distributionSetTypeManagement.get(testType.getId()).get();
        assertThat(testType.getLastModifiedBy()).isEqualTo("uploadTester");
        assertThat(testType.getOptLockRevision()).isEqualTo(2);
        assertThat(testType.getOptionalModuleTypes()).containsExactly(osType);
        assertThat(testType.getMandatoryModuleTypes()).isEmpty();

    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Verifies quota enforcement for /rest/v1/distributionsettypes/{ID}/optionalmoduletypes POST requests.")
    public void assignModuleTypesToDistributionSetTypeUntilQuotaExceeded() throws Exception {

        // create software module types
        final int maxSoftwareModuleTypes = quotaManagement.getMaxSoftwareModuleTypesPerDistributionSetType();
        final List<Long> moduleTypeIds = Lists.newArrayList();
        for (int i = 0; i < maxSoftwareModuleTypes + 1; ++i) {
            final SoftwareModuleTypeCreate smCreate = entityFactory.softwareModuleType().create().name("smType_" + i)
                    .description("smType_" + i).maxAssignments(1).colour("blue").key("smType_" + i);
            moduleTypeIds.add(softwareModuleTypeManagement.create(smCreate).getId());
        }

        // verify quota enforcement for optional module types

        final DistributionSetType testType = distributionSetTypeManagement.create(entityFactory.distributionSetType()
                .create().key("testType").name("testType").description("testType").colour("col12"));
        assertThat(testType.getOptLockRevision()).isEqualTo(1);

        for (int i = 0; i < moduleTypeIds.size() - 1; ++i) {
            mvc.perform(post("/rest/v1/distributionsettypes/{dstID}/optionalmoduletypes", testType.getId())
                    .content("{\"id\":" + moduleTypeIds.get(i) + "}").contentType(MediaType.APPLICATION_JSON))
                    .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        }

        mvc.perform(post("/rest/v1/distributionsettypes/{dstID}/optionalmoduletypes", testType.getId())
                .content("{\"id\":" + moduleTypeIds.get(moduleTypeIds.size() - 1) + "}")
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.exceptionClass", equalTo(AssignmentQuotaExceededException.class.getName())))
                .andExpect(jsonPath("$.errorCode", equalTo(SpServerError.SP_QUOTA_EXCEEDED.getKey())));

        // verify quota enforcement for mandatory module types

        final DistributionSetType testType2 = distributionSetTypeManagement.create(entityFactory.distributionSetType()
                .create().key("testType2").name("testType2").description("testType2").colour("col12"));
        assertThat(testType2.getOptLockRevision()).isEqualTo(1);

        for (int i = 0; i < moduleTypeIds.size() - 1; ++i) {
            mvc.perform(post("/rest/v1/distributionsettypes/{dstID}/mandatorymoduletypes", testType2.getId())
                    .content("{\"id\":" + moduleTypeIds.get(i) + "}").contentType(MediaType.APPLICATION_JSON))
                    .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        }

        mvc.perform(post("/rest/v1/distributionsettypes/{dstID}/mandatorymoduletypes", testType2.getId())
                .content("{\"id\":" + moduleTypeIds.get(moduleTypeIds.size() - 1) + "}")
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.exceptionClass", equalTo(AssignmentQuotaExceededException.class.getName())))
                .andExpect(jsonPath("$.errorCode", equalTo(SpServerError.SP_QUOTA_EXCEEDED.getKey())));

    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/distributionsettypes/{ID}/mandatorymoduletypes GET requests.")
    public void getMandatoryModulesOfDistributionSetType() throws Exception {
        final DistributionSetType testType = generateTestType();

        mvc.perform(get("/rest/v1/distributionsettypes/{dstID}/mandatorymoduletypes", testType.getId())
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("[0].name", equalTo(osType.getName())))
                .andExpect(jsonPath("[0].description", equalTo(osType.getDescription())))
                .andExpect(jsonPath("[0].maxAssignments", equalTo(1))).andExpect(jsonPath("[0].key", equalTo("os")));
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/distributionsettypes/{ID}/optionalmoduletypes GET requests.")
    public void getOptionalModulesOfDistributionSetType() throws Exception {
        final DistributionSetType testType = generateTestType();

        mvc.perform(get("/rest/v1/distributionsettypes/{dstID}/optionalmoduletypes", testType.getId())
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("[0].name", equalTo(appType.getName())))
                .andExpect(jsonPath("[0].description", equalTo(appType.getDescription())))
                .andExpect(jsonPath("[0].maxAssignments", equalTo(Integer.MAX_VALUE)))
                .andExpect(jsonPath("[0].key", equalTo("application")));
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/distributionsettypes/{ID}/mandatorymoduletypes/{ID} GET requests.")
    public void getMandatoryModuleOfDistributionSetType() throws Exception {
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
        final DistributionSetType testType = distributionSetTypeManagement.create(entityFactory.distributionSetType()
                .create().key("test123").name("TestName123").description("Desc123").colour("col")
                .mandatory(Arrays.asList(osType.getId())).optional(Arrays.asList(appType.getId())));
        assertThat(testType.getOptLockRevision()).isEqualTo(1);
        assertThat(testType.getOptionalModuleTypes()).containsExactly(appType);
        assertThat(testType.getMandatoryModuleTypes()).containsExactly(osType);
        return testType;
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/distributionsettypes/{ID}/optionalmoduletypes/{ID} GET requests.")
    public void getOptionalModuleOfDistributionSetType() throws Exception {
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
    public void removeMandatoryModuleToDistributionSetType() throws Exception {
        DistributionSetType testType = generateTestType();

        mvc.perform(delete("/rest/v1/distributionsettypes/{dstID}/mandatorymoduletypes/{smtId}", testType.getId(),
                osType.getId()).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        testType = distributionSetTypeManagement.get(testType.getId()).get();
        assertThat(testType.getLastModifiedBy()).isEqualTo("uploadTester");
        assertThat(testType.getOptLockRevision()).isEqualTo(2);
        assertThat(testType.getOptionalModuleTypes()).containsExactly(appType);
        assertThat(testType.getMandatoryModuleTypes()).isEmpty();
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/distributionsettypes/{ID}/optionalmoduletypes/{ID} DELETE requests.")
    public void removeOptionalModuleToDistributionSetType() throws Exception {
        DistributionSetType testType = generateTestType();

        mvc.perform(delete("/rest/v1/distributionsettypes/{dstID}/optionalmoduletypes/{smtId}", testType.getId(),
                appType.getId()).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        testType = distributionSetTypeManagement.get(testType.getId()).get();
        assertThat(testType.getLastModifiedBy()).isEqualTo("uploadTester");
        assertThat(testType.getOptLockRevision()).isEqualTo(2);
        assertThat(testType.getOptionalModuleTypes()).isEmpty();
        assertThat(testType.getMandatoryModuleTypes()).containsExactly(osType);
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/distributionsettypes/{ID} GET requests.")
    public void getDistributionSetType() throws Exception {

        DistributionSetType testType = distributionSetTypeManagement.create(entityFactory.distributionSetType().create()
                .key("test123").name("TestName123").description("Desc123").colour("col12"));
        testType = distributionSetTypeManagement
                .update(entityFactory.distributionSetType().update(testType.getId()).description("Desc1234"));

        mvc.perform(get("/rest/v1/distributionsettypes/{dstId}", testType.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.name", equalTo("TestName123")))
                .andExpect(jsonPath("$.description", equalTo("Desc1234")))
                .andExpect(jsonPath("$.createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("$.createdAt", equalTo(testType.getCreatedAt())))
                .andExpect(jsonPath("$.lastModifiedBy", equalTo("uploadTester")))
                .andExpect(jsonPath("$.deleted", equalTo(testType.isDeleted())));
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/DistributionSetTypes/{ID} DELETE requests (hard delete scenario).")
    public void deleteDistributionSetTypeUnused() throws Exception {
        final DistributionSetType testType = distributionSetTypeManagement.create(entityFactory.distributionSetType()
                .create().key("test123").name("TestName123").description("Desc123").colour("col12"));

        assertThat(distributionSetTypeManagement.count()).isEqualTo(DEFAULT_DS_TYPES + 1);

        mvc.perform(delete("/rest/v1/distributionsettypes/{dsId}", testType.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        assertThat(distributionSetTypeManagement.count()).isEqualTo(DEFAULT_DS_TYPES);
    }

    @Test
    @Description("Ensures that DS type deletion request to API on an entity that does not exist results in NOT_FOUND.")
    public void deleteDistributionSetTypeThatDoesNotExistLeadsToNotFound() throws Exception {
        mvc.perform(delete("/rest/v1/distributionsettypes/1234")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/DistributionSetTypes/{ID} DELETE requests (soft delete scenario).")
    public void deleteDistributionSetTypeUsed() throws Exception {
        final DistributionSetType testType = distributionSetTypeManagement.create(entityFactory.distributionSetType()
                .create().key("test123").name("TestName123").description("Desc123").colour("col12"));

        distributionSetManagement.create(entityFactory.distributionSet().create().name("sdfsd").description("dsfsdf")
                .version("1").type(testType));

        assertThat(distributionSetTypeManagement.count()).isEqualTo(DEFAULT_DS_TYPES + 1);
        assertThat(distributionSetManagement.count()).isEqualTo(1);

        mvc.perform(get("/rest/v1/distributionsettypes/{dstId}", testType.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("$.deleted", equalTo(false)));

        mvc.perform(delete("/rest/v1/distributionsettypes/{dstId}", testType.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        mvc.perform(get("/rest/v1/distributionsettypes/{dstId}", testType.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("$.deleted", equalTo(true)));

        assertThat(distributionSetManagement.count()).isEqualTo(1);
        assertThat(distributionSetTypeManagement.count()).isEqualTo(DEFAULT_DS_TYPES);
    }

    @Test
    @Description("Checks the correct behaviour of /rest/v1/distributionsettypes/{ID} PUT requests.")
    public void updateDistributionSetTypeOnlyDescriptionAndNameUntouched() throws Exception {
        final DistributionSetType testType = distributionSetTypeManagement.create(entityFactory.distributionSetType()
                .create().key("test123").name("TestName123").description("Desc123").colour("col"));

        final String body = new JSONObject().put("id", testType.getId()).put("description", "foobardesc")
                .put("name", "nameShouldNotBeChanged").toString();

        mvc.perform(put("/rest/v1/distributionsettypes/{smId}", testType.getId()).content(body)
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(testType.getId().intValue())))
                .andExpect(jsonPath("$.description", equalTo("foobardesc")))
                .andExpect(jsonPath("$.name", equalTo("TestName123"))).andReturn();
    }

    @Test
    @Description("Tests the update of the deletion flag. It is verfied that the distribution set type can't be marked as deleted through update operation.")
    public void updateDistributionSetTypeDeletedFlag() throws Exception {
        final DistributionSetType testType = distributionSetTypeManagement
                .create(entityFactory.distributionSetType().create().key("test123").name("TestName123").colour("col"));

        final String body = new JSONObject().put("id", testType.getId()).put("deleted", true).toString();

        mvc.perform(put("/rest/v1/distributionsettypes/{dstId}", testType.getId()).content(body)
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(testType.getId().intValue())))
                .andExpect(jsonPath("$.deleted", equalTo(false)));
    }

    @Test
    @Description("Checks the correct behaviour of /rest/v1/distributionsettypes GET requests with paging.")
    public void getDistributionSetTypesWithoutAddtionalRequestParameters() throws Exception {

        // 4 types overall (3 hawkbit tenant default, 1 test default
        final int types = 4;
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
        // final DistributionSetType testDsType = distributionSetManagement
        // .createDistributionSetType(entityFactory.distributionSetType().create().key("test123")
        // .name("TestName123").description("Desc123").colour("col"));

        final SoftwareModuleType testSmType = softwareModuleTypeManagement
                .create(entityFactory.softwareModuleType().create().key("test123").name("TestName123"));

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

        final DistributionSetType testNewType = entityFactory.distributionSetType().create().key("test123")
                .name("TestName123").description("Desc123").colour("col").mandatory(Arrays.asList(osType.getId()))
                .optional(Collections.emptyList()).build();

        mvc.perform(post("/rest/v1/distributionsettypes")
                .content(JsonBuilder.distributionSetTypes(Arrays.asList(testNewType)))
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

        final DistributionSetType toLongName = entityFactory.distributionSetType().create().key("test123")
                .name(RandomStringUtils.randomAlphanumeric(NamedEntity.NAME_MAX_SIZE + 1)).build();
        mvc.perform(post("/rest/v1/distributionsettypes")
                .content(JsonBuilder.distributionSetTypes(Arrays.asList(toLongName)))
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
        distributionSetTypeManagement
                .create(entityFactory.distributionSetType().create().key("test123").name("TestName123"));
        distributionSetTypeManagement
                .create(entityFactory.distributionSetType().create().key("test1234").name("TestName1234"));

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
            softwareModuleManagement.create(
                    entityFactory.softwareModule().create().name(str).description(str).vendor(str).version(str));
            character++;
        }
    }

}
