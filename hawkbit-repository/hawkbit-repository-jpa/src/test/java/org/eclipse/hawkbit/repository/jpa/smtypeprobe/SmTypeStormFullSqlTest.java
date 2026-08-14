/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.smtypeprobe;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.test.util.QueryCount;
import org.eclipse.hawkbit.repository.test.util.QueryCountConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Diagnostic (no assertions): prints every JDBC statement per path via the shared recorder, each path in its own
 * transaction, so the SQL maps 1:1 to the Java call. Kept for ad-hoc before/after inspection; not a regression test.
 */
@Slf4j
@Import(QueryCountConfiguration.class)
class SmTypeStormFullSqlTest extends AbstractJpaIntegrationTest {

    @Autowired
    private QueryCount queryCount;

    @Autowired
    private PlatformTransactionManager txManager;

    @Test
    void printAllSqlPerPath() {
        queryCount.resetQueries();
        final DistributionSet ds = testdataFactory.createDistributionSet("diag");
        final long dsId = ds.getId();
        final long dsTypeId = ds.getType().getId();
        final long moduleId = ds.getModules().iterator().next().getId();
        final String controllerId = testdataFactory.createTarget("diag-controller").getControllerId();
        assignDistributionSet(dsId, controllerId);
        final String freshControllerId = testdataFactory.createTarget("diag-controller-fresh").getControllerId();
        final long targetTypeId = testdataFactory.createTargetType("diag-tt", java.util.Set.of(ds.getType())).getId();
        dump("SETUP: createDistributionSet + createTarget + assign + createTargetType");

        path("P1 distributionSetTypeManagement.get(dsTypeId)",
                () -> distributionSetTypeManagement.get(dsTypeId));
        path("P2 distributionSetTypeManagement.get(dsTypeId).getMandatoryModuleTypes().forEach(getKey)",
                () -> distributionSetTypeManagement.get(dsTypeId).getMandatoryModuleTypes().forEach(t -> t.getKey()));
        path("P3 distributionSetManagement.get(dsId).getType().getKey()",
                () -> distributionSetManagement.get(dsId).getType().getKey());
        path("P4 distributionSetManagement.get(dsId).getModules().forEach(m -> m.getType().getKey())",
                () -> distributionSetManagement.get(dsId).getModules().forEach(m -> m.getType().getKey()));
        path("P5 distributionSetManagement.get(dsId).isComplete()",
                () -> distributionSetManagement.get(dsId).isComplete());
        path("P6 distributionSetManagement.getValidAndComplete(dsId)",
                () -> distributionSetManagement.getValidAndComplete(dsId));
        path("P7 softwareModuleManagement.get(moduleId).getType().getKey()",
                () -> softwareModuleManagement.get(moduleId).getType().getKey());
        path("P8 controller poll: findActiveActionWithHighestWeight -> ds.getModules().getType().getKey()", () -> {
            final Action action = controllerManagement.findActiveActionWithHighestWeight(controllerId).orElseThrow();
            action.getDistributionSet().getModules().forEach(m -> m.getType().getKey());
        });
        path("P9 assignDistributionSet(dsId, freshTarget)",
                () -> assignDistributionSet(dsId, freshControllerId));
        path("P10 targetType.getDistributionSetTypes().forEach(getKey)",
                () -> targetTypeManagement.get(targetTypeId).getDistributionSetTypes().forEach(dst -> dst.getKey()));
        path("P11 distributionSetTypeManagement.get(dsTypeId).getMandatoryModuleTypeIds() [FIXED id-path]",
                () -> distributionSetTypeManagement.get(dsTypeId).getMandatoryModuleTypeIds());
        path("P12 distributionSetTypeManagement.get(dsTypeId).getOptionalModuleTypeIds() [FIXED id-path]",
                () -> distributionSetTypeManagement.get(dsTypeId).getOptionalModuleTypeIds());
    }

    private void path(final String name, final Runnable body) {
        queryCount.resetQueries();
        new TransactionTemplate(txManager).executeWithoutResult(s -> body.run());
        dump(name);
    }

    private void dump(final String label) {
        final var statements = queryCount.getAllStatements();
        log.warn("SQLDUMP | {} | ==== {} statement(s), {} sp_software_module_type ====",
                label, statements.size(), queryCount.countSelectsFromTable("sp_software_module_type"));
        for (int i = 0; i < statements.size(); i++) {
            log.warn("SQLDUMP | {} | [{}] {}", label, i + 1, statements.get(i));
        }
    }
}
