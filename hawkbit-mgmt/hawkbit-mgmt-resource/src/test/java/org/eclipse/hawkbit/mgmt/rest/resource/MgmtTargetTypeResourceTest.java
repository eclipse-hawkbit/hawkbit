/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.jayway.jsonpath.JsonPath;
import org.eclipse.hawkbit.auth.SpPermission;
import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.mgmt.json.model.MgmtId;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetTypeManagement.Create;
import org.eclipse.hawkbit.repository.TargetTypeManagement.Update;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Spring MVC Tests against the MgmtTargetTypeResource.
 * <p/>
 * Feature: Component Tests - Management API<br/>
 * Story: Target Type Resource
 */
class MgmtTargetTypeResourceTest extends AbstractManagementApiIntegrationTest {

    private static final String TARGETTYPES_ENDPOINT = MgmtRestConstants.TARGETTYPE_V1_REQUEST_MAPPING;
    private static final String TARGETTYPE_SINGLE_ENDPOINT = MgmtRestConstants.TARGETTYPE_V1_REQUEST_MAPPING + "/{typeid}";
    private static final String TARGETTYPE_DSTYPES_ENDPOINT = TARGETTYPE_SINGLE_ENDPOINT + "/" + MgmtRestConstants.TARGETTYPE_V1_DS_TYPES;
    private static final String TARGETTYPE_DSTYPE_SINGLE_ENDPOINT = TARGETTYPE_DSTYPES_ENDPOINT + "/{dstypeid}";

    private static final String TEST_USER = "targetTypeTester";
    private static final String SPACE_AND_DESCRIPTION = " description";

    /**
     * GET targettypes returns Forbidden when permission is missing
     */
    @Test
    @WithUser(
            principal = "targetTypeTester", allSpPermissions = true,
            removeFromAllPermission = {
                    SpPermission.CREATE_TARGET, SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET,
                    SpPermission.READ_TARGET_TYPE })
    void getTargetTypesWithoutPermission() throws Exception {
        mvc.perform(get(TARGETTYPES_ENDPOINT).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden());
    }

