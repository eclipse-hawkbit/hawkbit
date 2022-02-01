/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.hawkbit.repository.test.util.TargetTestData.ATTRIBUTE_KEY_TOO_LONG;
import static org.eclipse.hawkbit.repository.test.util.TargetTestData.ATTRIBUTE_KEY_VALID;
import static org.eclipse.hawkbit.repository.test.util.TargetTestData.ATTRIBUTE_VALUE_TOO_LONG;
import static org.eclipse.hawkbit.repository.test.util.TargetTestData.ATTRIBUTE_VALUE_VALID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.eclipse.hawkbit.ddi.rest.api.DdiRestConstants;
import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.InvalidTargetAttributeException;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import com.google.common.collect.Maps;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
/**
 * Test config data from the controller.
 */
@ActiveProfiles({ "im", "test" })
@Feature("Component Tests - Direct Device Integration API")
@Story("Config Data Resource")
class DdiConfigDataTest extends AbstractDDiApiIntegrationTest {

    public static final String TARGET1_ID = "4717";
    public static final String TARGET1_CONFIGDATA_PATH = "/{tenant}/controller/v1/" + TARGET1_ID + "/configData";
    public static final String TARGET2_ID = "4718";
    public static final String TARGET2_CONFIGDATA_PATH = "/{tenant}/controller/v1/" + TARGET2_ID + "/configData";

