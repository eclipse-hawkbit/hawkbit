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
public class DdiConfigDataTest extends AbstractDDiApiIntegrationTest {

    private static final String KEY_TOO_LONG = generateRandomStringWithLength(Target.CONTROLLER_ATTRIBUTE_KEY_SIZE + 1);
    private static final String KEY_VALID = generateRandomStringWithLength(Target.CONTROLLER_ATTRIBUTE_KEY_SIZE);
    private static final String VALUE_TOO_LONG = generateRandomStringWithLength(
            Target.CONTROLLER_ATTRIBUTE_VALUE_SIZE + 1);
    private static final String VALUE_VALID = generateRandomStringWithLength(Target.CONTROLLER_ATTRIBUTE_VALUE_SIZE);

    @Test
    @Description("Verify that config data can be uploaded as CBOR")
    public void putConfigDataAsCbor() throws Exception {
        testdataFactory.createTarget("4717");

        final Map<String, String> attributes = new HashMap<>();
        attributes.put(KEY_VALID, VALUE_VALID);

        mvc.perform(put("/{tenant}/controller/v1/4717/configData", tenantAware.getCurrentTenant())
                .content(jsonToCbor(JsonBuilder.configData(attributes).toString()))
                .contentType(DdiRestConstants.MEDIA_TYPE_CBOR)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        assertThat(targetManagement.getControllerAttributes("4717")).isEqualTo(attributes);
    }

    @Test
    @Description("We verify that the config data (i.e. device attributes like serial number, hardware revision etc.) "
            + "are requested only once from the device.")
    @SuppressWarnings("squid:S2925")
    public void requestConfigDataIfEmpty() throws Exception {
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
        assertThat(targetManagement.getByControllerID("4712").get().getLastTargetQuery())
                .isLessThanOrEqualTo(System.currentTimeMillis());
        assertThat(targetManagement.getByControllerID("4712").get().getLastTargetQuery())
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
    public void putConfigData() throws Exception {
        testdataFactory.createTarget("4717");

        // initial
        final Map<String, String> attributes = new HashMap<>();
        attributes.put(KEY_VALID, VALUE_VALID);

        mvc.perform(put("/{tenant}/controller/v1/4717/configData", tenantAware.getCurrentTenant())
                .content(JsonBuilder.configData(attributes).toString()).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        assertThat(targetManagement.getControllerAttributes("4717")).isEqualTo(attributes);

        // update
        attributes.put("sdsds", "123412");
        mvc.perform(put("/{tenant}/controller/v1/4717/configData", tenantAware.getCurrentTenant())
                .content(JsonBuilder.configData(attributes).toString()).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        assertThat(targetManagement.getControllerAttributes("4717")).isEqualTo(attributes);
    }

    @Test
    @Description("We verify that the config data (i.e. device attributes like serial number, hardware revision etc.) "
            + "upload quota is enforced to protect the server from malicious attempts.")
    public void putTooMuchConfigData() throws Exception {
        testdataFactory.createTarget("4717");

        // initial
        Map<String, String> attributes = new HashMap<>();
        for (int i = 0; i < quotaManagement.getMaxAttributeEntriesPerTarget(); i++) {
            attributes.put("dsafsdf" + i, "sdsds" + i);
        }
        mvc.perform(put("/{tenant}/controller/v1/4717/configData", tenantAware.getCurrentTenant())
                .content(JsonBuilder.configData(attributes).toString()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        attributes = new HashMap<>();
        attributes.put("on too many", "sdsds");
        mvc.perform(put("/{tenant}/controller/v1/4717/configData", tenantAware.getCurrentTenant())
                .content(JsonBuilder.configData(attributes).toString()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.exceptionClass", equalTo(AssignmentQuotaExceededException.class.getName())))
                .andExpect(jsonPath("$.errorCode", equalTo(SpServerError.SP_QUOTA_EXCEEDED.getKey())));

    }

    @Test
    @Description("We verify that the config data (i.e. device attributes like serial number, hardware revision etc.) "
            + "resource behaves as expected in case of invalid request attempts.")
    public void badConfigData() throws Exception {
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
    public void putConfigDataWithInvalidAttributes() throws Exception {
        // create a target
        final String controllerId = "4718";
        testdataFactory.createTarget(controllerId);
        final String configDataPath = "/{tenant}/controller/v1/" + controllerId + "/configData";

        putAndVerifyConfigDataWithKeyTooLong(configDataPath);

        putAndVerifyConfigDataWithValueTooLong(configDataPath);
    }

    @Step
    private void putAndVerifyConfigDataWithKeyTooLong(final String configDataPath) throws Exception {

        final Map<String, String> attributes = Collections.singletonMap(KEY_TOO_LONG, VALUE_VALID);

        mvc.perform(put(configDataPath, tenantAware.getCurrentTenant())
                .content(JsonBuilder.configData(attributes).toString()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionClass", equalTo(InvalidTargetAttributeException.class.getName())))
                .andExpect(jsonPath("$.errorCode", equalTo(SpServerError.SP_TARGET_ATTRIBUTES_INVALID.getKey())));
    }

    @Step
    private void putAndVerifyConfigDataWithValueTooLong(final String configDataPath) throws Exception {

        final Map<String, String> attributes = Collections.singletonMap(KEY_VALID, VALUE_TOO_LONG);

        mvc.perform(put(configDataPath, tenantAware.getCurrentTenant())
                .content(JsonBuilder.configData(attributes).toString()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionClass", equalTo(InvalidTargetAttributeException.class.getName())))
                .andExpect(jsonPath("$.errorCode", equalTo(SpServerError.SP_TARGET_ATTRIBUTES_INVALID.getKey())));
    }

    @Test
    @Description("Verify that config data (device attributes) can be updated by the controller using different update modes (merge, replace, remove).")
    public void putConfigDataWithDifferentUpdateModes() throws Exception {

        // create a target
        final String controllerId = "4717";
        testdataFactory.createTarget(controllerId);
        final String configDataPath = "/{tenant}/controller/v1/" + controllerId + "/configData";

        // no update mode
        putConfigDataWithoutUpdateMode(controllerId, configDataPath);

        // update mode REPLACE
        putConfigDataWithUpdateModeReplace(controllerId, configDataPath);

        // update mode MERGE
        putConfigDataWithUpdateModeMerge(controllerId, configDataPath);

        // update mode REMOVE
        putConfigDataWithUpdateModeRemove(controllerId, configDataPath);

        // invalid update mode
        putConfigDataWithInvalidUpdateMode(configDataPath);

    }

    @Step
    private void putConfigDataWithInvalidUpdateMode(final String configDataPath) throws Exception {

        // create some attriutes
        final Map<String, String> attributes = new HashMap<>();
        attributes.put("k0", "v0");
        attributes.put("k1", "v1");

        // use an invalid update mode
        mvc.perform(put(configDataPath, tenantAware.getCurrentTenant())
                .content(JsonBuilder.configData(attributes, "KJHGKJHGKJHG").toString())
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    @Step
    private void putConfigDataWithUpdateModeRemove(final String controllerId, final String configDataPath)
            throws Exception {

        // get the current attributes
        final int previousSize = targetManagement.getControllerAttributes(controllerId).size();

        // update the attributes using update mode REMOVE
        final Map<String, String> removeAttributes = new HashMap<>();
        removeAttributes.put("k1", "foo");
        removeAttributes.put("k3", "bar");

        mvc.perform(put(configDataPath, tenantAware.getCurrentTenant())
                .content(JsonBuilder.configData(removeAttributes, "remove").toString())
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // verify attribute removal
        final Map<String, String> updatedAttributes = targetManagement.getControllerAttributes(controllerId);
        assertThat(updatedAttributes.size()).isEqualTo(previousSize - 2);
        assertThat(updatedAttributes).doesNotContainKeys("k1", "k3");

    }

    @Step
    private void putConfigDataWithUpdateModeMerge(final String controllerId, final String configDataPath)
            throws Exception {

        // get the current attributes
        final Map<String, String> attributes = new HashMap<>(targetManagement.getControllerAttributes(controllerId));

        // update the attributes using update mode MERGE
        final Map<String, String> mergeAttributes = new HashMap<>();
        mergeAttributes.put("k1", "v1_modified_again");
        mergeAttributes.put("k4", "v4");
        mvc.perform(put(configDataPath, tenantAware.getCurrentTenant())
                .content(JsonBuilder.configData(mergeAttributes, "merge").toString())
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // verify attribute merge
        final Map<String, String> updatedAttributes = targetManagement.getControllerAttributes(controllerId);
        assertThat(updatedAttributes.size()).isEqualTo(4);
        assertThat(updatedAttributes).containsAllEntriesOf(mergeAttributes);
        assertThat(updatedAttributes.get("k1")).isEqualTo("v1_modified_again");
        attributes.keySet().forEach(assertThat(updatedAttributes)::containsKey);

    }

    @Step
    private void putConfigDataWithUpdateModeReplace(final String controllerId, final String configDataPath)
            throws Exception {

        // get the current attributes
        final Map<String, String> attributes = new HashMap<>(targetManagement.getControllerAttributes(controllerId));

        // update the attributes using update mode REPLACE
        final Map<String, String> replacementAttributes = new HashMap<>();
        replacementAttributes.put("k1", "v1_modified");
        replacementAttributes.put("k2", "v2");
        replacementAttributes.put("k3", "v3");
        mvc.perform(put(configDataPath, tenantAware.getCurrentTenant())
                .content(JsonBuilder.configData(replacementAttributes, "replace").toString())
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // verify attribute replacement
        final Map<String, String> updatedAttributes = targetManagement.getControllerAttributes(controllerId);
        assertThat(updatedAttributes.size()).isEqualTo(replacementAttributes.size());
        assertThat(updatedAttributes).containsAllEntriesOf(replacementAttributes);
        assertThat(updatedAttributes.get("k1")).isEqualTo("v1_modified");
        attributes.entrySet().forEach(assertThat(updatedAttributes)::doesNotContain);

    }

    @Step
    private void putConfigDataWithoutUpdateMode(final String controllerId, final String configDataPath)
            throws Exception {

        // create some attriutes
        final Map<String, String> attributes = new HashMap<>();
        attributes.put("k0", "v0");
        attributes.put("k1", "v1");

        // set the initial attributes
        mvc.perform(put(configDataPath, tenantAware.getCurrentTenant())
                .content(JsonBuilder.configData(attributes).toString()).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        // verify the initial parameters
        final Map<String, String> updatedAttributes = targetManagement.getControllerAttributes(controllerId);
        assertThat(updatedAttributes.size()).isEqualTo(attributes.size());
        assertThat(updatedAttributes).containsAllEntriesOf(attributes);

    }

}
