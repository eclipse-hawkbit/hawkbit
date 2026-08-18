/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ddi.rest.resource;

import static org.eclipse.hawkbit.context.AccessContext.tenant;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.ddi.json.model.DdiResult;
import org.eclipse.hawkbit.ddi.json.model.DdiStatus;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.test.util.QueryCount;
import org.eclipse.hawkbit.repository.test.util.QueryCountConfiguration;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

/**
 * Diagnostic (no assertions): drives a full DDI install flow through the real controller endpoints and prints every
 * JDBC statement per endpoint via the shared recorder.
 */
@Slf4j
@Import(QueryCountConfiguration.class)
@TestPropertySource(properties = { "logging.level.org.eclipse.hawkbit.repository.jpa.scheduler.DdiDeploymentSqlProbeTest=DEBUG" })
@Disabled("For manual run only. It drives a full DDI install flow through the real controller endpoints and prints every JDBC statement per endpoint via the shared recorder.")
class DdiDeploymentSqlProbeTest extends AbstractDDiApiIntegrationTest {

    private static final String CONTROLLER_ID = "diag-ddi";
    private static final String CONFIG_DATA = CONTROLLER_BASE + "/configData";

    @Autowired
    private QueryCount queryCount;

    @FunctionalInterface
    private interface MvcCall {

        void run() throws Exception;
    }

    @Test
    void printAllSqlPerEndpoint() throws Exception {
        // reset the query count before the setup, so that the setup queries are not included in the measured queries for the test - i.e schema creation, etc.
        queryCount.resetQueries();
        // setup (not measured): target + DS with artifacts + assignment
        testdataFactory.createTarget(CONTROLLER_ID);
        final DistributionSet ds = testdataFactory.createDistributionSet("diag");
        final long moduleId = ds.getModules().iterator().next().getId();
        testdataFactory.createArtifacts(moduleId);
        assignDistributionSet(ds.getId(), CONTROLLER_ID);
        final Action action = deploymentManagement.findActiveActionsByTarget(CONTROLLER_ID, PAGE).getContent().get(0);
        final Long actionId = action.getId();
        dump("SETUP: createTarget + createDistributionSet + createArtifacts + assign");

        path("E1 poll GET /controller/v1/{id} (assigned, before configData)",
                () -> mvc.perform(get(CONTROLLER_BASE, tenant(), CONTROLLER_ID).accept(MediaTypes.HAL_JSON))
                        .andExpect(status().isOk()));
        path("E2 PUT /configData",
                () -> mvc.perform(put(CONFIG_DATA, tenant(), CONTROLLER_ID)
                        .content(JsonBuilder.configData(Map.of("k", "v")).toString())
                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk()));
        path("E3 GET /deploymentBase/{actionId}",
                () -> performGet(DEPLOYMENT_BASE, MediaTypes.HAL_JSON, status().isOk(),
                        tenant(), CONTROLLER_ID, actionId.toString()));
        path("E4 POST /deploymentBase/{actionId}/feedback PROCEEDING",
                () -> postDeploymentFeedback(CONTROLLER_ID, actionId, getJsonProceedingDeploymentActionFeedback(), status().isOk()));
        path("E5 POST /deploymentBase/{actionId}/feedback CLOSED SUCCESS (installed)",
                () -> postDeploymentFeedback(CONTROLLER_ID, actionId,
                        getJsonActionFeedback(DdiStatus.ExecutionStatus.CLOSED, DdiResult.FinalResult.SUCCESS, List.of("installed")),
                        status().isOk()));
        path("E6 poll GET /controller/v1/{id} (after install, no deployment)",
                () -> mvc.perform(get(CONTROLLER_BASE, tenant(), CONTROLLER_ID).accept(MediaTypes.HAL_JSON))
                        .andExpect(status().isOk()));
    }

    private void path(final String name, final MvcCall body) throws Exception {
        queryCount.resetQueries();
        body.run();
        dump(name);
    }

    private void dump(final String label) {
        final var statements = queryCount.getAllStatements();
        log.warn("SQLDUMP | {} | ==== {} statement(s), {} select(s), {} sp_software_module_type ====",
                label, statements.size(), queryCount.countSelect(), queryCount.countSelectsFromTable("sp_software_module_type"));
        for (int i = 0; i < statements.size(); i++) {
            log.warn("SQLDUMP | {} | [{}] {}", label, i + 1, statements.get(i));
        }
    }
}
