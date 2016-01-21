/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit;

import static org.fest.assertions.api.Assertions.assertThat;

import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.eclipse.hawkbit.cache.TenantAwareCacheManager;
import org.eclipse.hawkbit.repository.ActionRepository;
import org.eclipse.hawkbit.repository.ActionStatusRepository;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetRepository;
import org.eclipse.hawkbit.repository.DistributionSetTagRepository;
import org.eclipse.hawkbit.repository.DistributionSetTypeRepository;
import org.eclipse.hawkbit.repository.ExternalArtifactRepository;
import org.eclipse.hawkbit.repository.LocalArtifactRepository;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleMetadataRepository;
import org.eclipse.hawkbit.repository.SoftwareModuleRepository;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeRepository;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetInfoRepository;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetRepository;
import org.eclipse.hawkbit.repository.TargetTagRepository;
import org.eclipse.hawkbit.repository.TenantMetaDataRepository;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.utils.RepositoryDataGenerator.DatabaseCleanupUtil;
import org.eclipse.hawkbit.security.DosFilter;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.MethodRule;
import org.junit.rules.TestWatchman;
import org.junit.runner.RunWith;
import org.junit.runners.model.FrameworkMethod;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@SpringApplicationConfiguration(classes = { RepositoryApplicationConfiguration.class, TestConfiguration.class })
@ActiveProfiles({ "test" })
@WithUser(principal = "bumlux", allSpPermissions = true, authorities = "ROLE_CONTROLLER")
// destroy the context after each test class because otherwise we get problem
// when context is
// refreshed we e.g. get two instances of CacheManager which leads to very
// strange test failures.
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractIntegrationTest implements EnvironmentAware {
    protected static Logger LOG = null;

    protected static final Pageable pageReq = new PageRequest(0, 400);

    @PersistenceContext
    protected EntityManager entityManager;

    @Autowired
    protected TargetRepository targetRepository;

    @Autowired
    protected ActionRepository actionRepository;

    @Autowired
    protected DistributionSetRepository distributionSetRepository;

    @Autowired
    protected SoftwareModuleRepository softwareModuleRepository;

    @Autowired
    protected TenantMetaDataRepository tenantMetaDataRepository;

    @Autowired
    protected DistributionSetTypeRepository distributionSetTypeRepository;

    @Autowired
    protected SoftwareModuleTypeRepository softwareModuleTypeRepository;

    @Autowired
    protected TargetTagRepository targetTagRepository;

    @Autowired
    protected DistributionSetTagRepository distributionSetTagRepository;

    @Autowired
    protected SoftwareModuleMetadataRepository softwareModuleMetadataRepository;

    @Autowired
    protected ActionStatusRepository actionStatusRepository;

    @Autowired
    protected ExternalArtifactRepository externalArtifactRepository;

    @Autowired
    protected SoftwareManagement softwareManagement;

    @Autowired
    protected DistributionSetManagement distributionSetManagement;

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
    protected LocalArtifactRepository artifactRepository;

    @Autowired
    protected TargetInfoRepository targetInfoRepository;

    @Autowired
    protected GridFsOperations operations;

    @Autowired
    protected WebApplicationContext context;

    @Autowired
    protected ControllerManagement controllerManagament;

    @Autowired
    protected AuditingHandler auditingHandler;

    @Autowired
    protected TenantAware tenantAware;

    @Autowired
    protected SystemManagement systemManagement;

    @Autowired
    protected TenantAwareCacheManager cacheManager;

    protected MockMvc mvc;

    @Autowired
    protected DatabaseCleanupUtil dbCleanupUtil;

    protected SoftwareModuleType osType;
    protected SoftwareModuleType appType;
    protected SoftwareModuleType runtimeType;

    protected DistributionSetType standardDsType;

    @Rule
    public final WithSpringAuthorityRule securityRule = new WithSpringAuthorityRule();

    protected Environment environment = null;

    private static CIMySqlTestDatabase tesdatabase;

    static {
        final Properties props = System.getProperties();

        // if( props.getProperty( SuiteEmbeddedConfiguration.IM_HOME ) == null )
        // {
        // props.setProperty( SuiteEmbeddedConfiguration.IM_HOME,
        // "./src/test/resources/im" );
        // }

    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.context.EnvironmentAware#setEnvironment(org.
     * springframework.core.env. Environment)
     */
    @Override
    public void setEnvironment(final Environment environment) {
        this.environment = environment;
    }

    @Before
    public void before() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilter(new DosFilter(100, 10, "127\\.0\\.0\\.1|\\[0:0:0:0:0:0:0:1\\]", "(^192\\.168\\.)",
                        "X-Forwarded-For"))
                .addFilter(new ExcludePathAwareShallowETagFilter(
                        "/rest/v1/softwaremodules/{smId}/artifacts/{artId}/download", "/*/controller/artifacts/**"))
                .build();

        standardDsType = securityRule.runAsPrivileged(() -> systemManagement.getTenantMetadata().getDefaultDsType());

        osType = securityRule.runAsPrivileged(() -> softwareManagement.findSoftwareModuleTypeByKey("os"));
        osType.setDescription("Updated description to have lastmodified available in tests");
        osType = securityRule.runAsPrivileged(() -> softwareManagement.updateSoftwareModuleType(osType));

        appType = securityRule.runAsPrivileged(() -> softwareManagement.findSoftwareModuleTypeByKey("application"));
        appType.setDescription("Updated description to have lastmodified available in tests");
        appType = securityRule.runAsPrivileged(() -> softwareManagement.updateSoftwareModuleType(appType));

        runtimeType = securityRule.runAsPrivileged(() -> softwareManagement.findSoftwareModuleTypeByKey("runtime"));
        runtimeType.setDescription("Updated description to have lastmodified available in tests");
        runtimeType = securityRule.runAsPrivileged(() -> softwareManagement.updateSoftwareModuleType(runtimeType));
    }

    @BeforeClass
    public static void beforeClass() {
        createTestdatabaseAndStart();
    }

    private static void createTestdatabaseAndStart() {
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

    @After
    public void after() throws Exception {
        deleteAllRepos();
        cacheManager.getDirectCacheNames().forEach(name -> cacheManager.getDirectCache(name).clear());
        assertThat(actionStatusRepository.findAll()).isEmpty();
        assertThat(targetRepository.findAll()).isEmpty();
        assertThat(actionRepository.findAll()).isEmpty();
        assertThat(distributionSetRepository.findAll()).isEmpty();
        assertThat(targetTagRepository.findAll()).isEmpty();
        assertThat(distributionSetTagRepository.findAll()).isEmpty();
        assertThat(softwareModuleRepository.findAll()).isEmpty();
        assertThat(softwareModuleTypeRepository.findAll()).isEmpty();
        assertThat(distributionSetTypeRepository.findAll()).isEmpty();
        assertThat(tenantMetaDataRepository.findAll()).isEmpty();
    }

    @Transactional
    protected void deleteAllRepos() throws Exception {
        final List<String> tenants = securityRule.runAs(WithSpringAuthorityRule.withUser(false),
                () -> systemManagement.findTenants());
        tenants.forEach(tenant -> {
            try {
                securityRule.runAs(WithSpringAuthorityRule.withUser(false), () -> {
                    systemManagement.deleteTenant(tenant);
                    return null;
                });
            } catch (final Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Rule
    public MethodRule watchman = new TestWatchman() {
        @Override
        public void starting(final FrameworkMethod method) {
            if (LOG != null) {
                LOG.info("Starting Test {}...", method.getName());
            }
        }

        @Override
        public void succeeded(final FrameworkMethod method) {
            if (LOG != null) {
                LOG.info("Test {} succeeded.", method.getName());
            }
        }

        @Override
        public void failed(final Throwable e, final FrameworkMethod method) {
            if (LOG != null) {
                LOG.error("Test {} failed with {}.", method.getName(), e);
            }
        }
    };
}
