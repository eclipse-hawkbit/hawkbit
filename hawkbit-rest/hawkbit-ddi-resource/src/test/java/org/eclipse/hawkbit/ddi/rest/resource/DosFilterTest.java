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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;

import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.security.DosFilter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.web.context.WebApplicationContext;

import com.google.common.net.HttpHeaders;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Test potential DOS attack scenarios and check if the filter prevents them.
 *
 */
@ActiveProfiles({ "test" })
@Feature("Component Tests - REST Security")
@Story("Denial of Service protection filter")
public class DosFilterTest extends AbstractDDiApiIntegrationTest {

    @Override
    protected DefaultMockMvcBuilder createMvcWebAppContext(final WebApplicationContext context) {
        return super.createMvcWebAppContext(context).addFilter(new DosFilter(null, 10, 10,
                "127\\.0\\.0\\.1|\\[0:0:0:0:0:0:0:1\\]", "(^192\\.168\\.)", "X-Forwarded-For"));
    }

    @Test
    @Description("Ensures that clients that are on the blacklist are forbidded ")
    public void blackListedClientIsForbidden() throws Exception {
        mvc.perform(get("/{tenant}/controller/v1/4711", tenantAware.getCurrentTenant())
                .header(HttpHeaders.X_FORWARDED_FOR, "192.168.0.4 , 10.0.0.1 ")).andExpect(status().isForbidden());
    }

    @Test
    @Description("Ensures that a READ DoS attempt is blocked ")
    public void getFloddingAttackThatisPrevented() throws Exception {

        MvcResult result = null;

        int requests = 0;
        do {
            result = mvc.perform(get("/{tenant}/controller/v1/4711", tenantAware.getCurrentTenant())
                    .header(HttpHeaders.X_FORWARDED_FOR, "10.0.0.1")).andReturn();
            requests++;

            // we give up after 1.000 requests
            assertThat(requests).isLessThan(1_000);
        } while (result.getResponse().getStatus() != HttpStatus.TOO_MANY_REQUESTS.value());

        // the filter shuts down after 100 GET requests
        assertThat(requests).isGreaterThanOrEqualTo(10);
    }

    @Test
    @Description("Ensures that an assumed READ DoS attempt is not blocked as the client (with IPv4 address) is on a whitelist")
    public void unacceptableGetLoadButOnWhitelistIPv4() throws Exception {
        for (int i = 0; i < 100; i++) {
            mvc.perform(get("/{tenant}/controller/v1/4711", tenantAware.getCurrentTenant())
                    .header(HttpHeaders.X_FORWARDED_FOR, "127.0.0.1")).andExpect(status().isOk());
        }
    }

    @Test
    @Description("Ensures that an assumed READ DoS attempt is not blocked as the client (with IPv6 address) is on a whitelist")
    public void unacceptableGetLoadButOnWhitelistIPv6() throws Exception {
        for (int i = 0; i < 100; i++) {
            mvc.perform(get("/{tenant}/controller/v1/4711", tenantAware.getCurrentTenant())
                    .header(HttpHeaders.X_FORWARDED_FOR, "0:0:0:0:0:0:0:1")).andExpect(status().isOk());
        }
    }

    @Test
    @Description("Ensures that a relatively high number of READ requests is allowed if it is below the DoS detection threshold")
    public void acceptableGetLoad() throws Exception {

        for (int x = 0; x < 3; x++) {
            // sleep for one second
            Thread.sleep(1100);
            for (int i = 0; i < 9; i++) {
                mvc.perform(get("/{tenant}/controller/v1/4711", tenantAware.getCurrentTenant())
                        .header(HttpHeaders.X_FORWARDED_FOR, "10.0.0.1")).andExpect(status().isOk());
            }
        }
    }

    @Test
    @Description("Ensures that a WRITE DoS attempt is blocked ")
    public void putPostFloddingAttackThatisPrevented() throws Exception {
        final Long actionId = prepareDeploymentBase();
        final String feedback = JsonBuilder.deploymentActionFeedback(actionId.toString(), "proceeding");

        MvcResult result = null;
        int requests = 0;
        do {
            result = mvc.perform(post("/{tenant}/controller/v1/4711/deploymentBase/" + actionId + "/feedback",
                    tenantAware.getCurrentTenant()).header(HttpHeaders.X_FORWARDED_FOR, "10.0.0.1").content(feedback)
                            .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                    .andReturn();
            requests++;

            // we give up after 500 requests
            assertThat(requests).isLessThan(500);
        } while (result.getResponse().getStatus() != HttpStatus.TOO_MANY_REQUESTS.value());

        // the filter shuts down after 10 POST requests
        assertThat(requests).isGreaterThanOrEqualTo(10);

    }

    @Test
    @Description("Ensures that a relatively high number of WRITE requests is allowed if it is below the DoS detection threshold")
    public void acceptablePutPostLoad() throws Exception {
        final Long actionId = prepareDeploymentBase();
        final String feedback = JsonBuilder.deploymentActionFeedback(actionId.toString(), "proceeding");

        for (int x = 0; x < 5; x++) {
            // sleep for one second
            Thread.sleep(1100);

            for (int i = 0; i < 9; i++) {
                mvc.perform(post("/{tenant}/controller/v1/4711/deploymentBase/" + actionId + "/feedback",
                        tenantAware.getCurrentTenant()).header(HttpHeaders.X_FORWARDED_FOR, "10.0.0.1")
                                .content(feedback).contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk());
            }
        }
    }

    private Long prepareDeploymentBase() {
        final DistributionSet ds = testdataFactory.createDistributionSet("test");
        final Target target = testdataFactory.createTarget("4711");
        final List<Target> toAssign = Collections.singletonList(target);

        assignDistributionSet(ds, toAssign);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, target.getControllerId())).hasSize(1);

        final Action uaction = deploymentManagement.findActiveActionsByTarget(PAGE, target.getControllerId())
                .getContent().get(0);

        return uaction.getId();
    }

}
