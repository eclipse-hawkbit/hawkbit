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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.jayway.jsonpath.JsonPath;
import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetTypeRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleTypeAssignment;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetTypeRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Test for {@link MgmtDistributionSetTypeResource}.
 * <p/>
 * Feature: Component Tests - Management API<br/>
 * Story: Distribution Set Type Resource
 */
class MgmtDistributionSetTypeResourceTest extends AbstractManagementApiIntegrationTest {

    /**
     * Checks the correct behaviour of /rest/v1/distributionsettypes GET requests.
     */
    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    void getDistributionSetTypes() throws Exception {
        DistributionSetType testType = distributionSetTypeManagement.create(
                DistributionSetTypeManagement.Create.builder()
                        .key("test123")
                        .name("TestName123")
                        .description("Desc123")
                        .colour("col12")
                        .build());
        testType = distributionSetTypeManagement.update(
                DistributionSetTypeManagement.Update.builder().id(testType.getId()).description("Desc1234").build());

        // 4 types overall (2 hawkbit tenant default, 1 test default and 1
        // generated in this test)
        mvc.perform(get("/rest/v1/distributionsettypes").accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
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
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].colour", contains("col12")))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].createdBy", contains("uploadTester")))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].createdAt", contains(testType.getCreatedAt())))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].lastModifiedBy", contains("uploadTester")))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].lastModifiedAt", contains(testType.getLastModifiedAt())))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].key", contains("test123")))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')]._links.self.href",
                        contains("http://localhost/rest/v1/distributionsettypes/" + testType.getId())))
                .andExpect(jsonPath("$.total", equalTo(5)));
    }

    /**
     * Checks the correct behaviour of /rest/v1/distributionsettypes GET requests with sorting by KEY.
     */
    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    void getDistributionSetTypesSortedByKey() throws Exception {
        DistributionSetType testType = distributionSetTypeManagement.create(
                DistributionSetTypeManagement.Create.builder()
                        .key("zzzzz")
                        .name("TestName123")
                        .description("Desc123")
                        .colour("col12")
                        .build());
        testType = distributionSetTypeManagement.update(
                DistributionSetTypeManagement.Update.builder().id(testType.getId()).description("Desc1234").build());

        // descending
        mvc.perform(get("/rest/v1/distributionsettypes")
                        .accept(MediaType.APPLICATION_JSON)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "KEY:DESC"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content.[0].id", equalTo(testType.getId().intValue())))
                .andExpect(jsonPath("$.content.[0].name", equalTo("TestName123")))
                .andExpect(jsonPath("$.content.[0].description", equalTo("Desc1234")))
                .andExpect(jsonPath("$.content.[0].colour", equalTo("col12")))
                .andExpect(jsonPath("$.content.[0].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("$.content.[0].createdAt", equalTo(testType.getCreatedAt())))
                .andExpect(jsonPath("$.content.[0].lastModifiedBy", equalTo("uploadTester")))
                .andExpect(jsonPath("$.content.[0].lastModifiedAt", equalTo(testType.getLastModifiedAt())))
                .andExpect(jsonPath("$.content.[0].key", equalTo("zzzzz")))
                .andExpect(jsonPath("$.total", equalTo(DEFAULT_DS_TYPES + 1)));

        // ascending
        mvc.perform(get("/rest/v1/distributionsettypes")
                        .accept(MediaType.APPLICATION_JSON)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "KEY:ASC"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content.[4].id", equalTo(testType.getId().intValue())))
                .andExpect(jsonPath("$.content.[4].name", equalTo("TestName123")))
                .andExpect(jsonPath("$.content.[4].description", equalTo("Desc1234")))
                .andExpect(jsonPath("$.content.[4].colour", equalTo("col12")))
                .andExpect(jsonPath("$.content.[4].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("$.content.[4].createdAt", equalTo(testType.getCreatedAt())))
                .andExpect(jsonPath("$.content.[4].lastModifiedBy", equalTo("uploadTester")))
                .andExpect(jsonPath("$.content.[4].lastModifiedAt", equalTo(testType.getLastModifiedAt())))
                .andExpect(jsonPath("$.content.[4].key", equalTo("zzzzz")))
                .andExpect(jsonPath("$.total", equalTo(DEFAULT_DS_TYPES + 1)));
    }

    /**
     * Checks the correct behaviour of /rest/v1/distributionsettypes POST requests.
     */
    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    void createDistributionSetTypes() throws Exception {
        final MvcResult mvcResult = runPostDistributionSetType(createTestDistributionSetTestTypes());
        verifyCreatedDistributionSetTypes(mvcResult);
    }

    /**
     * Checks the correct behaviour of /rest/v1/distributionsettypes/{ID}/mandatorymoduletypes POST requests.
     */
    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    void addMandatoryModuleToDistributionSetType() throws Exception {
        DistributionSetType testType = distributionSetTypeManagement.create(
                DistributionSetTypeManagement.Create.builder()
                        .key("test123")
                        .name("TestName123")
                        .description("Desc123")
                        .colour("col12")
                        .build());

        mvc.perform(post("/rest/v1/distributionsettypes/{dstID}/mandatorymoduletypes", testType.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + osType.getId() + "}"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        testType = distributionSetTypeManagement.find(testType.getId()).get();
        assertThat(testType.getLastModifiedBy()).isEqualTo("uploadTester");
        assertThat(testType.getMandatoryModuleTypes()).containsExactly(osType);
        assertThat(testType.getOptionalModuleTypes()).isEmpty();
    }

    /**
     * Checks the correct behaviour of /rest/v1/distributionsettypes/{ID}/optionalmoduletypes POST requests.
     */
    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    void addOptionalModuleToDistributionSetType() throws Exception {
        DistributionSetType testType = distributionSetTypeManagement.create(
                DistributionSetTypeManagement.Create.builder()
                        .key("test123")
                        .name("TestName123")
                        .description("Desc123")
                        .colour("col12")
                        .build());

        mvc.perform(post("/rest/v1/distributionsettypes/{dstID}/optionalmoduletypes", testType.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + osType.getId() + "}"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        testType = distributionSetTypeManagement.find(testType.getId()).get();
        assertThat(testType.getLastModifiedBy()).isEqualTo("uploadTester");
        assertThat(testType.getOptionalModuleTypes()).containsExactly(osType);
        assertThat(testType.getMandatoryModuleTypes()).isEmpty();
    }

    /**
     * Verifies quota enforcement for /rest/v1/distributionsettypes/{ID}/optionalmoduletypes POST requests.
     */
    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    void assignModuleTypesToDistributionSetTypeUntilQuotaExceeded() throws Exception {
        // create software module types
        final int maxSoftwareModuleTypes = quotaManagement.getMaxSoftwareModuleTypesPerDistributionSetType();
        final List<Long> moduleTypeIds = new ArrayList<>();
        for (int i = 0; i < maxSoftwareModuleTypes + 1; ++i) {
            final SoftwareModuleTypeManagement.Create smCreate = SoftwareModuleTypeManagement.Create.builder().name("smType_" + i)
                    .description("smType_" + i).maxAssignments(1).colour("blue").key("smType_" + i).build();
            moduleTypeIds.add(softwareModuleTypeManagement.create(smCreate).getId());
        }

        // verify quota enforcement for optional module types

        final DistributionSetType testType = distributionSetTypeManagement.create(
                DistributionSetTypeManagement.Create.builder()
                        .key("testType").name("testType").description("testType").colour("col12").build());
        assertThat(testType.getOptLockRevision()).isEqualTo(1);

        for (int i = 0; i < moduleTypeIds.size() - 1; ++i) {
            mvc.perform(post("/rest/v1/distributionsettypes/{dstID}/optionalmoduletypes", testType.getId())
                            .content("{\"id\":" + moduleTypeIds.get(i) + "}").contentType(MediaType.APPLICATION_JSON))
                    .andDo(MockMvcResultPrinter.print())
                    .andExpect(status().isNoContent());
        }

        mvc.perform(post("/rest/v1/distributionsettypes/{dstID}/optionalmoduletypes", testType.getId())
                        .content("{\"id\":" + moduleTypeIds.get(moduleTypeIds.size() - 1) + "}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.exceptionClass", equalTo(AssignmentQuotaExceededException.class.getName())))
                .andExpect(jsonPath("$.errorCode", equalTo(SpServerError.SP_QUOTA_EXCEEDED.getKey())));

        // verify quota enforcement for mandatory module types

        final DistributionSetType testType2 = distributionSetTypeManagement.create(
                DistributionSetTypeManagement.Create.builder()
                        .key("testType2").name("testType2").description("testType2").colour("col12").build());
        assertThat(testType2.getOptLockRevision()).isEqualTo(1);

        for (int i = 0; i < moduleTypeIds.size() - 1; ++i) {
            mvc.perform(post("/rest/v1/distributionsettypes/{dstID}/mandatorymoduletypes", testType2.getId())
                            .content("{\"id\":" + moduleTypeIds.get(i) + "}").contentType(MediaType.APPLICATION_JSON))
                    .andDo(MockMvcResultPrinter.print())
                    .andExpect(status().isNoContent());
        }

        mvc.perform(post("/rest/v1/distributionsettypes/{dstID}/mandatorymoduletypes", testType2.getId())
                        .content("{\"id\":" + moduleTypeIds.get(moduleTypeIds.size() - 1) + "}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.exceptionClass", equalTo(AssignmentQuotaExceededException.class.getName())))
                .andExpect(jsonPath("$.errorCode", equalTo(SpServerError.SP_QUOTA_EXCEEDED.getKey())));
    }

    /**
     * Checks the correct behaviour of /rest/v1/distributionsettypes/{ID}/mandatorymoduletypes GET requests.
     */
    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    void getMandatoryModulesOfDistributionSetType() throws Exception {
        final DistributionSetType testType = generateTestType();

        mvc.perform(get("/rest/v1/distributionsettypes/{dstID}/mandatorymoduletypes", testType.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("[0].name", equalTo(osType.getName())))
                .andExpect(jsonPath("[0].description", equalTo(osType.getDescription())))
                .andExpect(jsonPath("[0].maxAssignments", equalTo(1)))
                .andExpect(jsonPath("[0].key", equalTo("os")));
    }

    /**
     * Checks the correct behaviour of /rest/v1/distributionsettypes/{ID}/optionalmoduletypes GET requests.
     */
    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    void getOptionalModulesOfDistributionSetType() throws Exception {
        final DistributionSetType testType = generateTestType();

        mvc.perform(get("/rest/v1/distributionsettypes/{dstID}/optionalmoduletypes", testType.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("[0].name", equalTo(appType.getName())))
                .andExpect(jsonPath("[0].description", equalTo(appType.getDescription())))
                .andExpect(jsonPath("[0].maxAssignments", equalTo(Integer.MAX_VALUE)))
                .andExpect(jsonPath("[0].key", equalTo("application")));
    }

    /**
     * Checks the correct behaviour of /rest/v1/distributionsettypes/{ID}/mandatorymoduletypes/{ID} GET requests.
     */
    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    void getMandatoryModuleOfDistributionSetType() throws Exception {
        final DistributionSetType testType = generateTestType();

        mvc.perform(get("/rest/v1/distributionsettypes/{dstID}/mandatorymoduletypes/{smtId}", testType.getId(),
                        osType.getId()).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(osType.getId().intValue())))
                .andExpect(jsonPath("$.name", equalTo(osType.getName())))
                .andExpect(jsonPath("$.description", equalTo(osType.getDescription())))
                .andExpect(jsonPath("$.createdBy", equalTo(osType.getCreatedBy())))
                .andExpect(jsonPath("$.createdAt", equalTo(osType.getCreatedAt())))
                .andExpect(jsonPath("$.lastModifiedBy", equalTo(osType.getLastModifiedBy())))
                .andExpect(jsonPath("$.lastModifiedAt", equalTo(osType.getLastModifiedAt())));
    }

    /**
     * Checks the correct behaviour of /rest/v1/distributionsettypes/{ID}/optionalmoduletypes/{ID} GET requests.
     */
    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    void getOptionalModuleOfDistributionSetType() throws Exception {
        final DistributionSetType testType = generateTestType();

        mvc.perform(get("/rest/v1/distributionsettypes/{dstID}/optionalmoduletypes/{smtId}", testType.getId(),
                        appType.getId()).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(appType.getId().intValue())))
                .andExpect(jsonPath("$.name", equalTo(appType.getName())))
                .andExpect(jsonPath("$.description", equalTo(appType.getDescription())))
                .andExpect(jsonPath("$.createdBy", equalTo(appType.getCreatedBy())))
                .andExpect(jsonPath("$.createdAt", equalTo(appType.getCreatedAt())))
                .andExpect(jsonPath("$.lastModifiedBy", equalTo(appType.getLastModifiedBy())))
                .andExpect(jsonPath("$.lastModifiedAt", equalTo(appType.getLastModifiedAt())));
    }

    /**
     * Checks the correct behaviour of /rest/v1/distributionsettypes/{ID}/mandatorymoduletypes/{ID} DELETE requests.
     */
    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    void removeMandatoryModuleToDistributionSetType() throws Exception {
        DistributionSetType testType = generateTestType();

        mvc.perform(delete("/rest/v1/distributionsettypes/{dstID}/mandatorymoduletypes/{smtId}", testType.getId(),
                        osType.getId()).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        testType = distributionSetTypeManagement.find(testType.getId()).get();
        assertThat(testType.getLastModifiedBy()).isEqualTo("uploadTester");
        assertThat(testType.getOptionalModuleTypes()).containsExactly(appType);
        assertThat(testType.getMandatoryModuleTypes()).isEmpty();
    }

    /**
     * Checks the correct behaviour of /rest/v1/distributionsettypes/{ID}/optionalmoduletypes/{ID} DELETE requests.
     */
    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    void removeOptionalModuleToDistributionSetType() throws Exception {
        DistributionSetType testType = generateTestType();

        mvc.perform(delete("/rest/v1/distributionsettypes/{dstID}/optionalmoduletypes/{smtId}", testType.getId(),
                        appType.getId()).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        testType = distributionSetTypeManagement.find(testType.getId()).get();
        assertThat(testType.getLastModifiedBy()).isEqualTo("uploadTester");
        assertThat(testType.getOptionalModuleTypes()).isEmpty();
        assertThat(testType.getMandatoryModuleTypes()).containsExactly(osType);
    }

    /**
     * Checks the correct behaviour of /rest/v1/distributionsettypes/{ID} GET requests.
     */
    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    void getDistributionSetType() throws Exception {
        DistributionSetType testType = distributionSetTypeManagement.create(DistributionSetTypeManagement.Create.builder()
                .key("test123").name("TestName123").description("Desc123").build());
        testType = distributionSetTypeManagement
                .update(DistributionSetTypeManagement.Update.builder().id(testType.getId()).description("Desc1234").build());

        mvc.perform(get("/rest/v1/distributionsettypes/{dstId}", testType.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.name", equalTo("TestName123")))
                .andExpect(jsonPath("$.description", equalTo("Desc1234")))
                .andExpect(jsonPath("$.colour").doesNotExist())
                .andExpect(jsonPath("$.createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("$.createdAt", equalTo(testType.getCreatedAt())))
                .andExpect(jsonPath("$.lastModifiedBy", equalTo("uploadTester")))
                .andExpect(jsonPath("$.deleted", equalTo(testType.isDeleted())));
    }

    /**
     * Handles the GET request of retrieving all distribution set types within SP based on parameter.
     */
    @Test
    void getDistributionSetTypesWithParameter() throws Exception {
        mvc.perform(get(MgmtDistributionSetTypeRestApi.DISTRIBUTIONSETTYPES_V1
                        + "?limit=10&sort=name:ASC&offset=0&q=name==a"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    /**
     * Checks the correct behaviour of /rest/v1/DistributionSetTypes/{ID} DELETE requests (hard delete scenario).
     */
    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    void deleteDistributionSetTypeUnused() throws Exception {
        final DistributionSetType testType = distributionSetTypeManagement.create(
                DistributionSetTypeManagement.Create.builder()
                        .key("test123").name("TestName123").description("Desc123").colour("col12").build());

        assertThat(distributionSetTypeManagement.count()).isEqualTo(DEFAULT_DS_TYPES + 1);

        mvc.perform(delete("/rest/v1/distributionsettypes/{dsId}", testType.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        assertThat(distributionSetTypeManagement.count()).isEqualTo(DEFAULT_DS_TYPES);
    }

    /**
     * Ensures that DS type deletion request to API on an entity that does not exist results in NOT_FOUND.
     */
    @Test
    void deleteDistributionSetTypeThatDoesNotExistLeadsToNotFound() throws Exception {
        mvc.perform(delete("/rest/v1/distributionsettypes/1234"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    /**
     * Checks the correct behaviour of /rest/v1/DistributionSetTypes/{ID} DELETE requests (soft delete scenario).
     */
    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    void deleteDistributionSetTypeUsed() throws Exception {
        final DistributionSetType testType = distributionSetTypeManagement.create(
                DistributionSetTypeManagement.Create.builder()
                        .key("test123").name("TestName123").description("Desc123").colour("col12").build());

        distributionSetManagement.create(DistributionSetManagement.Create.builder()
                .type(distributionSetTypeManagement.findByKey(testType.getKey()).orElseThrow())
                .name("sdfsd").version("1").description("dsfsdf")
                .build());

        assertThat(distributionSetTypeManagement.count()).isEqualTo(DEFAULT_DS_TYPES + 1);
        assertThat(distributionSetManagement.count()).isEqualTo(1);

        mvc.perform(get("/rest/v1/distributionsettypes/{dstId}", testType.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted", equalTo(false)));

        mvc.perform(delete("/rest/v1/distributionsettypes/{dstId}", testType.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        mvc.perform(get("/rest/v1/distributionsettypes/{dstId}", testType.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted", equalTo(true)));

        assertThat(distributionSetManagement.count()).isEqualTo(1);
        assertThat(distributionSetTypeManagement.count()).isEqualTo(DEFAULT_DS_TYPES);
    }

    /**
     * Checks the correct behaviour of /rest/v1/distributionsettypes/{ID} PUT requests.
     */
    @Test
    void updateDistributionSetTypeColourDescriptionAndNameUntouched() throws Exception {
        final DistributionSetType testType = distributionSetTypeManagement.create(
                DistributionSetTypeManagement.Create.builder()
                        .key("test123").name("TestName123").description("Desc123").colour("col").build());

        final String body = new JSONObject().put("id", testType.getId()).put("description", "foobardesc")
                .put("colour", "updatedColour")
                .put("name", "nameShouldNotBeChanged").toString();

        mvc.perform(put("/rest/v1/distributionsettypes/{smId}", testType.getId()).content(body)
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
     * Handles the PUT request for a single distribution set type within SP.
     */
    @Test
    void updateDistributionSetTypeDescriptionAndColor() throws Exception {
        final DistributionSetType testType = distributionSetTypeManagement.update(
                DistributionSetTypeManagement.Update.builder()
                        .id(testdataFactory.createDistributionSet().getType().getId()).description("Desc1234").build());
        final String body = new JSONObject()
                .put("description", "an updated description")
                .put("colour", "rgb(106,178,83)").toString();

        mvc
                .perform(put(MgmtDistributionSetTypeRestApi.DISTRIBUTIONSETTYPES_V1 + "/{distributionSetTypeId}",
                        testType.getId()).content(body).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    /**
     * Tests the update of the deletion flag. It is verfied that the distribution set type can't be marked as deleted through update operation.
     */
    @Test
    void updateDistributionSetTypeDeletedFlag() throws Exception {
        final DistributionSetType testType = distributionSetTypeManagement
                .create(DistributionSetTypeManagement.Create.builder().key("test123").name("TestName123").colour("col").build());

        final String body = new JSONObject().put("id", testType.getId()).put("deleted", true).toString();

        mvc.perform(put("/rest/v1/distributionsettypes/{dstId}", testType.getId()).content(body)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(testType.getId().intValue())))
                .andExpect(jsonPath("$.deleted", equalTo(false)));
    }

    /**
     * Checks the correct behaviour of /rest/v1/distributionsettypes GET requests with paging.
     */
    @Test
    void getDistributionSetTypesWithoutAddtionalRequestParameters() throws Exception {

        // 4 types overall (3 hawkbit tenant default, 1 test default
        final int types = 4;
        mvc.perform(get(MgmtDistributionSetTypeRestApi.DISTRIBUTIONSETTYPES_V1))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(types)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(types)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(types)));
    }

    /**
     * Checks the correct behaviour of /rest/v1/distributionsettypes GET requests with paging.
     */
    @Test
    void getDistributionSetTypesWithPagingLimitRequestParameter() throws Exception {

        final int types = DEFAULT_DS_TYPES;
        final int limitSize = 1;
        mvc.perform(get(MgmtDistributionSetTypeRestApi.DISTRIBUTIONSETTYPES_V1)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(limitSize)))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(types)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(limitSize)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(limitSize)));
    }

    /**
     * Checks the correct behaviour of /rest/v1/distributionsettypes GET requests with paging.
     */
    @Test
    void getDistributionSetTypesWithPagingLimitAndOffsetRequestParameter() throws Exception {
        final int types = DEFAULT_DS_TYPES;
        final int offsetParam = 2;
        final int expectedSize = types - offsetParam;
        mvc.perform(get(MgmtDistributionSetTypeRestApi.DISTRIBUTIONSETTYPES_V1)
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
    void invalidRequestsOnDistributionSetTypesResource() throws Exception {
        final SoftwareModuleType testSmType = softwareModuleTypeManagement
                .create(SoftwareModuleTypeManagement.Create.builder().key("test123").name("TestName123").build());

        // DST does not exist
        mvc.perform(get("/rest/v1/distributionsettypes/12345678"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
        mvc.perform(get("/rest/v1/distributionsettypes/12345678/mandatorymoduletypes"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
        mvc.perform(get("/rest/v1/distributionsettypes/12345678/optionalmoduletypes"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        mvc.perform(delete("/rest/v1/distributionsettypes/12345678"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // Module types incorrect
        mvc.perform(get("/rest/v1/distributionsettypes/" + standardDsType.getId() + "/mandatorymoduletypes"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        mvc.perform(get("/rest/v1/distributionsettypes/" + standardDsType.getId() + "/mandatorymoduletypes/565765656"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
        mvc.perform(get("/rest/v1/distributionsettypes/" + standardDsType.getId() + "/optionalmoduletypes/565765656"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        mvc.perform(get(
                        "/rest/v1/distributionsettypes/" + standardDsType.getId() + "/mandatorymoduletypes/" + osType.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        mvc.perform(get("/rest/v1/distributionsettypes/" + standardDsType.getId() + "/mandatorymoduletypes/"
                        + testSmType.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        mvc.perform(get(
                        "/rest/v1/distributionsettypes/" + standardDsType.getId() + "/optionalmoduletypes/" + osType.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        mvc.perform(get(
                        "/rest/v1/distributionsettypes/" + standardDsType.getId() + "/optionalmoduletypes/" + appType.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        mvc.perform(get("/rest/v1/distributionsettypes/" + standardDsType.getId() + "/optionalmoduletypes/"
                        + testSmType.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // Modules types at creation time invalid

        final DistributionSetTypeManagement.Create testNewType = DistributionSetTypeManagement.Create.builder()
                .key("test123")
                .name("TestName123").description("Desc123").colour("col")
                .mandatoryModuleTypes(Set.of(osType))
                .optionalModuleTypes(Collections.emptySet())
                .build();

        mvc.perform(post("/rest/v1/distributionsettypes").content(toJson(List.of(testNewType)))
                        .contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isUnsupportedMediaType());

        // bad request - no content
        mvc.perform(post("/rest/v1/distributionsettypes").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        // bad request - bad content
        mvc.perform(post("/rest/v1/distributionsettypes").content("sdfjsdlkjfskdjf".getBytes())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        // Missing mandatory field name
        mvc.perform(post("/rest/v1/distributionsettypes").content("[{\"description\":\"Desc123\",\"key\":\"test123\"}]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        final DistributionSetTypeManagement.Create toLongName = DistributionSetTypeManagement.Create.builder()
                .key("test123")
                .name(randomString(NamedEntity.NAME_MAX_SIZE + 1))
                .build();
        mvc.perform(post("/rest/v1/distributionsettypes").content(toJson(List.of(toLongName)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        // not allowed methods
        mvc.perform(put("/rest/v1/distributionsettypes"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(delete("/rest/v1/distributionsettypes"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(put("/rest/v1/distributionsettypes/" + standardDsType.getId() + "/mandatorymoduletypes"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(put(
                        "/rest/v1/distributionsettypes/" + standardDsType.getId() + "/optionalmoduletypes/" + osType.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
    }

    /**
     * Search erquest of software module types.
     */
    @Test
    void searchDistributionSetTypeRsql() throws Exception {
        distributionSetTypeManagement
                .create(DistributionSetTypeManagement.Create.builder().key("test123").name("TestName123").build());
        distributionSetTypeManagement
                .create(DistributionSetTypeManagement.Create.builder().key("test1234").name("TestName1234").build());

        final String rsqlFindLikeDs1OrDs2 = "name==TestName123,name==TestName1234";

        mvc.perform(get("/rest/v1/distributionsettypes?q=" + rsqlFindLikeDs1OrDs2))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("size", equalTo(2)))
                .andExpect(jsonPath("total", equalTo(2)))
                .andExpect(jsonPath("content[0].name", equalTo("TestName123")))
                .andExpect(jsonPath("content[1].name", equalTo("TestName1234")));
    }

    private void verifyCreatedDistributionSetTypes(final MvcResult mvcResult) throws UnsupportedEncodingException {
        final DistributionSetType created1 = distributionSetTypeManagement.findByKey("testKey1").get();
        final DistributionSetType created2 = distributionSetTypeManagement.findByKey("testKey2").get();
        final DistributionSetType created3 = distributionSetTypeManagement.findByKey("testKey3").get();

        assertThat(created1.getMandatoryModuleTypes()).containsOnly(osType);
        assertThat(created1.getOptionalModuleTypes()).containsOnly(runtimeType);
        assertThat(created2.getOptionalModuleTypes()).containsOnly(osType, runtimeType, appType);
        assertThat(created3.getMandatoryModuleTypes()).containsOnly(osType, runtimeType);

        assertThat((Object) JsonPath.compile("[0]_links.self.href").read(mvcResult.getResponse().getContentAsString()))
                .hasToString("http://localhost/rest/v1/distributionsettypes/" + created1.getId());
        assertThat((Object) JsonPath.compile("[1]_links.self.href").read(mvcResult.getResponse().getContentAsString()))
                .hasToString("http://localhost/rest/v1/distributionsettypes/" + created2.getId());
        assertThat((Object) JsonPath.compile("[2]_links.self.href").read(mvcResult.getResponse().getContentAsString()))
                .hasToString("http://localhost/rest/v1/distributionsettypes/" + created3.getId());

        assertThat(distributionSetTypeManagement.count()).isEqualTo(7);
    }

    private MvcResult runPostDistributionSetType(final List<DistributionSetTypeManagement.Create> types) throws Exception {
        return mvc
                .perform(post("/rest/v1/distributionsettypes").content(toJson(types.stream()
                                .map(type -> new MgmtDistributionSetTypeRequestBodyPost()
                                        .setMandatorymodules(type.getMandatoryModuleTypes().stream()
                                                .map(SoftwareModuleType::getId)
                                                .map(id -> new MgmtSoftwareModuleTypeAssignment().setId(id))
                                                .map(MgmtSoftwareModuleTypeAssignment.class::cast)
                                                .toList())
                                        .setOptionalmodules(type.getOptionalModuleTypes().stream()
                                                .map(SoftwareModuleType::getId)
                                                .map(id -> new MgmtSoftwareModuleTypeAssignment().setId(id))
                                                .map(MgmtSoftwareModuleTypeAssignment.class::cast)
                                                .toList())
                                        .setKey(type.getKey())
                                        .setName(type.getName())
                                        .setDescription(type.getDescription())
                                        .setColour(type.getColour()))
                                .toList()))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("[0].name", equalTo("TestName1")))
                .andExpect(jsonPath("[0].key", equalTo("testKey1")))
                .andExpect(jsonPath("[0].description", equalTo("Desc1")))
                .andExpect(jsonPath("[0].colour", equalTo("col")))
                .andExpect(jsonPath("[0].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[1].name", equalTo("TestName2")))
                .andExpect(jsonPath("[1].key", equalTo("testKey2")))
                .andExpect(jsonPath("[1].description", equalTo("Desc2")))
                .andExpect(jsonPath("[1].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[2].name", equalTo("TestName3")))
                .andExpect(jsonPath("[2].key", equalTo("testKey3")))
                .andExpect(jsonPath("[2].description", equalTo("Desc3")))
                .andExpect(jsonPath("[2].createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[2].createdAt", not(equalTo(0))))
                .andReturn();
    }

    private List<DistributionSetTypeManagement.Create> createTestDistributionSetTestTypes() {
        assertThat(distributionSetTypeManagement.count()).isEqualTo(DEFAULT_DS_TYPES);

        return Arrays.asList(
                DistributionSetTypeManagement.Create.builder()
                        .key("testKey1").name("TestName1").description("Desc1").colour("col")
                        .mandatoryModuleTypes(Set.of(osType))
                        .optionalModuleTypes(Set.of(runtimeType))
                        .build(),
                DistributionSetTypeManagement.Create.builder()
                        .key("testKey2").name("TestName2").description("Desc2").colour("col")
                        .mandatoryModuleTypes(Set.of())
                        .optionalModuleTypes(Set.of(runtimeType, osType, appType))
                        .build(),
                DistributionSetTypeManagement.Create.builder()
                        .key("testKey3").name("TestName3").description("Desc3").colour("col")
                        .mandatoryModuleTypes(Set.of(runtimeType, osType))
                        .optionalModuleTypes(Set.of())
                        .build());
    }

    private DistributionSetType generateTestType() {
        final DistributionSetType testType = distributionSetTypeManagement.create(
                DistributionSetTypeManagement.Create.builder()
                        .key("test123").name("TestName123").description("Desc123").colour("col")
                        .mandatoryModuleTypes(Set.of(osType))
                        .optionalModuleTypes(Set.of(appType))
                        .build());
        assertThat(testType.getOptLockRevision()).isEqualTo(1);
        assertThat(testType.getOptionalModuleTypes()).containsExactly(appType);
        assertThat(testType.getMandatoryModuleTypes()).containsExactly(osType);
        return testType;
    }
}