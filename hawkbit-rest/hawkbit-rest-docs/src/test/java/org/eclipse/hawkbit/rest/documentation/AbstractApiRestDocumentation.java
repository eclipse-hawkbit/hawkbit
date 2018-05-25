/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.documentation;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.restdocs.snippet.Attributes.key;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.hawkbit.ddi.rest.resource.DdiApiConfiguration;
import org.eclipse.hawkbit.mgmt.rest.resource.MgmtApiConfiguration;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.rest.AbstractRestIntegrationTest;
import org.eclipse.hawkbit.rest.util.FilterHttpResponse;
import org.junit.Before;
import org.junit.Rule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.snippet.Snippet;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import ru.yandex.qatools.allure.annotations.Features;

/**
 * Parent class for all Management API rest documentation classes.
 *
 */
@Features("Documentation Verfication - API")
@SpringApplicationConfiguration(classes = { DdiApiConfiguration.class, MgmtApiConfiguration.class })
@TestPropertySource(locations = { "classpath:/updateserver-restdocumentation-test.properties" })
public abstract class AbstractApiRestDocumentation extends AbstractRestIntegrationTest {

    @Rule
    public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("target/generated-snippets");

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private FilterHttpResponse filterHttpResponse;

    protected MockMvc mockMvc;

    protected String resourceName = "output";

    protected RestDocumentationResultHandler document;

    protected String arrayPrefix;

    protected String host = "management-api.host";

