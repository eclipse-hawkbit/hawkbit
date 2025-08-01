/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.servlet.HttpEncodingAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.server.Encoding;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * With Spring Boot 2.2.x the default charset encoding became deprecated. In hawkBit we want to keep the old behavior for now and still
 * return the charset in the response, which is achieved through enabling {@link Encoding} via properties.
 * <p/>
 * Feature: Component Tests - Management API<br/>
 * Story: Response Content-Type
 */
@SpringBootTest(properties = { "server.servlet.encoding.charset=UTF-8", "server.servlet.encoding.force=true" })
@Import(HttpEncodingAutoConfiguration.class)
@SuppressWarnings("java:S1874") // TODO for compatibility, to be checked if we really want to do that
public class MgmtContentTypeTest extends AbstractManagementApiIntegrationTest {

    private static final String DS_NAME = "DS-ö";
    private DistributionSetManagement.Create dsCreate;

    @BeforeEach
    public void setupBeforeTest() {
        dsCreate = DistributionSetManagement.Create.builder().type(defaultDsType()).name(DS_NAME).version("1.0").build();
    }

    /**
     * The response of a POST request shall contain charset=utf-8
     */
    @Test
     void postDistributionSet_ContentTypeJsonUtf8_woAccept() throws Exception {
        final MvcResult result = mvc.perform(
                        post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING).content(JsonBuilder.distributionSets(
                                        Collections.singletonList(dsCreate)))
                                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("[0]name", equalTo(DS_NAME)))
                .andReturn();

        assertEquals(MediaTypes.HAL_JSON_VALUE + ";charset=UTF-8", getResponseHeaderContentType(result));
    }

    /**
     * The response of a POST request shall contain charset=utf-8
     */
    @Test
     void postDistributionSet_ContentTypeJsonUtf8_wAcceptJson() throws Exception {
        final MvcResult result = mvc.perform(
                        post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING).content(JsonBuilder.distributionSets(
                                        Collections.singletonList(dsCreate)))
                                .contentType(MediaType.APPLICATION_JSON_UTF8).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("[0]name", equalTo(DS_NAME)))
                .andReturn();

        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, getResponseHeaderContentType(result));
    }

    /**
     * The response of a POST request shall contain charset=utf-8
     */
    @Test
     void postDistributionSet_ContentTypeJsonUtf8_wAcceptJsonUtf8() throws Exception {
        final MvcResult result = mvc.perform(
                        post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING).content(JsonBuilder.distributionSets(
                                        Collections.singletonList(dsCreate)))
                                .contentType(MediaType.APPLICATION_JSON_UTF8).accept(MediaType.APPLICATION_JSON_UTF8))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("[0]name", equalTo(DS_NAME)))
                .andReturn();

        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, getResponseHeaderContentType(result));
    }

    /**
     * The response of a POST request shall contain charset=utf-8
     */
    @Test
     void postDistributionSet_ContentTypeJsonUtf8_wAcceptHalJson() throws Exception {
        final MvcResult result = mvc.perform(
                        post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING).content(JsonBuilder.distributionSets(
                                        Collections.singletonList(dsCreate)))
                                .contentType(MediaType.APPLICATION_JSON_UTF8).accept(MediaTypes.HAL_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("[0]name", equalTo(DS_NAME)))
                .andReturn();

        assertEquals(MediaTypes.HAL_JSON_VALUE + ";charset=UTF-8", getResponseHeaderContentType(result));
    }

    /**
     * The response of a POST request shall contain charset=utf-8
     */
    @Test
     void postDistributionSet_ContentTypeJson_woAccept() throws Exception {
        final MvcResult result = mvc.perform(
                        post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING).content(JsonBuilder.distributionSets(
                                        Collections.singletonList(dsCreate)))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("[0]name", equalTo(DS_NAME)))
                .andReturn();

        assertEquals(MediaTypes.HAL_JSON_VALUE + ";charset=UTF-8", getResponseHeaderContentType(result));
    }

    /**
     * The response of a POST request shall contain charset=utf-8
     */
    @Test
     void postDistributionSet_ContentTypeJson_wAcceptJson() throws Exception {
        final MvcResult result = mvc.perform(
                        post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING).content(JsonBuilder.distributionSets(
                                        Collections.singletonList(dsCreate)))
                                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("[0]name", equalTo(DS_NAME)))
                .andReturn();

        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, getResponseHeaderContentType(result));
    }

    /**
     * The response of a POST request shall contain charset=utf-8
     */
    @Test
     void postDistributionSet_ContentTypeJson_wAcceptJsonUtf8() throws Exception {
        final MvcResult result = mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING)
                        .content(JsonBuilder.distributionSets(Collections.singletonList(dsCreate))).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("[0]name", equalTo(DS_NAME)))
                .andReturn();

        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, getResponseHeaderContentType(result));
    }

    /**
     * The response of a POST request shall contain charset=utf-8
     */
    @Test
     void postDistributionSet_ContentTypeJson_wAcceptHalJson() throws Exception {
        final MvcResult result = mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING)
                        .content(JsonBuilder.distributionSets(Collections.singletonList(dsCreate))).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaTypes.HAL_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("[0]name", equalTo(DS_NAME)))
                .andReturn();

        assertEquals(MediaTypes.HAL_JSON_VALUE + ";charset=UTF-8", getResponseHeaderContentType(result));
    }

    /**
     * The response of a GET request shall contain charset=utf-8
     */
    @Test
     void getDistributionSet_woAccept() throws Exception {
        final MvcResult result = mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andReturn();

        assertEquals(MediaTypes.HAL_JSON_VALUE + ";charset=UTF-8", getResponseHeaderContentType(result));
    }

    /**
     * The response of a GET request shall contain charset=utf-8
     */
    @Test
     void getDistributionSet_wAcceptJson() throws Exception {
        final MvcResult result = mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andReturn();

        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, getResponseHeaderContentType(result));
    }

    /**
     * The response of a GET request shall contain charset=utf-8
     */
    @Test
     void getDistributionSet_wAcceptJsonUtf8() throws Exception {
        final MvcResult result = mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING).accept(MediaType.APPLICATION_JSON_UTF8))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andReturn();

        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, getResponseHeaderContentType(result));
    }

    /**
     * The response of a GET request shall contain charset=utf-8
     */
    @Test
     void getDistributionSet_wAcceptHalJson() throws Exception {
        final MvcResult result = mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING).accept(MediaTypes.HAL_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andReturn();

        assertEquals(MediaTypes.HAL_JSON_VALUE + ";charset=UTF-8", getResponseHeaderContentType(result));
    }

    private String getResponseHeaderContentType(MvcResult result) {
        return result.getResponse().getHeader("Content-Type");
    }
}