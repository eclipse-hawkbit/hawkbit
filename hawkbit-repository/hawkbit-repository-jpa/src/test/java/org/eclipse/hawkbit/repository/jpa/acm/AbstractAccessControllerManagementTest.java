/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.acm;

import java.util.List;
import java.util.Set;

import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "hawkbit.acm.access-controller.enabled=true")
abstract class AbstractAccessControllerManagementTest extends AbstractJpaIntegrationTest {

    protected SoftwareModuleType smType1;
    protected SoftwareModuleType smType2;
    protected SoftwareModule sm1Type1;
    protected SoftwareModule sm2Type2;
    protected SoftwareModule sm3Type2;

    protected DistributionSetType dsType1;
    protected DistributionSetType dsType2;
    protected DistributionSetTag dsTag1;
    protected DistributionSetTag dsTag2;
    protected DistributionSet ds1Type1;
    protected DistributionSet ds2Type2;
    protected DistributionSet ds3Type2;

    protected TargetType targetType1;
    protected TargetType targetType2;
    protected TargetTag targetTag1;
    protected TargetTag targetTag2;
    protected Target target1Type1;
    protected Target target2Type2;
    protected Target target3Type2;

    @SuppressWarnings("java:S1117") // java:S1117 - intentional hiding of fiends - they are the same
    @BeforeEach
    @Override
    public void beforeAll() throws Exception {
        super.beforeAll();

        smType1 = testdataFactory.findOrCreateSoftwareModuleType("SmType1");
        smType2 = testdataFactory.findOrCreateSoftwareModuleType("SmType2");
        sm1Type1 = softwareModuleManagement.lock(testdataFactory.createSoftwareModule(smType1.getKey()));
        sm2Type2 = softwareModuleManagement.lock(testdataFactory.createSoftwareModule(smType2.getKey()));
        sm3Type2 = softwareModuleManagement.lock(testdataFactory.createSoftwareModule(smType2.getKey()));

        dsType1 = testdataFactory.findOrCreateDistributionSetType("DsType1", "DistributionSetType-1", List.of(smType1), List.of());
        dsType2 = testdataFactory.findOrCreateDistributionSetType("DsType2", "DistributionSetType-2", List.of(smType2), List.of(smType1));
        final List<DistributionSetTag> dsTags = testdataFactory.createDistributionSetTags(2);
        dsTag1 = dsTags.get(0);
        dsTag2 = dsTags.get(1);
        ds1Type1 = distributionSetManagement.lock(
                distributionSetManagement.assignTag(
                        List.of(testdataFactory.createDistributionSet("Ds1Type1", "1.0", dsType1, List.of(sm1Type1)).getId()),
                        dsTag1.getId()).get(0));
        ds2Type2 = distributionSetManagement.lock(
                distributionSetManagement.assignTag(
                        List.of(testdataFactory.createDistributionSet("Ds2Type2", "2.0", dsType2, List.of(sm2Type2, sm1Type1)).getId()),
                        dsTag1.getId()).get(0));
        ds3Type2 = distributionSetManagement.lock(
                distributionSetManagement.assignTag(
                        List.of(testdataFactory.createDistributionSet("Ds3Type2", "3.0", dsType2, List.of(sm3Type2, sm1Type1)).getId()),
                        dsTag2.getId()).get(0));

        targetType1 = testdataFactory.createTargetType("TargetType1", Set.of(dsType1, dsType2));
        targetType2 = testdataFactory.createTargetType("TargetType2", Set.of(dsType2));
        final List<? extends TargetTag> targetTags = testdataFactory.createTargetTags(2, "acm_");
        targetTag1 = targetTags.get(0);
        targetTag2 = targetTags.get(1);
        final Target target1Type1 = testdataFactory.createTarget("controller_1", "Controller-1", targetType1);
        final Target target2Type2 = testdataFactory.createTarget("controller_2", "Controller-2", targetType2);
        final Target target3Type2 = testdataFactory.createTarget("controller_3", "Controller-3", targetType2);
        targetManagement.assignTag(List.of(target1Type1.getControllerId(), target2Type2.getControllerId()), targetTag1.getId());
        targetManagement.assignTag(
                List.of(target1Type1.getControllerId(), target2Type2.getControllerId(), target3Type2.getControllerId()), targetTag2.getId());
        this.target1Type1 = targetManagement.get(target1Type1.getId());
        this.target2Type2 = targetManagement.get(target2Type2.getId());
        this.target3Type2 = targetManagement.get(target3Type2.getId());
    }

    protected static WithUser withAuthorities(final String... authorities) {
        AuthorityChecker.validateAuthorities(authorities);
        return SecurityContextSwitch.withUser("user", authorities);
    }
}