    @Before
    protected void setUp() {
        this.document = document(resourceName + "/{method-name}", preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()));
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(MockMvcRestDocumentation.documentationConfiguration(this.restDocumentation).uris()
                        .withScheme("https").withHost(host + ".com").withPort(443))
                .alwaysDo(this.document).addFilter(filterHttpResponse).build();
        arrayPrefix = "[]";
    }

    public static MyFieldFieldDesc requestFieldWithPath(final String path, final boolean mandatory) {
        final MyFieldFieldDesc myFieldDesc = new MyFieldFieldDesc(path);
        myFieldDesc.attributes(key("mandatory").value(mandatory ? "X" : ""));
        // defaults
        myFieldDesc.attributes(key("value").value(""));
        return myFieldDesc;
    }

    public static MyFieldFieldDesc requestFieldWithPath(final String path) {
        return requestFieldWithPath(path, true);
    }

    protected static MyFieldFieldDesc optionalRequestFieldWithPath(final String path) {
        return requestFieldWithPath(path, false);
    }

    public static class MyFieldFieldDesc extends FieldDescriptor {

        /**
         * @param path
         */
        protected MyFieldFieldDesc(final String path) {
            super(path);
        }
    }

    /**
     * Wrapper for FieldDescriptor adding attribute with a key named value if
     * not set.
     *
     * @param path
     * @return FieldDescriptor with value key
     */
    public static FieldDescriptor fieldWithPath(final String path) {
        final MyFieldFieldDesc myFieldDesc = new MyFieldFieldDesc(path);
        // defaults
        myFieldDesc.attributes(key("value").value(""));
        return myFieldDesc;
    }

    protected Target createTargetByGivenNameWithAttributes(final String name, final boolean inSync,
            final boolean timeforced, final DistributionSet distributionSet) {
        return createTargetByGivenNameWithAttributes(name, inSync, timeforced, distributionSet, null, null, null);
    }

    protected Target createTargetByGivenNameWithAttributes(final String name, final boolean inSync,
            final boolean timeforced, final DistributionSet distributionSet, final String maintenanceWindowSchedule,
            final String maintenanceWindowDuration, final String maintenanceWindowTimeZone) {

        final Target savedTarget = targetManagement.create(entityFactory.target().create().controllerId(name)
                .status(TargetUpdateStatus.UNKNOWN).address("http://192.168.0.1").description("My name is " + name)
                .lastTargetQuery(System.currentTimeMillis()));

        final List<Target> updatedTargets = maintenanceWindowSchedule == null
                ? assignWithoutMaintenanceWindow(distributionSet, savedTarget, timeforced)
                : assignWithMaintenanceWindow(distributionSet, savedTarget, timeforced, maintenanceWindowSchedule,
                        maintenanceWindowDuration, maintenanceWindowTimeZone);

        if (inSync) {
            feedbackToByInSync(distributionSet);
        }

        return updatedTargets.get(0);
    }

    private List<Target> assignWithoutMaintenanceWindow(final DistributionSet distributionSet, final Target savedTarget,
            final boolean timeforced) {
        return timeforced ? assignDistributionSetTimeForced(distributionSet, savedTarget).getAssignedEntity()
                : assignDistributionSet(distributionSet, savedTarget).getAssignedEntity();
    }

    private List<Target> assignWithMaintenanceWindow(final DistributionSet distributionSet, final Target savedTarget,
            final boolean timeforced, final String maintenanceWindowSchedule, final String maintenanceWindowDuration,
            final String maintenanceWindowTimeZone) {
        return timeforced
                ? assignDistributionSetWithMaintenanceWindowTimeForced(distributionSet.getId(),
                        savedTarget.getControllerId(), maintenanceWindowSchedule, maintenanceWindowDuration,
                        maintenanceWindowTimeZone).getAssignedEntity()
                : assignDistributionSetWithMaintenanceWindow(distributionSet.getId(), savedTarget.getControllerId(),
                        maintenanceWindowSchedule, maintenanceWindowDuration, maintenanceWindowTimeZone)
                                .getAssignedEntity();
    }

    protected DistributionSet createDistributionSet() {
        DistributionSet distributionSet = testdataFactory.createDistributionSet("");
        distributionSet = distributionSetManagement.update(entityFactory.distributionSet()
                .update(distributionSet.getId()).description("The descption of the distribution set."));

        distributionSet.getModules().forEach(module -> {
            final byte[] random = RandomStringUtils.random(5).getBytes();
            artifactManagement.create(new ByteArrayInputStream(random), module.getId(), "file1", false, 0);
            softwareModuleManagement.update(entityFactory.softwareModule().update(module.getId())
                    .description("Description of the software module"));
        });

        return distributionSet;
    }

    /*
     * helper method to give feedback mark an target IN_SNCY *
     */
    private void feedbackToByInSync(final DistributionSet savedSet) {
        final Action action = deploymentManagement.findActionsByDistributionSet(PAGE, savedSet.getId()).getContent()
                .get(0);

        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(action.getId()).status(Status.FINISHED));
    }

    protected Target createTargetByGivenNameWithAttributes(final String name, final DistributionSet distributionSet) {
        return createTargetByGivenNameWithAttributes(name, true, false, distributionSet);
    }

    protected String getArrayPrefix(final boolean isArray) {
        return isArray ? arrayPrefix : "";
    }

    protected Snippet getResponseFieldTarget(final boolean isArray, final FieldDescriptor... descriptors) {
        final String fieldArrayPrefix = getArrayPrefix(isArray);

        final List<FieldDescriptor> fields = Lists.newArrayList(
                fieldWithPath(fieldArrayPrefix + "createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                fieldWithPath(fieldArrayPrefix + "address").description(MgmtApiModelProperties.ADDRESS),
                fieldWithPath(fieldArrayPrefix + "createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                fieldWithPath(fieldArrayPrefix + "name").description(ApiModelPropertiesGeneric.NAME),
                fieldWithPath(fieldArrayPrefix + "description").description(ApiModelPropertiesGeneric.DESCRPTION),
                fieldWithPath(fieldArrayPrefix + "controllerId").description(ApiModelPropertiesGeneric.ITEM_ID),
                fieldWithPath(fieldArrayPrefix + "updateStatus").description(MgmtApiModelProperties.UPDATE_STATUS)
                        .type("enum")
                        .attributes(key("value").value("['error', 'in_sync', 'pending', 'registered', 'unknown']")),
                fieldWithPath(fieldArrayPrefix + "securityToken").description(MgmtApiModelProperties.SECURITY_TOKEN),
                fieldWithPath(fieldArrayPrefix + "requestAttributes")
                        .description(MgmtApiModelProperties.REQUEST_ATTRIBUTES),
                fieldWithPath(fieldArrayPrefix + "installedAt").description(MgmtApiModelProperties.INSTALLED_AT),
                fieldWithPath(fieldArrayPrefix + "lastModifiedAt")
                        .description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT).type("Number"),
                fieldWithPath(fieldArrayPrefix + "lastModifiedBy")
                        .description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY).type("String"),
                fieldWithPath(fieldArrayPrefix + "ipAddress").description(MgmtApiModelProperties.IP_ADDRESS)
                        .type("String"),
                fieldWithPath(fieldArrayPrefix + "lastControllerRequestAt")
                        .description(MgmtApiModelProperties.LAST_REQUEST_AT).type("Number"),
                fieldWithPath(fieldArrayPrefix + "_links.self").ignored());

        if (!isArray) {
            fields.addAll(Arrays.asList(
                    fieldWithPath(fieldArrayPrefix + "pollStatus").description(MgmtApiModelProperties.POLL_STATUS),
                    fieldWithPath(fieldArrayPrefix + "pollStatus.lastRequestAt")
                            .description(MgmtApiModelProperties.POLL_LAST_REQUEST_AT),
                    fieldWithPath(fieldArrayPrefix + "pollStatus.nextExpectedRequestAt")
                            .description(MgmtApiModelProperties.POLL_NEXT_EXPECTED_REQUEST_AT),
                    fieldWithPath(fieldArrayPrefix + "pollStatus.overdue")
                            .description(MgmtApiModelProperties.POLL_OVERDUE),
                    fieldWithPath(fieldArrayPrefix + "_links.assignedDS")
                            .description(MgmtApiModelProperties.LINKS_ASSIGNED_DS),
                    fieldWithPath(fieldArrayPrefix + "_links.installedDS")
                            .description(MgmtApiModelProperties.LINKS_INSTALLED_DS),
                    fieldWithPath(fieldArrayPrefix + "_links.attributes")
                            .description(MgmtApiModelProperties.LINKS_ATTRIBUTES),
                    fieldWithPath(fieldArrayPrefix + "_links.actions")
                            .description(MgmtApiModelProperties.LINKS_ACTIONS)));
        }
        fields.addAll(Arrays.asList(descriptors));

        return responseFields(fields);
    }

    protected Snippet getResponseFieldsDistributionSet(final boolean isArray, final FieldDescriptor... descriptors) {
        final String arrayPrefix = getArrayPrefix(isArray);
        final List<FieldDescriptor> fields = Lists.newArrayList(
                fieldWithPath(arrayPrefix + "id").description(ApiModelPropertiesGeneric.ITEM_ID),
                fieldWithPath(arrayPrefix + "name").description(ApiModelPropertiesGeneric.NAME),
                fieldWithPath(arrayPrefix + "description").description(ApiModelPropertiesGeneric.DESCRPTION),
                fieldWithPath(arrayPrefix + "createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                fieldWithPath(arrayPrefix + "createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                fieldWithPath(arrayPrefix + "lastModifiedBy").description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY),
                fieldWithPath(arrayPrefix + "lastModifiedAt").description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT),
                fieldWithPath(arrayPrefix + "type").description(MgmtApiModelProperties.DS_TYPE),
                fieldWithPath(arrayPrefix + "requiredMigrationStep")
                        .description(MgmtApiModelProperties.DS_REQUIRED_STEP),
                fieldWithPath(arrayPrefix + "complete").description(MgmtApiModelProperties.DS_COMPLETE),
                fieldWithPath(arrayPrefix + "deleted").description(ApiModelPropertiesGeneric.DELETED),
                fieldWithPath(arrayPrefix + "version").description(MgmtApiModelProperties.VERSION),
                fieldWithPath(arrayPrefix + "_links.self").ignored(), fieldWithPath(arrayPrefix + "modules").ignored());

        fields.addAll(Arrays.asList(descriptors));

        if (!isArray) {
            fields.add(fieldWithPath(arrayPrefix + "_links.type").description(MgmtApiModelProperties.DS_TYPE));
            fields.add(fieldWithPath(arrayPrefix + "_links.metadata").description(MgmtApiModelProperties.META_DATA));
            fields.add(fieldWithPath(arrayPrefix + "_links.modules").description(MgmtApiModelProperties.SM_LIST));
        }

        return responseFields(fields);
    }

    protected Snippet getFilterRequestParamter() {
        return requestParameters(
                parameterWithName("limit").attributes(key("type").value("query"))
                        .description(ApiModelPropertiesGeneric.LIMIT),
                parameterWithName("sort").description(ApiModelPropertiesGeneric.SORT),
                parameterWithName("offset").description(ApiModelPropertiesGeneric.OFFSET),
                parameterWithName("q").description(ApiModelPropertiesGeneric.FIQL));
    }

}
