/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.jayway.jsonpath.JsonPath;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.util.Lists;
import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeCreate;
import org.eclipse.hawkbit.repository.builder.TargetTypeCreate;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

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

/**
 * Spring MVC Tests against the MgmtTargetTypeResource.
 *
 */
@Feature("Component Tests - Management API")
@Story("Target Type Resource")
class MgmtTargetTypeResourceTest extends AbstractManagementApiIntegrationTest {

    private final static String TARGETTYPES_ENDPOINT = MgmtRestConstants.TARGETTYPE_V1_REQUEST_MAPPING;
    private final static String TARGETTYPE_SINGLE_ENDPOINT = MgmtRestConstants.TARGETTYPE_V1_REQUEST_MAPPING
            + "/{typeid}";
    private final static String TARGETTYPE_DSTYPES_ENDPOINT = TARGETTYPE_SINGLE_ENDPOINT + "/"
            + MgmtRestConstants.TARGETTYPE_V1_DS_TYPES;
    private final static String TARGETTYPE_DSTYPE_SINGLE_ENDPOINT = TARGETTYPE_DSTYPES_ENDPOINT + "/{dstypeid}";

    private final static String TEST_USER = "targetTypeTester";


    @Test
    @WithUser(principal = "targetTypeTester", allSpPermissions = true, removeFromAllPermission = { SpPermission.READ_TARGET })
    @Description("GET targettypes returns Forbidden when permission is missing")
    void getTargetTypesWithoutPermission() throws Exception {
        mvc.perform(get(TARGETTYPES_ENDPOINT).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isForbidden());
    }

    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/targettypes/{id} GET request.")
    void getTargetType() throws Exception {
        String typeName = "TestTypeGET";
        TargetType testType = createTestTargetTypeInDB(typeName);
        Long typeId = testType.getId();

        mvc.perform(get(TARGETTYPE_SINGLE_ENDPOINT, typeId).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id", is(typeId), Long.class)).andExpect(jsonPath("$.name", equalTo(typeName)))
                .andExpect(jsonPath("$.colour", is("#000000")))
                .andExpect(jsonPath("$.description", equalTo(typeName + " description")))
                .andExpect(jsonPath("$.createdBy", equalTo(TEST_USER)))
                .andExpect(jsonPath("$.createdAt", equalTo(testType.getCreatedAt())))
                .andExpect(jsonPath("$.lastModifiedBy", equalTo(TEST_USER)))
                .andExpect(jsonPath("$.lastModifiedAt", equalTo(testType.getLastModifiedAt())))
                .andExpect(jsonPath("$._links.self.href", equalTo("http://localhost/rest/v1/targettypes/" + typeId)))
                .andExpect(jsonPath("$.deleted").doesNotExist())
                .andExpect(jsonPath("$.key").doesNotExist())
                .andExpect(jsonPath("$._links.compatibledistributionsettypes.href",
                        equalTo("http://localhost/rest/v1/targettypes/" + typeId + "/compatibledistributionsettypes")));
    }

    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/targettypes GET requests.")
    void getTargetTypes() throws Exception {
        String typeName = "TestTypeGET";
        int count = 5;
        List<TargetType> testTypes = createTestTargetTypesInDB(typeName, count);

        ResultActions resultActions = mvc.perform(get(TARGETTYPES_ENDPOINT).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print());

        for (int index = 0; index < count; index++) {
            Long typeId = testTypes.get(index).getId();
            resultActions.andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.content.[?(@.id=='" + typeId + "')].id", contains(typeId.intValue())))
                    .andExpect(jsonPath("$.content.[?(@.id=='" + typeId + "')].name", contains(typeName + index)))
                    .andExpect(jsonPath("$.content.[?(@.id=='" + typeId + "')].colour", contains("#000000")))
                    .andExpect(jsonPath("$.content.[?(@.id=='" + typeId + "')].description",
                            contains(typeName + " description")))
                    .andExpect(jsonPath("$.content.[?(@.id=='" + typeId + "')].createdBy", contains(TEST_USER)))
                    .andExpect(jsonPath("$.content.[?(@.id=='" + typeId + "')].createdAt",
                            contains(testTypes.get(index).getCreatedAt())))
                    .andExpect(jsonPath("$.content.[?(@.id=='" + typeId + "')].lastModifiedBy", contains(TEST_USER)))
                    .andExpect(jsonPath("$.content.[?(@.id=='" + typeId + "')].lastModifiedAt",
                            contains(testTypes.get(index).getLastModifiedAt())))
                    .andExpect(jsonPath("$.content.[?(@.id=='" + typeId + "')].deleted").doesNotExist())
                    .andExpect(jsonPath("$.content.[?(@.id=='" + typeId + "')].key").doesNotExist())
                    .andExpect(jsonPath("$.content.[?(@.id=='" + typeId + "')]._links.self.href",
                            contains("http://localhost/rest/v1/targettypes/" + typeId)))
                    .andExpect(
                            jsonPath("$.content.[?(@.id=='" + typeId + "')]._links.compatibledistributionsettypes.href")
                                    .doesNotExist())
                    .andExpect(jsonPath("$.total", equalTo(count))).andExpect(jsonPath("$.size", equalTo(count)));
        }
    }

    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/targettypes GET requests without prior created target types.")
    void getDefaultTargetTypes() throws Exception {

        // 0 types overall (no default types are created)
        final int types = 0;
        mvc.perform(get(TARGETTYPES_ENDPOINT)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(types)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(types)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(types)));
    }

    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/targettypes GET requests with sorting by name.")
    void getTargetTypesSortedByName() throws Exception {
        String typeNameA = "ATestTypeGETsorted";
        String typeNameB = "BTestTypeGETsorted";
        String typeNameC = "CTestTypeGETsorted";
        TargetType testTypeB = createTestTargetTypeInDB(typeNameB);
        TargetType testTypeC = createTestTargetTypeInDB(typeNameC);
        TargetType testTypeA = createTestTargetTypeInDB(typeNameA);

        testTypeA = targetTypeManagement
                .update(entityFactory.targetType().update(testTypeA.getId()).description("Updated description"));

        // descending
        mvc.perform(get(TARGETTYPES_ENDPOINT).accept(MediaType.APPLICATION_JSON)
                .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "name:DESC")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content.[0].id", equalTo(testTypeC.getId().intValue())))
                .andExpect(jsonPath("$.content.[0].name", equalTo(typeNameC)))
                .andExpect(jsonPath("$.content.[0].colour", equalTo("#000000")))
                .andExpect(jsonPath("$.content.[0].description", equalTo(typeNameC + " description")))
                .andExpect(jsonPath("$.content.[0].createdBy", equalTo(TEST_USER)))
                .andExpect(jsonPath("$.content.[0].createdAt", equalTo(testTypeC.getCreatedAt())))
                .andExpect(jsonPath("$.content.[0].lastModifiedBy", equalTo(TEST_USER)))
                .andExpect(jsonPath("$.content.[0].lastModifiedAt", equalTo(testTypeC.getLastModifiedAt())))
                .andExpect(jsonPath("$.content.[0].deleted").doesNotExist())
                .andExpect(jsonPath("$.content.[0].key").doesNotExist())
                .andExpect(jsonPath("$.content.[0]._links.self.href",
                        equalTo("http://localhost/rest/v1/targettypes/" + testTypeC.getId())))
                .andExpect(jsonPath("$.content.[0]._links.compatibledistributionsettypes.href").doesNotExist())
                .andExpect(jsonPath("$.total", equalTo(3))).andExpect(jsonPath("$.size", equalTo(3)))
                .andExpect(jsonPath("$.content.[1].name", equalTo(typeNameB)))
                .andExpect(jsonPath("$.content.[2].name", equalTo(typeNameA)));

        // ascending
        mvc.perform(get(TARGETTYPES_ENDPOINT).accept(MediaType.APPLICATION_JSON)
                .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "name:ASC")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content.[0].id", equalTo(testTypeA.getId().intValue())))
                .andExpect(jsonPath("$.content.[0].name", equalTo(typeNameA)))
                .andExpect(jsonPath("$.content.[0].description", equalTo("Updated description")))
                .andExpect(jsonPath("$.content.[0].colour", equalTo("#000000")))
                .andExpect(jsonPath("$.content.[0].createdBy", equalTo(TEST_USER)))
                .andExpect(jsonPath("$.content.[0].createdAt", equalTo(testTypeA.getCreatedAt())))
                .andExpect(jsonPath("$.content.[0].lastModifiedBy", equalTo(TEST_USER)))
                .andExpect(jsonPath("$.content.[0].lastModifiedAt", equalTo(testTypeA.getLastModifiedAt())))
                .andExpect(jsonPath("$.content.[0].deleted").doesNotExist())
                .andExpect(jsonPath("$.content.[0].key").doesNotExist())
                .andExpect(jsonPath("$.content.[0]._links.self.href",
                        equalTo("http://localhost/rest/v1/targettypes/" + testTypeA.getId())))
                .andExpect(jsonPath("$.content.[0]._links.compatibledistributionsettypes.href").doesNotExist())
                .andExpect(jsonPath("$.total", equalTo(3))).andExpect(jsonPath("$.size", equalTo(3)))
                .andExpect(jsonPath("$.content.[1].name", equalTo(typeNameB)))
                .andExpect(jsonPath("$.content.[2].name", equalTo(typeNameC)));

    }

    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/targettypes GET requests with paging.")
    void getTargetTypesWithPagingLimitRequestParameter() throws Exception {
        final String typePrefix = "TestTypeGETPaging";
        final int count = 10;
        final int limit = 3;
        createTestTargetTypesInDB(typePrefix, count);

        mvc.perform(get(TARGETTYPES_ENDPOINT).param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT,
                String.valueOf(limit))).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(count)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(limit)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(limit)));
    }

    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/targettypes GET requests with paging and offset.")
    void getTargetTypesWithPagingLimitAndOffsetRequestParameter() throws Exception {
        final int count = 10;
        final int offset = 2;
        final int expectedSize = count - offset;
        final String typePrefix = "TestTypeGETPaging";
        createTestTargetTypesInDB(typePrefix, count);

        mvc.perform(get(TARGETTYPES_ENDPOINT)
                .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(offset))
                .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(count)))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(count)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(expectedSize)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(expectedSize)));
    }

    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/targettypes/{ID} PUT requests.")
    void updateTargetType() throws Exception {
        String typeName = "TestTypePUT";
        final TargetType testType = createTestTargetTypeInDB(typeName);
        final String body = new JSONObject().put("id", testType.getId()).put("description", "updated description")
                .put("name", "TestTypePUTupdated").put("colour", "#ffffff").toString();

        mvc.perform(
                put(TARGETTYPE_SINGLE_ENDPOINT, testType.getId()).content(body).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(testType.getId().intValue())))
                .andExpect(jsonPath("$.description", equalTo("updated description")))
                .andExpect(jsonPath("$.name", equalTo("TestTypePUTupdated")))
                .andExpect(jsonPath("$.colour", equalTo("#ffffff"))).andReturn();
    }

    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/targettypes/{id} GET requests.")
    void getUpdatedTargetType() throws Exception {
        TargetType testType = createTestTargetTypeInDB("TestTypeGET");
        String typeNameUpdated = "TestTypeGETupdated";
        testType = targetTypeManagement.update(entityFactory.targetType().update(testType.getId()).name(typeNameUpdated)
                .description("Updated Description").colour("#ffffff"));

        mvc.perform(get(TARGETTYPE_SINGLE_ENDPOINT, testType.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.name", equalTo(typeNameUpdated)))
                .andExpect(jsonPath("$.description", equalTo("Updated Description")))
                .andExpect(jsonPath("$.colour", equalTo("#ffffff")))
                .andExpect(jsonPath("$.createdBy", equalTo(TEST_USER)))
                .andExpect(jsonPath("$.createdAt", equalTo(testType.getCreatedAt())))
                .andExpect(jsonPath("$.lastModifiedBy", equalTo(TEST_USER)))
                .andExpect(jsonPath("$.lastModifiedAt", equalTo(testType.getLastModifiedAt())))
                .andExpect(jsonPath("$.deleted").doesNotExist())
                .andExpect(jsonPath("$.key").doesNotExist());
    }

    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/targettypes POST requests.")
    void createTargetTypes() throws Exception {
        String typeName = "TestTypePOST";
        final List<TargetType> types = buildTestTargetTypesWithoutDsTypes(typeName, 5);

        runPostTargetTypeAndVerify(types);
    }

    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/targettypes/{ID}/compatibledistributionsettypes POST requests.")
    void addDistributionSetTypeToTargetType() throws Exception {
        String typeName = "TestTypeAddDs";
        TargetType testType = createTestTargetTypeInDB(typeName);
        assertThat(testType.getOptLockRevision()).isEqualTo(1);

        mvc.perform(post(TARGETTYPE_DSTYPES_ENDPOINT, testType.getId())
                .content("[{\"id\":" + standardDsType.getId() + "}]").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        testType = targetTypeManagement.get(testType.getId()).get();
        assertThat(testType.getLastModifiedBy()).isEqualTo(TEST_USER);
        assertThat(testType.getOptLockRevision()).isEqualTo(2);
        assertThat(testType.getCompatibleDistributionSetTypes()).containsExactly(standardDsType);
    }

    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/targettypes/{ID}/compatibledistributionsettypes GET requests.")
    void getDistributionSetsOfTargetType() throws Exception {
        String typeName = "TestTypeGetDs";
        final TargetType testType = createTestTargetTypeInDB(typeName, Collections.singletonList(standardDsType));

        mvc.perform(get(TARGETTYPE_DSTYPES_ENDPOINT, testType.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$[0].name", equalTo(standardDsType.getName())))
                .andExpect(jsonPath("$[0].description", equalTo(standardDsType.getDescription())))
                .andExpect(jsonPath("$[0].key", equalTo("test_default_ds_type")))
                .andExpect(jsonPath("$[0]._links.self.href",
                        equalTo("http://localhost/rest/v1/distributionsettypes/" + standardDsType.getId())));
    }

    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/targettypes/{ID}/compatibledistributionsettypes/{ID} GET requests.")
    void getDistributionSetOfTargetTypeReturnsNotAllowed() throws Exception {
        String typeName = "TestTypeAddDs";
        final TargetType testType = createTestTargetTypeInDB(typeName);

        mvc.perform(get(TARGETTYPE_DSTYPE_SINGLE_ENDPOINT, testType.getId(), standardDsType.getId())
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

    }

    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/targettypes/{ID}/compatibledistributionsettypes/{ID} DELETE requests.")
    void removeDsTypeFromTargetType() throws Exception {
        String typeName = "TestTypeRemoveDs";
        TargetType testType = createTestTargetTypeInDB(typeName, Collections.singletonList(standardDsType));

        mvc.perform(delete(TARGETTYPE_DSTYPE_SINGLE_ENDPOINT, testType.getId(), standardDsType.getId())
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        testType = targetTypeManagement.get(testType.getId()).get();
        assertThat(testType.getLastModifiedBy()).isEqualTo(TEST_USER);
        assertThat(testType.getOptLockRevision()).isEqualTo(2);
        assertThat(testType.getCompatibleDistributionSetTypes()).isEmpty();
    }

    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/distributionsettypes/{ID} DELETE requests.")
    void deletingDsTypeRemovesAssignmentFromTargetType() throws Exception {
        String typeName = "TestTypeRemoveDs";
        TargetType testType = createTestTargetTypeInDB(typeName, Collections.singletonList(standardDsType));
        assertThat(testType.getCompatibleDistributionSetTypes()).hasSize(1);
        assertThat(distributionSetTypeManagement.getByKey(standardDsType.getKey())).isNotEmpty();

        mvc.perform(delete(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/" + standardDsType.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        testType = targetTypeManagement.get(testType.getId()).get();
        assertThat(testType.getLastModifiedBy()).isEqualTo(TEST_USER);
        assertThat(testType.getOptLockRevision()).isEqualTo(2);
        assertThat(testType.getCompatibleDistributionSetTypes()).isEmpty();
        assertThat(distributionSetTypeManagement.getByKey(standardDsType.getKey())).isEmpty();
    }

    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/targettypes/{ID} DELETE requests - Deletion when not in use.")
    void deleteTargetTypeUnused() throws Exception {
        String typeName = "TestTypeUnusedDelete";
        final TargetType testType = createTestTargetTypeInDB(typeName);

        assertThat(targetTypeManagement.count()).isEqualTo(1);

        mvc.perform(delete(TARGETTYPE_SINGLE_ENDPOINT, testType.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        assertThat(targetTypeManagement.count()).isZero();
    }

    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /rest/v1/targettypes/{ID} DELETE requests - Deletion not possible when in use.")
    void deleteTargetTypeUsed() throws Exception {
        String typeName = "TestTypeUsedDelete";
        final TargetType testType = createTestTargetTypeInDB(typeName);

        targetManagement.create(entityFactory.target().create().controllerId("target").name("TargetOfTestType")
                .description("target description").targetType(testType.getId()));

        assertThat(targetTypeManagement.count()).isEqualTo(1);
        assertThat(targetManagement.count()).isEqualTo(1);

        mvc.perform(delete(TARGETTYPE_SINGLE_ENDPOINT, testType.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isConflict());

        assertThat(targetManagement.count()).isEqualTo(1);
        assertThat(targetTypeManagement.count()).isEqualTo(1);
    }

    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    @Description("Ensures that target type deletion request to API on an entity that does not exist results in NOT_FOUND.")
    void deleteTargetTypeThatDoesNotExistLeadsToNotFound() throws Exception {
        mvc.perform(delete(TARGETTYPE_SINGLE_ENDPOINT, 1234)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    @Description("Tests the update of the deletion flag. It is verified that the target type can't be marked as deleted through update operation.")
    void updateTargetTypeDeletedFlag() throws Exception {
        String typeName = "TestTypePUT";
        final TargetType testType = createTestTargetTypeInDB(typeName);

        final String body = new JSONObject().put("id", testType.getId()).put("deleted", true).toString();

        mvc.perform(
                put(TARGETTYPE_SINGLE_ENDPOINT, testType.getId()).content(body).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(testType.getId().intValue())))
                .andExpect(jsonPath("$.deleted").doesNotExist());
    }

    @Test
    @Description("Ensures that the server is behaving as expected on invalid requests (wrong media type, wrong ID etc.).")
    void invalidRequestsOnTargetTypesResource() throws Exception {
        String typeName = "TestTypeInvalidReq";
        final TargetType testType = createTestTargetTypeInDB(typeName, Collections.singletonList(standardDsType));

        // target type does not exist
        mvc.perform(get(TARGETTYPE_SINGLE_ENDPOINT, 12345678)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
        mvc.perform(get(TARGETTYPE_DSTYPES_ENDPOINT, 123456789)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
        mvc.perform(delete(TARGETTYPE_SINGLE_ENDPOINT, 123456789)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // target types at creation time invalid
        final TargetType testNewType = createTestTargetTypeInDB(typeName + "Another",
                Collections.singletonList(standardDsType));

        mvc.perform(post(TARGETTYPES_ENDPOINT).content(JsonBuilder.targetTypes(Collections.singletonList(testNewType)))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isUnsupportedMediaType());

        // bad request - no content
        mvc.perform(post(TARGETTYPES_ENDPOINT).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        // bad request - bad content
        mvc.perform(post(TARGETTYPES_ENDPOINT).content("sdfjsdlkjfskdjf".getBytes())
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        // Missing mandatory field name
        mvc.perform(post(TARGETTYPES_ENDPOINT).content("[{\"description\":\"Desc123\"}]")
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        final TargetType tooLongName = entityFactory.targetType().create()
                .name(RandomStringUtils.randomAlphanumeric(NamedEntity.NAME_MAX_SIZE + 1)).build();
        mvc.perform(post(TARGETTYPES_ENDPOINT).content(JsonBuilder.targetTypes(Collections.singletonList(tooLongName)))
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        // ds types
        mvc.perform(get(TARGETTYPE_DSTYPES_ENDPOINT, testType.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        mvc.perform(delete(TARGETTYPE_DSTYPE_SINGLE_ENDPOINT, testType.getId(), 565765))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        mvc.perform(post(TARGETTYPE_DSTYPES_ENDPOINT, testType.getId()).content("{\"id\":1}")
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        mvc.perform(post(TARGETTYPE_DSTYPES_ENDPOINT, testType.getId()).content("[{\"id\":44456}]")
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // not allowed methods
        mvc.perform(put(TARGETTYPES_ENDPOINT)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(delete(TARGETTYPES_ENDPOINT)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(put(TARGETTYPE_DSTYPES_ENDPOINT, testType.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(delete(TARGETTYPE_DSTYPES_ENDPOINT, testType.getId(), 565765)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(put(TARGETTYPE_DSTYPE_SINGLE_ENDPOINT, testType.getId(), 565765))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        mvc.perform(post(TARGETTYPE_DSTYPE_SINGLE_ENDPOINT, testType.getId(), 565765)
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(get(TARGETTYPE_DSTYPE_SINGLE_ENDPOINT, testType.getId(), 565765))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());
    }

    @Test
    @Description("Search request of target types.")
    void searchTargetTypeRsql() throws Exception {
        targetTypeManagement.create(entityFactory.targetType().create().name("TestName123"));
        targetTypeManagement.create(entityFactory.targetType().create().name("TestName1234"));

        final String rsqlFindLikeDs1OrDs2 = "name==TestName123,name==TestName1234";

        mvc.perform(get(TARGETTYPES_ENDPOINT + "?q=" + rsqlFindLikeDs1OrDs2)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("size", equalTo(2)))
                .andExpect(jsonPath("total", equalTo(2))).andExpect(jsonPath("content[0].name", equalTo("TestName123")))
                .andExpect(jsonPath("content[1].name", equalTo("TestName1234")));
    }

    @Test
    @WithUser(principal = TEST_USER, allSpPermissions = true)
    @Description("Verifies quota enforcement for /rest/v1/targettypes/{ID}/compatibledistributionsettypes POST requests.")
    void assignDistributionSetTypeToTargetTypeUntilQuotaExceeded() throws Exception {
        final TargetType testType = createTestTargetTypeInDB("TestTypeQuota");

        // create distribution set types
        final int maxDistributionSetTypes = quotaManagement.getMaxDistributionSetTypesPerTargetType();
        final List<Long> dsTypeIds = Lists.newArrayList();
        for (int i = 0; i < maxDistributionSetTypes + 1; ++i) {
            final DistributionSetType ds = testdataFactory.findOrCreateDistributionSetType("dsType_" + i,
                    "dsType_" + i);
            dsTypeIds.add(ds.getId());
        }

        // verify quota enforcement for distribution set types
        mvc.perform(post(TARGETTYPE_DSTYPES_ENDPOINT, testType.getId())
                .content(JsonBuilder.ids(dsTypeIds.subList(0, dsTypeIds.size() - 1)))
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        mvc.perform(post(TARGETTYPE_DSTYPES_ENDPOINT, testType.getId())
                .content("[{\"id\":" + dsTypeIds.get(dsTypeIds.size() - 1) + "}]")
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.exceptionClass", equalTo(AssignmentQuotaExceededException.class.getName())))
                .andExpect(jsonPath("$.errorCode", equalTo(SpServerError.SP_QUOTA_EXCEEDED.getKey())));
    }

    @Step
    private TargetType buildTestTargetTypeBody(String name) {
        return prepareTestTargetType(name, null).build();
    }

    private TargetTypeCreate prepareTestTargetType(String name, Collection<DistributionSetType> dsTypes) {
        TargetTypeCreate create = entityFactory.targetType().create().name(name)
                .description("Description of the test type").colour("#aaaaaa");
        if (dsTypes != null && !dsTypes.isEmpty()) {
            create.compatible(Collections.singletonList(standardDsType.getId()));
        }
        return create;
    }

    @Step
    private List<TargetType> createTestTargetTypesInDB(String namePrefix, int count) {
        return testdataFactory.createTargetTypes(namePrefix, count);
    }

    @Step
    private TargetType createTestTargetTypeInDB(String name) {
        return testdataFactory.findOrCreateTargetType(name);
    }

    @Step
    private TargetType createTestTargetTypeInDB(String name, List<DistributionSetType> dsTypes) {
        TargetType targetType = testdataFactory.createTargetType(name, dsTypes);
        assertThat(targetType.getOptLockRevision()).isEqualTo(1);
        return targetType;
    }

    @Step
    private List<TargetType> buildTestTargetTypesWithoutDsTypes(String namePrefix, int count) {
        final List<TargetType> types = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            types.add(buildTestTargetTypeBody(namePrefix + index));
        }
        return types;
    }

    @Step
    private void runPostTargetTypeAndVerify(final List<TargetType> types) throws Exception {
        int size = types.size();
        ResultActions resultActions = mvc
                .perform(post(TARGETTYPES_ENDPOINT).content(JsonBuilder.targetTypes(types))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print());

        for (int index = 0; index < size; index++) {
            resultActions.andExpect(status().isCreated()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$[" + index + "].id").exists())
                    .andExpect(jsonPath("$[" + index + "].name", startsWith("TestTypePOST")))
                    .andExpect(jsonPath("$[" + index + "].colour", hasToString("#aaaaaa")))
                    .andExpect(jsonPath("$[" + index + "].description",
                            equalTo("Description of the test type")))
                    .andExpect(jsonPath("$[" + index + "].createdBy", equalTo(TEST_USER)))
                    .andExpect(jsonPath("$[" + index + "].createdAt").exists())
                    .andExpect(jsonPath("$[" + index + "].lastModifiedBy", equalTo(TEST_USER)))
                    .andExpect(jsonPath("$[" + index + "].lastModifiedAt").exists())
                    .andExpect(jsonPath("$[" + index + "].key").doesNotExist())
                    .andExpect(jsonPath("$[" + index + "].deleted").doesNotExist())
                    .andExpect(jsonPath("$[" + index + "]._links.self.href",
                            startsWith("http://localhost/rest/v1/targettypes/")))
                    .andExpect(
                            jsonPath("$[" + index + "]._links.compatibledistributionsettypes.href")
                                    .doesNotExist());
        }
        MvcResult mvcResult = resultActions.andReturn();

        for (int index = 0; index < size; index++) {
            String name = "TestTypePOST" + index;
            final TargetType created = targetTypeManagement.getByName(name).get();

            assertThat(JsonPath.compile("$[ ?(@.name=='" + name + "') ].id")
                    .read(mvcResult.getResponse().getContentAsString()).toString()).contains(String.valueOf(created.getId()));
            assertThat(JsonPath.compile("$[ ?(@.name=='" + name + "') ]._links.self.href")
                    .read(mvcResult.getResponse().getContentAsString()).toString()).contains("/"+created.getId());
             }

        assertThat(targetTypeManagement.count()).isEqualTo(size);
    }

}