    @Test
    @Description("Verify that config data can be uploaded as CBOR")
    void putConfigDataAsCbor() throws Exception {
        testdataFactory.createTarget(TARGET1_ID);

        final Map<String, String> attributes = new HashMap<>();
        attributes.put(ATTRIBUTE_KEY_VALID, ATTRIBUTE_VALUE_VALID);

        mvc.perform(put(TARGET1_CONFIGDATA_PATH, tenantAware.getCurrentTenant())
                .content(jsonToCbor(JsonBuilder.configData(attributes).toString()))
                .contentType(DdiRestConstants.MEDIA_TYPE_CBOR)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        assertThat(targetManagement.getControllerAttributes(TARGET1_ID)).isEqualTo(attributes);
    }

    @Test
    @Description("We verify that the config data (i.e. device attributes like serial number, hardware revision etc.) "
            + "are requested only once from the device.")
    @SuppressWarnings("squid:S2925")
    void requestConfigDataIfEmpty() throws Exception {
        final Target savedTarget = testdataFactory.createTarget("4712");

        final long current = System.currentTimeMillis();
        mvc.perform(
                get("/{tenant}/controller/v1/4712", tenantAware.getCurrentTenant()).accept(MediaTypes.HAL_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$._links.configData.href", equalTo(
                        "http://localhost/" + tenantAware.getCurrentTenant() + "/controller/v1/4712/configData")));
        Thread.sleep(1); // is required: otherwise processing the next line is
                         // often too fast and
                         // the following assert will fail
        assertThat(targetManagement.getByControllerID("4712").orElseThrow(NoSuchElementException::new)
                .getLastTargetQuery())
                .isLessThanOrEqualTo(System.currentTimeMillis());
        assertThat(targetManagement.getByControllerID("4712").orElseThrow(NoSuchElementException::new)
                .getLastTargetQuery())
                .isGreaterThanOrEqualTo(current);

        final Map<String, String> attributes = Maps.newHashMapWithExpectedSize(1);
        attributes.put("dsafsdf", "sdsds");

        final Target updateControllerAttributes = controllerManagement
                .updateControllerAttributes(savedTarget.getControllerId(), attributes, null);
        // request controller attributes need to be false because we don't want
        // to request the controller attributes again
        assertThat(updateControllerAttributes.isRequestControllerAttributes()).isFalse();

        mvc.perform(
                get("/{tenant}/controller/v1/4712", tenantAware.getCurrentTenant()).accept(MediaTypes.HAL_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$._links.configData.href").doesNotExist());
    }

    @Test
    @Description("We verify that the config data (i.e. device attributes like serial number, hardware revision etc.) "
            + "can be uploaded correctly by the controller.")
    void putConfigData() throws Exception {
        testdataFactory.createTarget(TARGET1_ID);

        // initial
        final Map<String, String> attributes = new HashMap<>();
        attributes.put(ATTRIBUTE_KEY_VALID, ATTRIBUTE_VALUE_VALID);

        mvc.perform(put(TARGET1_CONFIGDATA_PATH, tenantAware.getCurrentTenant())
                .content(JsonBuilder.configData(attributes).toString()).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        assertThat(targetManagement.getControllerAttributes(TARGET1_ID)).isEqualTo(attributes);

        // update
        attributes.put("sdsds", "123412");
        mvc.perform(put(TARGET1_CONFIGDATA_PATH, tenantAware.getCurrentTenant())
                .content(JsonBuilder.configData(attributes).toString()).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        assertThat(targetManagement.getControllerAttributes(TARGET1_ID)).isEqualTo(attributes);
    }

    @Test
    @Description("We verify that the config data (i.e. device attributes like serial number, hardware revision etc.) "
            + "upload quota is enforced to protect the server from malicious attempts.")
    void putTooMuchConfigData() throws Exception {
        testdataFactory.createTarget(TARGET1_ID);

        // initial
        Map<String, String> attributes = new HashMap<>();
        for (int i = 0; i < quotaManagement.getMaxAttributeEntriesPerTarget(); i++) {
            attributes.put("dsafsdf" + i, "sdsds" + i);
        }
        mvc.perform(put(TARGET1_CONFIGDATA_PATH, tenantAware.getCurrentTenant())
                .content(JsonBuilder.configData(attributes).toString()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        attributes = new HashMap<>();
        attributes.put("on too many", "sdsds");
        mvc.perform(put(TARGET1_CONFIGDATA_PATH, tenantAware.getCurrentTenant())
                .content(JsonBuilder.configData(attributes).toString()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.exceptionClass", equalTo(AssignmentQuotaExceededException.class.getName())))
                .andExpect(jsonPath("$.errorCode", equalTo(SpServerError.SP_QUOTA_EXCEEDED.getKey())));

    }

    @Test
    @Description("We verify that the config data (i.e. device attributes like serial number, hardware revision etc.) "
            + "resource behaves as expected in case of invalid request attempts.")
    void badConfigData() throws Exception {
        testdataFactory.createTarget("4712");

        // not allowed methods
        mvc.perform(post("/{tenant}/controller/v1/4712/configData", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).//
                andExpect(status().isMethodNotAllowed());

        mvc.perform(get("/{tenant}/controller/v1/4712/configData", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        mvc.perform(delete("/{tenant}/controller/v1/4712/configData", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        // bad content type
        final Map<String, String> attributes = new HashMap<>();
        attributes.put("dsafsdf", "sdsds");
        mvc.perform(put("/{tenant}/controller/v1/4712/configData", tenantAware.getCurrentTenant())
                .content(JsonBuilder.configData(attributes).toString()).contentType(MediaTypes.HAL_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isUnsupportedMediaType());

        // non existing target
        mvc.perform(put("/{tenant}/controller/v1/456456/configData", tenantAware.getCurrentTenant())
                .content(JsonBuilder.configData(attributes).toString()).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // bad body
        mvc.perform(put("/{tenant}/controller/v1/4712/configData", tenantAware.getCurrentTenant())
                .content("{\"id\": \"51659181\"}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Verifies that invalid config data attributes are handled correctly.")
    void putConfigDataWithInvalidAttributes() throws Exception {
        // create a target
        testdataFactory.createTarget(TARGET2_ID);
        putAndVerifyConfigDataWithKeyTooLong();
        putAndVerifyConfigDataWithValueTooLong();
    }

    @Step
    private void putAndVerifyConfigDataWithKeyTooLong() throws Exception {
        final Map<String, String> attributes = Collections.singletonMap(ATTRIBUTE_KEY_TOO_LONG, ATTRIBUTE_VALUE_VALID);
        mvc.perform(put(DdiConfigDataTest.TARGET2_CONFIGDATA_PATH, tenantAware.getCurrentTenant())
                .content(JsonBuilder.configData(attributes).toString()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionClass", equalTo(InvalidTargetAttributeException.class.getName())))
                .andExpect(jsonPath("$.errorCode", equalTo(SpServerError.SP_TARGET_ATTRIBUTES_INVALID.getKey())));
    }

    @Step
    private void putAndVerifyConfigDataWithValueTooLong() throws Exception {
        final Map<String, String> attributes = Collections.singletonMap(ATTRIBUTE_KEY_VALID, ATTRIBUTE_VALUE_TOO_LONG);
        mvc.perform(put(DdiConfigDataTest.TARGET2_CONFIGDATA_PATH, tenantAware.getCurrentTenant())
                .content(JsonBuilder.configData(attributes).toString()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionClass", equalTo(InvalidTargetAttributeException.class.getName())))
                .andExpect(jsonPath("$.errorCode", equalTo(SpServerError.SP_TARGET_ATTRIBUTES_INVALID.getKey())));
    }

    @Test
    @Description("Verify that config data (device attributes) can be updated by the controller using different update modes (merge, replace, remove).")
    void putConfigDataWithDifferentUpdateModes() throws Exception {

        // create a target
        testdataFactory.createTarget(TARGET1_ID);

        // no update mode
        putConfigDataWithoutUpdateMode();

        // update mode REPLACE
        putConfigDataWithUpdateModeReplace();

        // update mode MERGE
        putConfigDataWithUpdateModeMerge();

        // update mode REMOVE
        putConfigDataWithUpdateModeRemove();

        // invalid update mode
        putConfigDataWithInvalidUpdateMode();
    }

    @Step
    private void putConfigDataWithInvalidUpdateMode() throws Exception {
        // create some attriutes
        final Map<String, String> attributes = new HashMap<>();
        attributes.put("k0", "v0");
        attributes.put("k1", "v1");

        // use an invalid update mode
        mvc.perform(put(DdiConfigDataTest.TARGET1_CONFIGDATA_PATH, tenantAware.getCurrentTenant())
                .content(JsonBuilder.configData(attributes, "KJHGKJHGKJHG").toString())
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    @Step
    private void putConfigDataWithUpdateModeRemove() throws Exception {
        // get the current attributes
        final int previousSize = targetManagement.getControllerAttributes(DdiConfigDataTest.TARGET1_ID).size();

        // update the attributes using update mode REMOVE
        final Map<String, String> removeAttributes = new HashMap<>();
        removeAttributes.put("k1", "foo");
        removeAttributes.put("k3", "bar");

        mvc.perform(put(DdiConfigDataTest.TARGET1_CONFIGDATA_PATH, tenantAware.getCurrentTenant())
                .content(JsonBuilder.configData(removeAttributes, "remove").toString())
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // verify attribute removal
        final Map<String, String> updatedAttributes = targetManagement
                .getControllerAttributes(DdiConfigDataTest.TARGET1_ID);
        assertThat(updatedAttributes).hasSize(previousSize - 2);
        assertThat(updatedAttributes).doesNotContainKeys("k1", "k3");

    }

    @Step
    private void putConfigDataWithUpdateModeMerge()
            throws Exception {
        // get the current attributes
        final Map<String, String> attributes = new HashMap<>(
                targetManagement.getControllerAttributes(DdiConfigDataTest.TARGET1_ID));

        // update the attributes using update mode MERGE
        final Map<String, String> mergeAttributes = new HashMap<>();
        mergeAttributes.put("k1", "v1_modified_again");
        mergeAttributes.put("k4", "v4");
        mvc.perform(put(DdiConfigDataTest.TARGET1_CONFIGDATA_PATH, tenantAware.getCurrentTenant())
                .content(JsonBuilder.configData(mergeAttributes, "merge").toString())
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // verify attribute merge
        final Map<String, String> updatedAttributes = targetManagement
                .getControllerAttributes(DdiConfigDataTest.TARGET1_ID);
        assertThat(updatedAttributes).hasSize(4);
        assertThat(updatedAttributes).containsAllEntriesOf(mergeAttributes);
        assertThat(updatedAttributes).containsEntry("k1", "v1_modified_again");
        attributes.keySet().forEach(assertThat(updatedAttributes)::containsKey);

    }

    @Step
    private void putConfigDataWithUpdateModeReplace()
            throws Exception {
        // get the current attributes
        final Map<String, String> attributes = new HashMap<>(
                targetManagement.getControllerAttributes(DdiConfigDataTest.TARGET1_ID));

        // update the attributes using update mode REPLACE
        final Map<String, String> replacementAttributes = new HashMap<>();
        replacementAttributes.put("k1", "v1_modified");
        replacementAttributes.put("k2", "v2");
        replacementAttributes.put("k3", "v3");
        mvc.perform(put(DdiConfigDataTest.TARGET1_CONFIGDATA_PATH, tenantAware.getCurrentTenant())
                .content(JsonBuilder.configData(replacementAttributes, "replace").toString())
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // verify attribute replacement
        final Map<String, String> updatedAttributes = targetManagement
                .getControllerAttributes(DdiConfigDataTest.TARGET1_ID);
        assertThat(updatedAttributes).hasSize(replacementAttributes.size());
        assertThat(updatedAttributes).containsAllEntriesOf(replacementAttributes);
        assertThat(updatedAttributes).containsEntry("k1", "v1_modified");
        attributes.entrySet().forEach(assertThat(updatedAttributes)::doesNotContain);

    }

    @Step
    private void putConfigDataWithoutUpdateMode()
            throws Exception {
        // create some attriutes
        final Map<String, String> attributes = new HashMap<>();
        attributes.put("k0", "v0");
        attributes.put("k1", "v1");

        // set the initial attributes
        mvc.perform(put(DdiConfigDataTest.TARGET1_CONFIGDATA_PATH, tenantAware.getCurrentTenant())
                .content(JsonBuilder.configData(attributes).toString()).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        // verify the initial parameters
        final Map<String, String> updatedAttributes = targetManagement
                .getControllerAttributes(DdiConfigDataTest.TARGET1_ID);
        assertThat(updatedAttributes).containsExactlyEntriesOf(attributes);
    }
}
