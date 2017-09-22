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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.Test;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import com.google.common.collect.Maps;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Test config data from the controller.
 */
@ActiveProfiles({ "im", "test" })
@Features("Component Tests - Direct Device Integration API")
@Stories("Config Data Resource")
public class DdiConfigDataTest extends AbstractDDiApiIntegrationTest {

    @Test
    @Description("We verify that the config data (i.e. device attributes like serial number, hardware revision etc.) "
            + "are requested only once from the device.")
    public void requestConfigDataIfEmpty() throws Exception {
        final Target savedTarget = testdataFactory.createTarget("4712");

        final long current = System.currentTimeMillis();
        mvc.perform(
                get("/{tenant}/controller/v1/4712", tenantAware.getCurrentTenant()).accept(APPLICATION_JSON_HAL_UTF))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_HAL_UTF))
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
                .updateControllerAttributes(savedTarget.getControllerId(), attributes);
        // request controller attributes need to be false because we don't want
        // to request the
        // controller attributes again
        assertThat(updateControllerAttributes.isRequestControllerAttributes()).isFalse();

        mvc.perform(
                get("/{tenant}/controller/v1/4712", tenantAware.getCurrentTenant()).accept(APPLICATION_JSON_HAL_UTF))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_HAL_UTF))
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
        attributes.put("dsafsdf", "sdsds");

        mvc.perform(put("/{tenant}/controller/v1/4717/configData", tenantAware.getCurrentTenant())
                .content(JsonBuilder.configData("", attributes, "closed")).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        assertThat(targetManagement.getControllerAttributes("4717")).isEqualTo(attributes);

        // update
        attributes.put("sdsds", "123412");
        mvc.perform(put("/{tenant}/controller/v1/4717/configData", tenantAware.getCurrentTenant())
                .content(JsonBuilder.configData("", attributes, "closed")).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        assertThat(targetManagement.getControllerAttributes("4717")).isEqualTo(attributes);
    }

    @Test
    @Description("We verify that the config data (i.e. device attributes like serial number, hardware revision etc.) "
            + "upload limitation is inplace which is meant to protect the server from malicious attempts.")
    public void putToMuchConfigData() throws Exception {
        testdataFactory.createTarget("4717");

        // initial
        Map<String, String> attributes = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            attributes.put("dsafsdf" + i, "sdsds" + i);
        }
        mvc.perform(put("/{tenant}/controller/v1/4717/configData", tenantAware.getCurrentTenant())
                .content(JsonBuilder.configData("", attributes, "closed")).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        attributes = new HashMap<>();
        attributes.put("on too many", "sdsds");
        mvc.perform(put("/{tenant}/controller/v1/4717/configData", tenantAware.getCurrentTenant())
                .content(JsonBuilder.configData("", attributes, "closed")).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

    }

    @Test
    @Description("We verify that the config data (i.e. device attributes like serial number, hardware revision etc.) "
            + "resource behaves as exptected in cae of invalid request attempts.")
    public void badConfigData() throws Exception {
        final Target savedTarget = testdataFactory.createTarget("4712");

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
                .content(JsonBuilder.configData("", attributes, "closed")).contentType(MediaTypes.HAL_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isUnsupportedMediaType());

        // non existing target
        mvc.perform(put("/{tenant}/controller/v1/456456/configData", tenantAware.getCurrentTenant())
                .content(JsonBuilder.configData("", attributes, "closed")).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // bad body
        mvc.perform(put("/{tenant}/controller/v1/4712/configData", tenantAware.getCurrentTenant())
                .content("{\"id\": \"51659181\"}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
    }
}
