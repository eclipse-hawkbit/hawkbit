/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.system.rest.resource;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.eclipse.hawkbit.AbstractIntegrationTest;
import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.rest.json.model.ExceptionInfo;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.eclipse.hawkbit.system.rest.api.SystemRestConstant;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationKey;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - System RESTful API")
@Stories("ConfigurationResource")
public class SystemResourceTest extends AbstractIntegrationTest {

    private static String BASE_JSON_REQUEST_STRING = "{\"value\":\"%s\"}";

    @Test
    @Description("perform a GET request on all existing configurations.")
    public void getConfigurationValues() throws Exception {

        final ResultActions resultActions = mvc.perform(get(SystemRestConstant.SYSTEM_V1_REQUEST_MAPPING + "/configs/"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(TenantConfigurationKey.values().length)));

        for (final TenantConfigurationKey key : TenantConfigurationKey.values()) {

            final TenantConfigurationValue<?> confValue = tenantConfigurationManagement.getConfigurationValue(key);
            resultActions.andExpect(jsonPath("$.['" + key.getKeyName() + "'].value", equalTo(confValue.getValue())))
                    .andExpect(jsonPath("$.['" + key.getKeyName() + "'].global", equalTo(confValue.isGlobal())));
        }
    }

    @Test
    @Description("perform a GET request on a existing configuration key.")
    public void getConfigurationValue() throws Exception {

        final TenantConfigurationKey key = TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_AUTHORITY_NAME;
        final String notGlobalValue = "notTheGlobalHeaderAuthoryName";

        tenantConfigurationManagement.addOrUpdateConfiguration(key, notGlobalValue);

        mvc.perform(get(SystemRestConstant.SYSTEM_V1_REQUEST_MAPPING + "/configs/{keyName}/", key.getKeyName()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("value", equalTo(notGlobalValue))).andExpect(jsonPath("global", equalTo(false)))
                .andExpect(jsonPath("createdAt", notNullValue())).andExpect(jsonPath("createdBy", notNullValue()));
    }

    @Test
    @Description("perform a PUT request on a existing configuration key with a valid value.")
    public void putConfigurationValue() throws Exception {

        final TenantConfigurationKey key = TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_AUTHORITY_NAME;
        final String testValue = "12:12:12";

        mvc.perform(put(SystemRestConstant.SYSTEM_V1_REQUEST_MAPPING + "/configs/{keyName}", key.getKeyName())
                .content(String.format(BASE_JSON_REQUEST_STRING, testValue)).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        assertThat(tenantConfigurationManagement.getConfigurationValue(key, String.class).getValue())
                .isEqualTo(testValue);
    }

    @Test
    @Description("perform a DELETE request on a existing configuration key.")
    public void deleteConfigurationValue() throws Exception {

        final TenantConfigurationKey key = TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_AUTHORITY_NAME;
        final String notGlobalValue = "notTheGlobalHeaderAuthoryName";

        tenantConfigurationManagement.addOrUpdateConfiguration(key, notGlobalValue);
        assertThat(tenantConfigurationManagement.getConfigurationValue(key, String.class).isGlobal()).isEqualTo(false);

        assertThat(tenantConfigurationManagement.getConfigurationValue(key, String.class).getValue())
                .isEqualTo(notGlobalValue);

        mvc.perform(delete(SystemRestConstant.SYSTEM_V1_REQUEST_MAPPING + "/configs/{keyName}/", key.getKeyName()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNoContent());

        assertThat(tenantConfigurationManagement.getConfigurationValue(key, String.class).isGlobal()).isEqualTo(true);
        assertThat(tenantConfigurationManagement.getConfigurationValue(key, String.class).getValue())
                .isNotEqualTo(notGlobalValue);
    }

    @Test
    @Description("perform a (put) request on a not existing configuration key.")
    public void putInvalidConfigurationKey() throws Exception {

        final String notExistingKey = "notExistingKey";
        final String testValue = "12:12:12";

        final MvcResult mvcResult = mvc
                .perform(put(SystemRestConstant.SYSTEM_V1_REQUEST_MAPPING + "/configs/{keyName}", notExistingKey)
                        .content(String.format(BASE_JSON_REQUEST_STRING, testValue))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andReturn();

        // verify response json exception message
        final ExceptionInfo exceptionInfo = ResourceUtility
                .convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getErrorCode()).isEqualTo(SpServerError.SP_CONFIGURATION_KEY_INVALID.getKey());
    }

    @Test
    @Description("perform a put request with a not matching configuration value.")
    public void putInvalidConfigurationValue() throws Exception {

        final TenantConfigurationKey key = TenantConfigurationKey.POLLING_TIME_INTERVAL;
        final String testValue = "invalidFormattedDuration";

        final MvcResult mvcResult = mvc
                .perform(put(SystemRestConstant.SYSTEM_V1_REQUEST_MAPPING + "/configs/{keyName}", key.getKeyName())
                        .content(String.format(BASE_JSON_REQUEST_STRING, testValue))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andReturn();

        // verify response json exception message
        final ExceptionInfo exceptionInfo = ResourceUtility
                .convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getErrorCode()).isEqualTo(SpServerError.SP_CONFIGURATION_VALUE_INVALID.getKey());
    }
}
