/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.test.util;

import static org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions.CONTROLLER_ROLE;
import static org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions.SYSTEM_ROLE;
import static org.junit.rules.RuleChain.outerRule;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.eclipse.hawkbit.artifact.repository.ArtifactFilesystemProperties;
import org.eclipse.hawkbit.artifact.repository.ArtifactRepository;
import org.eclipse.hawkbit.cache.TenantAwareCacheManager;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.RepositoryModelConstants;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetWithActionType;
import org.eclipse.hawkbit.repository.test.TestConfiguration;
import org.eclipse.hawkbit.repository.test.matcher.EventVerifier;
import org.eclipse.hawkbit.security.ExcludePathAwareShallowETagFilter;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.bus.ServiceMatcher;
import org.springframework.cloud.stream.test.binder.TestSupportBinderAutoConfiguration;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ActiveProfiles({ "test" })
@WithUser(principal = "bumlux", allSpPermissions = true, authorities = { CONTROLLER_ROLE, SYSTEM_ROLE })
@SpringApplicationConfiguration(classes = { TestConfiguration.class, TestSupportBinderAutoConfiguration.class })
// destroy the context after each test class because otherwise we get problem
// when context is
// refreshed we e.g. get two instances of CacheManager which leads to very
// strange test failures.
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractIntegrationTest implements EnvironmentAware {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractIntegrationTest.class);

    protected static final Pageable PAGE = new PageRequest(0, 400, new Sort(Direction.ASC, "id"));

    /**
     * Constant for MediaType HAL with encoding UTF-8. Necessary since Spring
     * version 4.3.2 @see https://jira.spring.io/browse/SPR-14577
     */
    protected static final String APPLICATION_JSON_HAL_UTF = MediaTypes.HAL_JSON + ";charset=UTF-8";

    /**
     * Number of {@link DistributionSetType}s that exist in every test case. One
     * generated by using
     * {@link TestdataFactory#findOrCreateDefaultTestDsType()} and two
     * {@link SystemManagement#getTenantMetadata()};
     */
    protected static final int DEFAULT_DS_TYPES = RepositoryConstants.DEFAULT_DS_TYPES_IN_TENANT + 1;

    @Autowired
    protected EntityFactory entityFactory;

    @Autowired
    protected SoftwareModuleManagement softwareModuleManagement;

    @Autowired
    protected SoftwareModuleTypeManagement softwareModuleTypeManagement;

    @Autowired
    protected DistributionSetManagement distributionSetManagement;

    @Autowired
    protected DistributionSetTypeManagement distributionSetTypeManagement;

    @Autowired
    protected ControllerManagement controllerManagement;

    @Autowired
    protected TargetManagement targetManagement;

    @Autowired
    protected TargetFilterQueryManagement targetFilterQueryManagement;

    @Autowired
    protected TagManagement tagManagement;

    @Autowired
    protected DeploymentManagement deploymentManagement;

    @Autowired
    protected ArtifactManagement artifactManagement;

    @Autowired
    protected WebApplicationContext context;

    @Autowired
    protected AuditingHandler auditingHandler;

    @Autowired
    protected TenantAware tenantAware;

    @Autowired
    protected SystemManagement systemManagement;

    @Autowired
    protected TenantConfigurationManagement tenantConfigurationManagement;

    @Autowired
    protected RolloutManagement rolloutManagement;

    @Autowired
    protected RolloutGroupManagement rolloutGroupManagement;

    @Autowired
    protected SystemSecurityContext systemSecurityContext;

    @Autowired
    private ArtifactFilesystemProperties artifactFilesystemProperties;

    @Autowired
    protected ArtifactRepository binaryArtifactRepository;

    @Autowired
    protected TenantAwareCacheManager cacheManager;

    @Autowired
    protected QuotaManagement quotaManagement;

    protected MockMvc mvc;

    protected SoftwareModuleType osType;
    protected SoftwareModuleType appType;
    protected SoftwareModuleType runtimeType;

    protected DistributionSetType standardDsType;

    @Autowired
    protected TestdataFactory testdataFactory;

    @Autowired
    protected ServiceMatcher serviceMatcher;

    @Rule
    // Cleaning repository will fire "delete" events. We won't count them to the
    // test execution. So there is order between both rules:
    public RuleChain ruleChain = outerRule(new CleanRepositoryRule()).around(new EventVerifier());

    @Rule
    public final WithSpringAuthorityRule securityRule = new WithSpringAuthorityRule();

    @Rule
    public TestWatcher testLifecycleLoggerRule = new TestWatcher() {

        @Override
        protected void starting(final Description description) {
            LOG.info("Starting Test {}...", description.getMethodName());
        }

        @Override
        protected void succeeded(final Description description) {
            LOG.info("Test {} succeeded.", description.getMethodName());
        }

        @Override
        protected void failed(final Throwable e, final Description description) {
            LOG.error("Test {} failed with {}.", description.getMethodName(), e);
        }
    };

    protected Environment environment = null;

    @Override
    public void setEnvironment(final Environment environment) {
        this.environment = environment;
    }

    protected DistributionSetAssignmentResult assignDistributionSet(final Long dsID, final String controllerId) {
        return deploymentManagement.assignDistributionSet(dsID, Arrays.asList(
                new TargetWithActionType(controllerId, ActionType.FORCED, RepositoryModelConstants.NO_FORCE_TIME)));
    }

    protected DistributionSetAssignmentResult assignDistributionSet(final DistributionSet pset,
            final List<Target> targets) {
        return deploymentManagement.assignDistributionSet(pset.getId(),
                targets.stream().map(Target::getTargetWithActionType).collect(Collectors.toList()));
    }

    protected DistributionSetAssignmentResult assignDistributionSet(final DistributionSet pset, final Target target) {
        return assignDistributionSet(pset, Arrays.asList(target));
    }

    protected DistributionSetMetadata createDistributionSetMetadata(final Long dsId, final MetaData md) {
        return distributionSetManagement.createDistributionSetMetadata(dsId, Collections.singletonList(md)).get(0);
    }

    protected Long getOsModule(final DistributionSet ds) {
        return ds.findFirstModuleByType(osType).get().getId();
    }

    protected Action prepareFinishedUpdate() {
        return prepareFinishedUpdate(TestdataFactory.DEFAULT_CONTROLLER_ID, "", false);
    }

    protected Action prepareFinishedUpdate(final String controllerId, final String distributionSet,
            final boolean isRequiredMigrationStep) {
        final DistributionSet ds = testdataFactory.createDistributionSet(distributionSet, isRequiredMigrationStep);
        Target savedTarget = testdataFactory.createTarget(controllerId);
        savedTarget = assignDistributionSet(ds.getId(), savedTarget.getControllerId()).getAssignedEntity().iterator()
                .next();
        Action savedAction = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);

        savedAction = controllerManagement.addUpdateActionStatus(
                entityFactory.actionStatus().create(savedAction.getId()).status(Action.Status.RUNNING));

        return controllerManagement.addUpdateActionStatus(
                entityFactory.actionStatus().create(savedAction.getId()).status(Action.Status.FINISHED));
    }

    @Before
    public void before() throws Exception {
        mvc = createMvcWebAppContext().build();
        final String description = "Updated description.";

        osType = securityRule
                .runAsPrivileged(() -> testdataFactory.findOrCreateSoftwareModuleType(TestdataFactory.SM_TYPE_OS));
        osType = securityRule.runAsPrivileged(() -> softwareModuleTypeManagement.updateSoftwareModuleType(
                entityFactory.softwareModuleType().update(osType.getId()).description(description)));

        appType = securityRule.runAsPrivileged(
                () -> testdataFactory.findOrCreateSoftwareModuleType(TestdataFactory.SM_TYPE_APP, Integer.MAX_VALUE));
        appType = securityRule.runAsPrivileged(() -> softwareModuleTypeManagement.updateSoftwareModuleType(
                entityFactory.softwareModuleType().update(appType.getId()).description(description)));

        runtimeType = securityRule
                .runAsPrivileged(() -> testdataFactory.findOrCreateSoftwareModuleType(TestdataFactory.SM_TYPE_RT));
        runtimeType = securityRule.runAsPrivileged(() -> softwareModuleTypeManagement.updateSoftwareModuleType(
                entityFactory.softwareModuleType().update(runtimeType.getId()).description(description)));

        standardDsType = securityRule.runAsPrivileged(() -> testdataFactory.findOrCreateDefaultTestDsType());
    }

    @After
    public void cleanUp() {
        try {
            FileUtils.deleteDirectory(new File(artifactFilesystemProperties.getPath()));
        } catch (final IOException | IllegalArgumentException e) {
            LOG.warn("Cannot cleanup file-directory", e);
        }
    }

    protected DefaultMockMvcBuilder createMvcWebAppContext() {
        return MockMvcBuilders.webAppContextSetup(context)
                .addFilter(new ExcludePathAwareShallowETagFilter(
                        "/rest/v1/softwaremodules/{smId}/artifacts/{artId}/download",
                        "/{tenant}/controller/v1/{controllerId}/softwaremodules/{softwareModuleId}/artifacts/**",
                        "/api/v1/downloadserver/**"));
    }

    private static CIMySqlTestDatabase tesdatabase;

    @BeforeClass
    public static void beforeClass() {
        createTestdatabaseAndStart();
    }

    private static synchronized void createTestdatabaseAndStart() {
        if ("MYSQL".equals(System.getProperty("spring.jpa.database"))) {
            tesdatabase = new CIMySqlTestDatabase();
            tesdatabase.before();
        }
    }

    @AfterClass
    public static void afterClass() {
        if (tesdatabase != null) {
            tesdatabase.after();
        }
    }
}
