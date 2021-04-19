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
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.hawkbit.ddi.rest.resource.DdiApiConfiguration;
import org.eclipse.hawkbit.mgmt.rest.resource.MgmtApiConfiguration;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.jpa.RepositoryApplicationConfiguration;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ArtifactUpload;
import org.eclipse.hawkbit.repository.model.DeploymentRequestBuilder;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.test.TestConfiguration;
import org.eclipse.hawkbit.rest.AbstractRestIntegrationTest;
import org.eclipse.hawkbit.rest.RestConfiguration;
import org.eclipse.hawkbit.rest.util.FilterHttpResponse;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.test.binder.TestSupportBinderAutoConfiguration;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.SubsectionDescriptor;
import org.springframework.restdocs.snippet.Snippet;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import io.qameta.allure.Feature;

/**
 * Parent class for all Management API rest documentation classes.
 *
 */
@Feature("Documentation Verification - API")
@ExtendWith(RestDocumentationExtension.class)
@ContextConfiguration(classes = { DdiApiConfiguration.class, MgmtApiConfiguration.class, RestConfiguration.class,
        RepositoryApplicationConfiguration.class, TestConfiguration.class, TestSupportBinderAutoConfiguration.class })
@TestPropertySource(locations = { "classpath:/updateserver-restdocumentation-test.properties" })
public abstract class AbstractApiRestDocumentation extends AbstractRestIntegrationTest {

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private FilterHttpResponse filterHttpResponse;

    protected MockMvc mockMvc;

    protected RestDocumentationResultHandler document;

    protected String arrayPrefix;

    protected String host = "management-api.host";

    /**
     * The generated REST docs snippets will be outputted to an own resource folder.
     * The child class has to specify the name of that output folder where to put its corresponding snippets.
     *
     * @return the name of the resource folder
     */
    public abstract String getResourceName();

    @BeforeEach
    protected void setupMvc(final RestDocumentationContextProvider restDocContext) {
        this.document = document(getResourceName() + "/{method-name}", preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()));
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(MockMvcRestDocumentation.documentationConfiguration(restDocContext).uris()
                        .withScheme("https").withHost(host + ".com").withPort(443))
                .alwaysDo(this.document).addFilter(filterHttpResponse).build();
        arrayPrefix = "[]";
    }

    public static MyFieldFieldDesc requestFieldWithPath(final String path, final boolean mandatory) {
        return requestFieldWithPath(path, mandatory, mandatory ? "X" : "");
    }

    private static MyFieldFieldDesc requestFieldWithPath(final String path, final boolean mandatory,
            final String mandatoryMessage) {
        final MyFieldFieldDesc myFieldDesc = new MyFieldFieldDesc(path);
        myFieldDesc.attributes(key("mandatory").value(mandatoryMessage));
        // defaults
        myFieldDesc.attributes(key("value").value(""));

        if (!mandatory) {
            myFieldDesc.optional();
        }

        return myFieldDesc;
    }

    public static MyFieldFieldDesc requestFieldWithPath(final String path) {
        return requestFieldWithPath(path, true);
    }

    protected static MyFieldFieldDesc optionalRequestFieldWithPath(final String path) {
        return requestFieldWithPath(path, false);
    }

    public static MyFieldFieldDesc requestFieldWithPathMandatoryInMultiAssignMode(final String path) {
        return requestFieldWithPath(path, false, "when multi-assignment is enabled");
    }

    public static class MyFieldFieldDesc extends SubsectionDescriptor {

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
        final DeploymentRequestBuilder deploymentRequestBuilder = DeploymentManagement
                .deploymentRequest(savedTarget.getControllerId(), distributionSet.getId())
                .setMaintenance(maintenanceWindowSchedule, maintenanceWindowDuration, maintenanceWindowTimeZone);
        if (timeforced) {
            deploymentRequestBuilder.setActionType(ActionType.TIMEFORCED);
        }
        if (isMultiAssignmentsEnabled()) {
            deploymentRequestBuilder.setWeight(600);
        }
        final List<Target> updatedTargets = makeAssignment(deploymentRequestBuilder.build()).getAssignedEntity()
                .stream().map(Action::getTarget).collect(Collectors.toList());

        if (inSync) {
            feedbackToByInSync(distributionSet);
        }

        return updatedTargets.get(0);
    }

    protected DistributionSet createDistributionSet() {
        DistributionSet distributionSet = testdataFactory.createDistributionSet("");
        distributionSet = distributionSetManagement.update(entityFactory.distributionSet()
                .update(distributionSet.getId()).description("The descption of the distribution set."));

        distributionSet.getModules().forEach(module -> {
            final byte[] random = RandomStringUtils.random(5).getBytes();
            artifactManagement
                    .create(new ArtifactUpload(new ByteArrayInputStream(random), module.getId(), "file1", false, 0));
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
                            .description(MgmtApiModelProperties.LINKS_ACTIONS),
                    fieldWithPath(fieldArrayPrefix + "_links.metadata").description(MgmtApiModelProperties.META_DATA)));

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

    protected boolean isMultiAssignmentsEnabled() {
        return Boolean.TRUE.equals(tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationKey.MULTI_ASSIGNMENTS_ENABLED, Boolean.class).getValue());
    }

}