    /**
     * Checks the correct behaviour of /rest/v1/targettypes/{id} GET request.
     */
    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    void getTargetType() throws Exception {
        String typeName = "TestTypeGET";
        TargetType testType = createTestTargetTypeInDB(typeName);
        Long typeId = testType.getId();

        mvc.perform(get(TARGETTYPE_SINGLE_ENDPOINT, typeId).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id", is(typeId), Long.class))
                .andExpect(jsonPath("$.name", equalTo(typeName)))
                .andExpect(jsonPath("$.colour", is("#000000")))
                .andExpect(jsonPath("$.description", equalTo(typeName + SPACE_AND_DESCRIPTION)))
                .andExpect(jsonPath("$.createdBy", equalTo(TEST_USER)))
                .andExpect(jsonPath("$.createdAt", equalTo(testType.getCreatedAt())))
                .andExpect(jsonPath("$.lastModifiedBy", equalTo(TEST_USER)))
                .andExpect(jsonPath("$.lastModifiedAt", equalTo(testType.getLastModifiedAt())))
                .andExpect(jsonPath("$._links.self.href", equalTo("http://localhost/rest/v1/targettypes/" + typeId)))
                .andExpect(jsonPath("$.deleted", equalTo(false)))
                .andExpect(jsonPath("$.key", equalTo(typeName + " key")))
                .andExpect(jsonPath("$._links.compatibledistributionsettypes.href",
                        equalTo("http://localhost/rest/v1/targettypes/" + typeId + "/compatibledistributionsettypes")));
    }

    /**
     * Checks the correct behaviour of /rest/v1/targettypes GET requests.
     */
    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    void getTargetTypes() throws Exception {
        String typeName = "TestTypeGET";
        int count = 5;
        final List<? extends TargetType> testTypes = createTestTargetTypesInDB(typeName, count);

        ResultActions resultActions = mvc.perform(get(TARGETTYPES_ENDPOINT).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print());

        for (int index = 0; index < count; index++) {
            Long typeId = testTypes.get(index).getId();
            resultActions.andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.content.[?(@.id=='" + typeId + "')].id", contains(typeId.intValue())))
                    .andExpect(jsonPath("$.content.[?(@.id=='" + typeId + "')].name", contains(typeName + index)))
                    .andExpect(jsonPath("$.content.[?(@.id=='" + typeId + "')].colour", contains("#000000")))
                    .andExpect(jsonPath("$.content.[?(@.id=='" + typeId + "')].description",
                            contains(typeName + SPACE_AND_DESCRIPTION)))
                    .andExpect(jsonPath("$.content.[?(@.id=='" + typeId + "')].createdBy", contains(TEST_USER)))
                    .andExpect(jsonPath("$.content.[?(@.id=='" + typeId + "')].createdAt",
                            contains(testTypes.get(index).getCreatedAt())))
                    .andExpect(jsonPath("$.content.[?(@.id=='" + typeId + "')].lastModifiedBy", contains(TEST_USER)))
                    .andExpect(jsonPath("$.content.[?(@.id=='" + typeId + "')].lastModifiedAt",
                            contains(testTypes.get(index).getLastModifiedAt())))
                    .andExpect(jsonPath("$.content.[?(@.id=='" + typeId + "')].deleted", contains(false)))
                    .andExpect(jsonPath("$.content.[?(@.id=='" + typeId + "')].key", contains(typeName + index + " key")))
                    .andExpect(jsonPath("$.content.[?(@.id=='" + typeId + "')]._links.self.href",
                            contains("http://localhost/rest/v1/targettypes/" + typeId)))
                    .andExpect(
                            jsonPath("$.content.[?(@.id=='" + typeId + "')]._links.compatibledistributionsettypes.href")
                                    .doesNotExist())
                    .andExpect(jsonPath("$.total", equalTo(count)))
                    .andExpect(jsonPath("$.size", equalTo(count)));
        }
    }

    /**
     * Checks the correct behaviour of /rest/v1/targettypes GET requests without prior created target types.
     */
    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    void getDefaultTargetTypes() throws Exception {

        // 0 types overall (no default types are created)
        final int types = 0;
        mvc.perform(get(TARGETTYPES_ENDPOINT))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(types)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(types)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(types)));
    }

    /**
     * Checks the correct behaviour of /rest/v1/targettypes GET requests with sorting by name.
     */
    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    void getTargetTypesSortedByName() throws Exception {
        String typeNameA = "ATestTypeGETsorted";
        String typeNameB = "BTestTypeGETsorted";
        String typeNameC = "CTestTypeGETsorted";
        createTestTargetTypeInDB(typeNameB);
        TargetType testTypeC = createTestTargetTypeInDB(typeNameC);
        TargetType testTypeA = createTestTargetTypeInDB(typeNameA);

        testTypeA = targetTypeManagement
                .update(Update.builder().id(testTypeA.getId()).description("Updated description").build());

        // descending
        mvc.perform(get(TARGETTYPES_ENDPOINT).accept(MediaType.APPLICATION_JSON)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "name:DESC"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content.[0].id", equalTo(testTypeC.getId().intValue())))
                .andExpect(jsonPath("$.content.[0].name", equalTo(typeNameC)))
                .andExpect(jsonPath("$.content.[0].colour", equalTo("#000000")))
                .andExpect(jsonPath("$.content.[0].description", equalTo(typeNameC + SPACE_AND_DESCRIPTION)))
                .andExpect(jsonPath("$.content.[0].createdBy", equalTo(TEST_USER)))
                .andExpect(jsonPath("$.content.[0].createdAt", equalTo(testTypeC.getCreatedAt())))
                .andExpect(jsonPath("$.content.[0].lastModifiedBy", equalTo(TEST_USER)))
                .andExpect(jsonPath("$.content.[0].lastModifiedAt", equalTo(testTypeC.getLastModifiedAt())))
                .andExpect(jsonPath("$.content.[0].deleted", equalTo(false)))
                .andExpect(jsonPath("$.content.[0].key", equalTo(typeNameC + " key")))
                .andExpect(jsonPath("$.content.[0]._links.self.href",
                        equalTo("http://localhost/rest/v1/targettypes/" + testTypeC.getId())))
                .andExpect(jsonPath("$.content.[0]._links.compatibledistributionsettypes.href").doesNotExist())
                .andExpect(jsonPath("$.total", equalTo(3)))
                .andExpect(jsonPath("$.size", equalTo(3)))
                .andExpect(jsonPath("$.content.[1].name", equalTo(typeNameB)))
                .andExpect(jsonPath("$.content.[2].name", equalTo(typeNameA)));

        // ascending
        mvc.perform(get(TARGETTYPES_ENDPOINT).accept(MediaType.APPLICATION_JSON)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "name:ASC"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content.[0].id", equalTo(testTypeA.getId().intValue())))
                .andExpect(jsonPath("$.content.[0].name", equalTo(typeNameA)))
                .andExpect(jsonPath("$.content.[0].description", equalTo("Updated description")))
                .andExpect(jsonPath("$.content.[0].colour", equalTo("#000000")))
                .andExpect(jsonPath("$.content.[0].createdBy", equalTo(TEST_USER)))
                .andExpect(jsonPath("$.content.[0].createdAt", equalTo(testTypeA.getCreatedAt())))
                .andExpect(jsonPath("$.content.[0].lastModifiedBy", equalTo(TEST_USER)))
                .andExpect(jsonPath("$.content.[0].lastModifiedAt", equalTo(testTypeA.getLastModifiedAt())))
                .andExpect(jsonPath("$.content.[0].deleted", equalTo(false)))
                .andExpect(jsonPath("$.content.[0].key", equalTo(typeNameA + " key")))
                .andExpect(jsonPath("$.content.[0]._links.self.href",
                        equalTo("http://localhost/rest/v1/targettypes/" + testTypeA.getId())))
                .andExpect(jsonPath("$.content.[0]._links.compatibledistributionsettypes.href").doesNotExist())
                .andExpect(jsonPath("$.total", equalTo(3)))
                .andExpect(jsonPath("$.size", equalTo(3)))
                .andExpect(jsonPath("$.content.[1].name", equalTo(typeNameB)))
                .andExpect(jsonPath("$.content.[2].name", equalTo(typeNameC)));

    }

    /**
     * Checks the correct behaviour of /rest/v1/targettypes GET requests with paging.
     */
    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    void getTargetTypesWithPagingLimitRequestParameter() throws Exception {
        final String typePrefix = "TestTypeGETPaging";
        final int count = 10;
        final int limit = 3;
        createTestTargetTypesInDB(typePrefix, count);

        mvc.perform(get(TARGETTYPES_ENDPOINT).param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT,
                        String.valueOf(limit)))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(count)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(limit)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(limit)));
    }

    /**
     * Checks the correct behaviour of /rest/v1/targettypes GET requests with paging and offset.
     */
    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    void getTargetTypesWithPagingLimitAndOffsetRequestParameter() throws Exception {
        final int count = 10;
        final int offset = 2;
        final int expectedSize = count - offset;
        final String typePrefix = "TestTypeGETPaging";
        createTestTargetTypesInDB(typePrefix, count);

        mvc.perform(get(TARGETTYPES_ENDPOINT)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(offset))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(count)))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(count)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(expectedSize)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(expectedSize)));
    }

    /**
     * Checks the correct behaviour of /rest/v1/targettypes/{ID} PUT requests.
     */
    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    void updateTargetType() throws Exception {
        String typeName = "TestTypePUT";
        final TargetType testType = createTestTargetTypeInDB(typeName);
        final String body = new JSONObject().put("id", testType.getId()).put("description", "updated description")
                .put("name", "TestTypePUTupdated").put("colour", "#ffffff").toString();

        mvc.perform(
                        put(TARGETTYPE_SINGLE_ENDPOINT, testType.getId()).content(body).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(testType.getId().intValue())))
                .andExpect(jsonPath("$.description", equalTo("updated description")))
                .andExpect(jsonPath("$.name", equalTo("TestTypePUTupdated")))
                .andExpect(jsonPath("$.colour", equalTo("#ffffff")))
                .andReturn();
    }

    /**
     * Checks the correct behaviour of /rest/v1/targettypes/{id} GET requests.
     */
    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    void getUpdatedTargetType() throws Exception {
        final String initialTypeName = "TestTypeGET";
        TargetType testType = createTestTargetTypeInDB(initialTypeName);
        final String typeNameUpdated = "TestTypeGETupdated";
        testType = targetTypeManagement.update(Update.builder().id(testType.getId())
                .name(typeNameUpdated).description("Updated Description").colour("#ffffff")
                .build());

        mvc.perform(get(TARGETTYPE_SINGLE_ENDPOINT, testType.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.name", equalTo(typeNameUpdated)))
                .andExpect(jsonPath("$.description", equalTo("Updated Description")))
                .andExpect(jsonPath("$.colour", equalTo("#ffffff")))
                .andExpect(jsonPath("$.createdBy", equalTo(TEST_USER)))
                .andExpect(jsonPath("$.createdAt", equalTo(testType.getCreatedAt())))
                .andExpect(jsonPath("$.lastModifiedBy", equalTo(TEST_USER)))
                .andExpect(jsonPath("$.lastModifiedAt", equalTo(testType.getLastModifiedAt())))
                .andExpect(jsonPath("$.deleted", equalTo(false)))
                .andExpect(jsonPath("$.key", equalTo(initialTypeName + " key")));
    }

    /**
     * Checks the correct behaviour of /rest/v1/targettypes POST requests.
     */
    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    void createTargetTypes() throws Exception {
        String typeName = "TestTypePOST";
        final List<Create> types = buildTestTargetTypesWithoutDsTypes(typeName, 5);

        runPostTargetTypeAndVerify(types);
    }

    /**
     * Checks the correct behaviour of /rest/v1/targettypes/{ID}/compatibledistributionsettypes POST requests.
     */
    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    void addDistributionSetTypeToTargetType() throws Exception {
        String typeName = "TestTypeAddDs";
        TargetType testType = createTestTargetTypeInDB(typeName);
        assertThat(testType.getOptLockRevision()).isEqualTo(1);

        mvc.perform(post(TARGETTYPE_DSTYPES_ENDPOINT, testType.getId())
                        .content("[{\"id\":" + standardDsType.getId() + "}]").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        testType = targetTypeManagement.find(testType.getId()).get();
        assertThat(testType.getLastModifiedBy()).isEqualTo(TEST_USER);
        assertThat(testType.getOptLockRevision()).isEqualTo(2);
        assertThat(testType.getDistributionSetTypes()).containsExactly(standardDsType);
    }

    /**
     * Checks the correct behaviour of /rest/v1/targettypes/{ID}/compatibledistributionsettypes GET requests.
     */
    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    void getDistributionSetsOfTargetType() throws Exception {
        String typeName = "TestTypeGetDs";
        final TargetType testType = createTestTargetTypeInDB(typeName, Set.of(standardDsType));

        mvc.perform(get(TARGETTYPE_DSTYPES_ENDPOINT, testType.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$[0].name", equalTo(standardDsType.getName())))
                .andExpect(jsonPath("$[0].description", equalTo(standardDsType.getDescription())))
                .andExpect(jsonPath("$[0].key", equalTo("test_default_ds_type")))
                .andExpect(jsonPath("$[0]._links.self.href",
                        equalTo("http://localhost/rest/v1/distributionsettypes/" + standardDsType.getId())));
    }

    /**
     * Checks the correct behaviour of /rest/v1/targettypes/{ID}/compatibledistributionsettypes/{ID} GET requests.
     */
    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    void getDistributionSetOfTargetTypeReturnsNotAllowed() throws Exception {
        String typeName = "TestTypeAddDs";
        final TargetType testType = createTestTargetTypeInDB(typeName);

        mvc.perform(get(TARGETTYPE_DSTYPE_SINGLE_ENDPOINT, testType.getId(), standardDsType.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

    }

    /**
     * Checks the correct behaviour of /rest/v1/targettypes/{ID}/compatibledistributionsettypes/{ID} DELETE requests.
     */
    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    void removeDsTypeFromTargetType() throws Exception {
        String typeName = "TestTypeRemoveDs";
        TargetType testType = createTestTargetTypeInDB(typeName, Set.of(standardDsType));

        mvc.perform(delete(TARGETTYPE_DSTYPE_SINGLE_ENDPOINT, testType.getId(), standardDsType.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        testType = targetTypeManagement.find(testType.getId()).get();
        assertThat(testType.getLastModifiedBy()).isEqualTo(TEST_USER);
        assertThat(testType.getOptLockRevision()).isEqualTo(2);
        assertThat(testType.getDistributionSetTypes()).isEmpty();
    }

    /**
     * Checks the correct behaviour of /rest/v1/distributionsettypes/{ID} DELETE requests.
     */
    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    void deletingDsTypeRemovesAssignmentFromTargetType() throws Exception {
        TargetType testType = createTestTargetTypeInDB("TestTypeRemoveDs", Set.of(standardDsType));
        assertThat(testType.getDistributionSetTypes()).hasSize(1);
        assertThat(distributionSetTypeManagement.findByKey(standardDsType.getKey())).isNotEmpty();

        mvc.perform(delete(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/" + standardDsType.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        testType = targetTypeManagement.find(testType.getId()).get();
        assertThat(testType.getLastModifiedBy()).isEqualTo(TEST_USER);
        assertThat(testType.getOptLockRevision()).isEqualTo(2);
        assertThat(testType.getDistributionSetTypes()).isEmpty();
        assertThat(distributionSetTypeManagement.findByKey(standardDsType.getKey())).isEmpty();
    }

    /**
     * Checks the correct behaviour of /rest/v1/targettypes/{ID} DELETE requests - Deletion when not in use.
     */
    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    void deleteTargetTypeUnused() throws Exception {
        String typeName = "TestTypeUnusedDelete";
        final TargetType testType = createTestTargetTypeInDB(typeName);

        assertThat(targetTypeManagement.count()).isEqualTo(1);

        mvc.perform(delete(TARGETTYPE_SINGLE_ENDPOINT, testType.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        assertThat(targetTypeManagement.count()).isZero();
    }

    /**
     * Checks the correct behaviour of /rest/v1/targettypes/{ID} DELETE requests - Deletion not possible when in use.
     */
    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    void deleteTargetTypeUsed() throws Exception {
        String typeName = "TestTypeUsedDelete";
        final TargetType testType = createTestTargetTypeInDB(typeName);

        targetManagement.create(TargetManagement.Create.builder()
                .controllerId("target").name("TargetOfTestType").description("target description").targetType(testType).build());

        assertThat(targetTypeManagement.count()).isEqualTo(1);
        assertThat(targetManagement.count()).isEqualTo(1);

        mvc.perform(delete(TARGETTYPE_SINGLE_ENDPOINT, testType.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isConflict());

        assertThat(targetManagement.count()).isEqualTo(1);
        assertThat(targetTypeManagement.count()).isEqualTo(1);
    }

    /**
     * Ensures that target type deletion request to API on an entity that does not exist results in NOT_FOUND.
     */
    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    void deleteTargetTypeThatDoesNotExistLeadsToNotFound() throws Exception {
        mvc.perform(delete(TARGETTYPE_SINGLE_ENDPOINT, 1234))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    /**
     * Tests the update of the deletion flag. It is verified that the target type can't be marked as deleted through update operation.
     */
    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    void updateTargetTypeDeletedFlag() throws Exception {
        String typeName = "TestTypePUT";
        final TargetType testType = createTestTargetTypeInDB(typeName);

        final String body = new JSONObject().put("id", testType.getId()).put("deleted", true).toString();

        mvc.perform(
                        put(TARGETTYPE_SINGLE_ENDPOINT, testType.getId()).content(body).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(testType.getId().intValue())))
                .andExpect(jsonPath("$.deleted", equalTo(false))); // don't delete with update
    }

    /**
     * Ensures that the server is behaving as expected on invalid requests (wrong media type, wrong ID etc.).
     */
    @Test
    void invalidRequestsOnTargetTypesResource() throws Exception {
        String typeName = "TestTypeInvalidReq";
        final TargetType testType = createTestTargetTypeInDB(typeName, Set.of(standardDsType));

        // target type does not exist
        mvc.perform(get(TARGETTYPE_SINGLE_ENDPOINT, 12345678))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
        mvc.perform(get(TARGETTYPE_DSTYPES_ENDPOINT, 123456789))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
        mvc.perform(delete(TARGETTYPE_SINGLE_ENDPOINT, 123456789))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // target types at creation time invalid
        final TargetType testNewType = createTestTargetTypeInDB(typeName + "Another", Set.of(standardDsType));

        mvc.perform(post(TARGETTYPES_ENDPOINT).content(toJson(List.of(testNewType)))
                        .contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isUnsupportedMediaType());

        // bad request - no content
        mvc.perform(post(TARGETTYPES_ENDPOINT).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        // bad request - bad content
        mvc.perform(post(TARGETTYPES_ENDPOINT).content("sdfjsdlkjfskdjf".getBytes())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        // Missing mandatory field name
        mvc.perform(post(TARGETTYPES_ENDPOINT).content("[{\"description\":\"Desc123\"}]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        final Create tooLongName = Create.builder().name(randomString(NamedEntity.NAME_MAX_SIZE + 1)).build();
        mvc.perform(post(TARGETTYPES_ENDPOINT).content(toJson(List.of(tooLongName)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        // ds types
        mvc.perform(get(TARGETTYPE_DSTYPES_ENDPOINT, testType.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        mvc.perform(delete(TARGETTYPE_DSTYPE_SINGLE_ENDPOINT, testType.getId(), 565765))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        mvc.perform(post(TARGETTYPE_DSTYPES_ENDPOINT, testType.getId()).content("{\"id\":1}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        mvc.perform(post(TARGETTYPE_DSTYPES_ENDPOINT, testType.getId()).content("[{\"id\":44456}]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // not allowed methods
        mvc.perform(put(TARGETTYPES_ENDPOINT))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(delete(TARGETTYPES_ENDPOINT))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(put(TARGETTYPE_DSTYPES_ENDPOINT, testType.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(delete(TARGETTYPE_DSTYPES_ENDPOINT, testType.getId(), 565765))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(put(TARGETTYPE_DSTYPE_SINGLE_ENDPOINT, testType.getId(), 565765))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(post(TARGETTYPE_DSTYPE_SINGLE_ENDPOINT, testType.getId(), 565765)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(get(TARGETTYPE_DSTYPE_SINGLE_ENDPOINT, testType.getId(), 565765))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
    }

    /**
     * Search request of target types.
     */
    @Test
    void searchTargetTypeRsql() throws Exception {
        targetTypeManagement.create(Create.builder().name("TestName123").build());
        targetTypeManagement.create(Create.builder().name("TestName1234").build());

        final String rsqlFindLikeDs1OrDs2 = "name==TestName123,name==TestName1234";

        mvc.perform(get(TARGETTYPES_ENDPOINT + "?q=" + rsqlFindLikeDs1OrDs2))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("size", equalTo(2)))
                .andExpect(jsonPath("total", equalTo(2)))
                .andExpect(jsonPath("content[0].name", equalTo("TestName123")))
                .andExpect(jsonPath("content[1].name", equalTo("TestName1234")));
    }

    /**
     * Verifies quota enforcement for /rest/v1/targettypes/{ID}/compatibledistributionsettypes POST requests.
     */
    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    void assignDistributionSetTypeToTargetTypeUntilQuotaExceeded() throws Exception {
        final TargetType testType = createTestTargetTypeInDB("TestTypeQuota");

        // create distribution set types
        final int maxDistributionSetTypes = quotaManagement.getMaxDistributionSetTypesPerTargetType();
        final List<Long> dsTypeIds = new ArrayList<>();
        for (int i = 0; i < maxDistributionSetTypes + 1; ++i) {
            final DistributionSetType ds = testdataFactory.findOrCreateDistributionSetType("dsType_" + i,
                    "dsType_" + i);
            dsTypeIds.add(ds.getId());
        }

        // verify quota enforcement for distribution set types
        mvc.perform(post(TARGETTYPE_DSTYPES_ENDPOINT, testType.getId())
                        .content(toJson(dsTypeIds.subList(0, dsTypeIds.size() - 1).stream().map(MgmtId::new).toList()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        mvc.perform(post(TARGETTYPE_DSTYPES_ENDPOINT, testType.getId())
                        .content("[{\"id\":" + dsTypeIds.get(dsTypeIds.size() - 1) + "}]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.exceptionClass", equalTo(AssignmentQuotaExceededException.class.getName())))
                .andExpect(jsonPath("$.errorCode", equalTo(SpServerError.SP_QUOTA_EXCEEDED.getKey())));
    }

    private Create buildTestTargetTypeBody(final String name) {
        return prepareTestTargetType(name, null);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Create prepareTestTargetType(final String name, final Collection<DistributionSetType> dsTypes) {
        final Create.CreateBuilder create = Create.builder().name(name)
                .description("Description of the test type").colour("#aaaaaa");
        if (dsTypes != null && !dsTypes.isEmpty()) {
            create.distributionSetTypes(Set.of(standardDsType.getId()));
        }
        return create.build();
    }

    private List<? extends TargetType> createTestTargetTypesInDB(final String namePrefix, final int count) {
        return testdataFactory.createTargetTypes(namePrefix, count);
    }

    private TargetType createTestTargetTypeInDB(final String name) {
        return testdataFactory.findOrCreateTargetType(name);
    }

    private TargetType createTestTargetTypeInDB(final String name, final Set<DistributionSetType> dsTypes) {
        TargetType targetType = testdataFactory.createTargetType(name, dsTypes);
        assertThat(targetType.getOptLockRevision()).isEqualTo(1);
        return targetType;
    }

    private List<Create> buildTestTargetTypesWithoutDsTypes(final String namePrefix, final int count) {
        final List<Create> types = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            types.add(buildTestTargetTypeBody(namePrefix + index));
        }
        return types;
    }

    private void runPostTargetTypeAndVerify(final List<Create> types) throws Exception {
        int size = types.size();
        ResultActions resultActions = mvc
                .perform(post(TARGETTYPES_ENDPOINT).content(toJson(types))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print());

        for (int index = 0; index < size; index++) {
            resultActions.andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$[" + index + "].id").exists())
                    .andExpect(jsonPath("$[" + index + "].name", startsWith("TestTypePOST")))
                    .andExpect(jsonPath("$[" + index + "].colour", hasToString("#aaaaaa")))
                    .andExpect(jsonPath("$[" + index + "].description",
                            equalTo("Description of the test type")))
                    .andExpect(jsonPath("$[" + index + "].createdBy", equalTo(TEST_USER)))
                    .andExpect(jsonPath("$[" + index + "].createdAt").exists())
                    .andExpect(jsonPath("$[" + index + "].lastModifiedBy", equalTo(TEST_USER)))
                    .andExpect(jsonPath("$[" + index + "].lastModifiedAt").exists())
                    .andExpect(jsonPath("$[" + index + "].deleted", equalTo(false)))
                    .andExpect(jsonPath("$[" + index + "].key", startsWith("TestTypePOST")))
                    .andExpect(jsonPath("$[" + index + "]._links.self.href",
                            startsWith("http://localhost/rest/v1/targettypes/")))
                    .andExpect(
                            jsonPath("$[" + index + "]._links.compatibledistributionsettypes.href")
                                    .doesNotExist());
        }
        MvcResult mvcResult = resultActions.andReturn();

        for (int index = 0; index < size; index++) {
            String name = "TestTypePOST" + index;
            final TargetType created = findTargetTypeByName(name);

            assertThat(JsonPath.compile("$[ ?(@.name=='" + name + "') ].id")
                    .read(mvcResult.getResponse().getContentAsString()).toString()).contains(String.valueOf(created.getId()));
            assertThat(JsonPath.compile("$[ ?(@.name=='" + name + "') ]._links.self.href")
                    .read(mvcResult.getResponse().getContentAsString()).toString()).contains("/" + created.getId());
        }

        assertThat(targetTypeManagement.count()).isEqualTo(size);
    }

}
