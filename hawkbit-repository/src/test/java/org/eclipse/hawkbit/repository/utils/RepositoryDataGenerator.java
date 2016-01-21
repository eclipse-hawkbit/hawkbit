/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.utils;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.Transactional;

import org.eclipse.hawkbit.TestDataUtil;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.util.IpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import net._01001111.text.LoremIpsum;

/**
 * Generates test data for setting up the repository for test or demonstration
 * purpose.
 *
 *
 *
 */
public final class RepositoryDataGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(RepositoryDataGenerator.class);

    public static void initDemoRepo(final ConfigurableApplicationContext context) {
        final PersistentInitDemoDataBuilder initDemoDataBuilder = new PersistentInitDemoDataBuilder(context);
        initDemoDataBuilder.initDemoRepo(20, 0);
    }

    public static void initLoadRepo(final ConfigurableApplicationContext context) {
        final PersistentInitDemoDataBuilder initDemoDataBuilder = new PersistentInitDemoDataBuilder(context);
        initDemoDataBuilder.initDemoRepo(200, 200);
    }

    /**
     * builder which build initial demo data and stores it to the repos.
     *
     */
    private static final class PersistentInitDemoDataBuilder {

        private final SoftwareManagement softwareManagement;
        private final TargetManagement targetManagement;
        private final DeploymentManagement deploymentManagement;
        private final TagManagement tagManagement;
        private final ControllerManagement controllerManagement;
        private final DistributionSetManagement distributionSetManagement;

        private final DatabaseCleanupUtil dbCleanupUtil;

        private final AuditingHandler auditingHandler;

        final LoremIpsum jlorem = new LoremIpsum();

        PersistentInitDemoDataBuilder(final ConfigurableApplicationContext context) {
            softwareManagement = context.getBean(SoftwareManagement.class);
            targetManagement = context.getBean(TargetManagement.class);
            tagManagement = context.getBean(TagManagement.class);
            deploymentManagement = context.getBean(DeploymentManagement.class);
            controllerManagement = context.getBean(ControllerManagement.class);
            distributionSetManagement = context.getBean(DistributionSetManagement.class);

            dbCleanupUtil = context.getBean(DatabaseCleanupUtil.class);

            auditingHandler = context.getBean(AuditingHandler.class);
        }

        private void runAsAllAuthorityContext(final Runnable runnable) {
            final SecurityContext oldContext = SecurityContextHolder.getContext();
            try {
                final SecurityContextImpl securityContextImpl = new SecurityContextImpl();
                final TestingAuthenticationToken authentication = new TestingAuthenticationToken("repogenator",
                        "repogenator", SpPermission.CREATE_REPOSITORY, SpPermission.CREATE_TARGET,
                        SpPermission.DELETE_REPOSITORY, SpPermission.DELETE_TARGET, SpPermission.READ_REPOSITORY,
                        SpPermission.READ_TARGET, SpPermission.UPDATE_REPOSITORY, SpPermission.UPDATE_TARGET,
                        SpringEvalExpressions.CONTROLLER_ROLE);
                securityContextImpl.setAuthentication(authentication);
                authentication.setDetails(new TenantAwareAuthenticationDetails("default", false));
                SecurityContextHolder.setContext(securityContextImpl);
                runnable.run();
            } finally {
                SecurityContextHolder.setContext(oldContext);
            }
        }

        public void generateTestTagetGroup(final String group, final int sizeMultiplikator) {
            final DistributionSetTag dsTag = tagManagement
                    .createDistributionSetTag(new DistributionSetTag("For " + group + "s"));

            auditingHandler.setDateTimeProvider(new DateTimeProvider() {
                @Override
                public Calendar getNow() {
                    final Calendar instance = Calendar.getInstance();
                    instance.add(Calendar.MONTH, -new Random().nextInt(7));

                    return instance;
                }
            });

            final List<Target> targets = createTargetTestGroup(group, 20 * sizeMultiplikator);

            auditingHandler.setDateTimeProvider(new DateTimeProvider() {
                @Override
                public Calendar getNow() {
                    final Calendar instance = Calendar.getInstance();
                    return instance;
                }
            });

            LOG.debug("initDemoRepo - start now real action history for group: {}", group);

            // Old history of succesfully finished operations
            IntStream.range(0, 10).forEach(idx -> {
                final DistributionSet dsReal = TestDataUtil.generateDistributionSet(group + " Release", "v1." + idx,
                        softwareManagement, distributionSetManagement,
                        Arrays.asList(new DistributionSetTag[] { dsTag }));

                final DistributionSetAssignmentResult result = deploymentManagement.assignDistributionSet(dsReal,
                        targets);

                createSimpleActionStatusHistory(result.getActions());
            });

            final List<List<Target>> targetGroups = splitIntoGroups(targets);

            IntStream.range(0, targetGroups.size()).forEach(idx -> {
                final DistributionSet dsReal = TestDataUtil.generateDistributionSet(group + " Release", "v2." + idx,
                        softwareManagement, distributionSetManagement,
                        Arrays.asList(new DistributionSetTag[] { dsTag }));

                final DistributionSetAssignmentResult result = deploymentManagement.assignDistributionSet(dsReal,
                        targetGroups.get(idx));

                createActionStatusHistory(result.getActions(), sizeMultiplikator);
            });

            LOG.debug("initDemoRepo - real action history finished for group: {}", group);
        }

        private List<List<Target>> splitIntoGroups(final List<Target> allTargets) {

            final int elements = allTargets.size();

            final int group1 = elements * (5 + new Random().nextInt(5)) / 100;
            final int group2 = elements * (15 + new Random().nextInt(5)) / 100;

            final List<List<Target>> result = new ArrayList<List<Target>>();

            result.add(allTargets.subList(0, group1));
            result.add(allTargets.subList(group1, group2));
            result.add(allTargets.subList(group2, elements));

            return result;

        }

        private void createActionStatusHistory(final List<Action> actions, final int sizeMultiplikator) {
            final AtomicInteger counter = new AtomicInteger();

            int index = 0;
            for (final Action actionGiven : actions) {
                // retrieved
                Action action = controllerManagement.registerRetrieved(actionGiven,
                        "Controller retrieved update action and should start now the download.");

                // download
                final ActionStatus download = new ActionStatus();
                download.setAction(action);
                download.setStatus(Status.DOWNLOAD);
                download.addMessage("Controller started download.");
                action = controllerManagement.addUpdateActionStatus(download, action);

                // warning
                final ActionStatus warning = new ActionStatus();
                warning.setAction(action);
                warning.setStatus(Status.WARNING);
                warning.addMessage("Some warning: " + jlorem.words(new Random().nextInt(50)));
                action = controllerManagement.addUpdateActionStatus(warning, action);

                // garbage
                for (int i = 0; i < new Random().nextInt(10); i++) {
                    final ActionStatus running = new ActionStatus();
                    running.setAction(action);
                    running.setStatus(Status.RUNNING);
                    running.addMessage("Still running: " + jlorem.words(new Random().nextInt(50)));
                    action = controllerManagement.addUpdateActionStatus(running, action);
                    for (int g = 0; g < new Random().nextInt(5); g++) {
                        final ActionStatus rand = new ActionStatus();
                        rand.setAction(action);
                        rand.setStatus(Status.RUNNING);
                        rand.addMessage(jlorem.words(new Random().nextInt(50)));
                        action = controllerManagement.addUpdateActionStatus(rand, action);
                    }
                }

                // close
                final ActionStatus close = new ActionStatus();
                close.setAction(action);

                // with error
                final int incrementAndGet = counter.incrementAndGet();
                if (incrementAndGet % 5 == 0) {
                    close.setStatus(Status.ERROR);
                    close.addMessage("Controller reported CLOSED with ERROR!");
                    action = controllerManagement.addUpdateActionStatus(close, action);
                }
                // with OK
                else {
                    close.setStatus(Status.FINISHED);
                    close.addMessage("Controller reported CLOSED with OK!");
                    action = controllerManagement.addUpdateActionStatus(close, action);
                }

                index++;
            }
        }

        private void createSimpleActionStatusHistory(final List<Action> actions) {
            for (final Action actionGiven : actions) {
                // retrieved
                Action action = controllerManagement.registerRetrieved(actionGiven,
                        "Controller retrieved update action and should start now the download.");

                // close
                final ActionStatus close = new ActionStatus();
                close.setAction(action);
                close.setStatus(Status.FINISHED);
                close.addMessage("Controller reported CLOSED with OK!");
                action = controllerManagement.addUpdateActionStatus(close, action);

            }
        }

        private List<Target> createTargetTestGroup(final String group, final int targets) {
            LOG.debug("createTargetTestGroup: create group {}", group);

            final TargetTag targTag = tagManagement.createTargetTag(new TargetTag(group));

            final List<Target> targAs = targetManagement.createTargets(buildTargets(targets, group),
                    TargetUpdateStatus.REGISTERED, System.currentTimeMillis() - new Random().nextInt(50_000_000),
                    generateIPAddress());
            LOG.debug("createTargetTestGroup: {} created", group);

            LOG.debug("createTargetTestGroup: {} targets status updated including IP", group);

            return targetManagement.toggleTagAssignment(targAs, targTag).getAssignedTargets();
        }

        private List<Target> buildTargets(final int noOfTgts, final String descriptionPrefix) {

            final List<Target> result = new ArrayList<Target>(noOfTgts);

            for (int i = 0; i < noOfTgts; i++) {
                final Target target = new Target(UUID.randomUUID().toString());

                final StringBuilder builder = new StringBuilder();
                builder.append(descriptionPrefix);
                builder.append(jlorem.words(5));

                target.setDescription(builder.toString());
                target.getTargetInfo().getControllerAttributes().put("revision", "1.1");
                target.getTargetInfo().getControllerAttributes().put("capacity", "128M");
                target.getTargetInfo().getControllerAttributes().put("serial",
                        String.valueOf(System.currentTimeMillis()));

                result.add(target);

            }

            return result;
        }

        /**
         * method writes initial test/demo data to the repositories.
         *
         * @param sizeMultiplikator
         *            the entire scenario
         * @param loadtestgroups
         *            packages of 1_000 targets
         */
        private void initDemoRepo(final int sizeMultiplikator, final int loadtestgroups) {
            final LoremIpsum jlorem = new LoremIpsum();

            runAsAllAuthorityContext(new Runnable() {

                @Override
                public void run() {
                    dbCleanupUtil.cleanupDB(null);

                    // generate targets and assign DS
                    // 5 groups - 100 targets each -> 500
                    final String[] targetTestGroups = { "SHC", "CCU", "Vehicle", "Vending machine", "ECU" };

                    final String[] modulesTypes = { "HeadUnit_FW", "EDC17_FW", "OSGi_Bundle" };

                    final DistributionSetTag depTag = tagManagement
                            .createDistributionSetTag(new DistributionSetTag("deprecated"));

                    Arrays.stream(targetTestGroups).forEach(group -> {
                        generateTestTagetGroup(group, sizeMultiplikator);
                    });

                    // garbage DS
                    LOG.debug("initDemoRepo - start now DS garbage");
                    TestDataUtil.generateDistributionSets("Generic Software Package", sizeMultiplikator,
                            softwareManagement, distributionSetManagement);
                    LOG.debug("initDemoRepo - DS garbage finished");

                    LOG.debug("initDemoRepo - start now Extra Software Modules and types");
                    Arrays.stream(modulesTypes).forEach(typeName -> {
                        final SoftwareModuleType smtype = softwareManagement.createSoftwareModuleType(
                                new SoftwareModuleType(typeName.toLowerCase().replaceAll("\\s+", ""), typeName,
                                        jlorem.words(5), Integer.MAX_VALUE));

                        for (int i = 0; i < sizeMultiplikator; i++) {
                            softwareManagement.createSoftwareModule(new SoftwareModule(smtype, typeName + i, "1.0." + i,
                                    jlorem.words(5), "the " + typeName + " vendor Inc."));
                        }

                    });
                    LOG.debug("initDemoRepo - Extra Software Modules and types finished");

                    LOG.debug("initDemoRepo - start now target garbage");

                    // garbage targets
                    // unknown
                    targetManagement
                            .createTargets(TestDataUtil.generateTargets(targetTestGroups.length * sizeMultiplikator));

                    // registered
                    targetManagement.createTargets(
                            TestDataUtil.generateTargets(targetTestGroups.length * sizeMultiplikator, "registered"),
                            TargetUpdateStatus.REGISTERED, System.currentTimeMillis(), generateIPAddress());

                    // pending
                    final DistributionSetTag dsTag = tagManagement
                            .createDistributionSetTag(new DistributionSetTag("OnlyAssignedTag"));
                    final DistributionSet ds = TestDataUtil.generateDistributionSet("Pending DS", "v1.0",
                            softwareManagement, distributionSetManagement,
                            Arrays.asList(new DistributionSetTag[] { dsTag }));
                    deploymentManagement.assignDistributionSet(ds, targetManagement.createTargets(
                            TestDataUtil.generateTargets(targetTestGroups.length * sizeMultiplikator, "pending")));

                    // Load test means additional 1_000_000 target

                    for (int i = 0; i < loadtestgroups; i++) {
                        targetManagement.createTargets(TestDataUtil.generateTargets(i * 1_000, 1_000, "loadtest-"));
                    }

                    LOG.debug("initDemoRepo complete");
                }

            });
        }
        /**
         * Adding controller attributes for given {@link Target}.
         */
        // private Target setControllerAttributes( final String targetId ) {
        // final Target target =
        // targetManagement.findTargetByControllerIDWithDetails( targetId );
        // target.getTargetStatus().getControllerAttributes().put( "revision",
        // "1.1" );
        // target.getTargetStatus().getControllerAttributes().put( "capacity",
        // "128M" );
        // target.getTargetStatus().getControllerAttributes().put( "serial",
        // String.valueOf(
        // System.currentTimeMillis()) );
        // return targetManagement.updateTarget( target );
        // }
    }

    /**
     *
     * Data clean up class.
     *
     *
     *
     */
    public static class DatabaseCleanupUtil {

        private static final Logger LOG = LoggerFactory.getLogger(DatabaseCleanupUtil.class);
        @Autowired
        private EntityManager entityManager;

        private static final String[] CLEAN_UP_SQLS = new String[] { "sp_tenant", "sp_tenant_configuration",
                "sp_artifact", "sp_external_artifact", "sp_external_provider", "sp_target_target_tag",
                "sp_target_attributes", "sp_target_tag", "sp_action_status_messages", "sp_action_status", "sp_action",
                "sp_ds_dstag", "sp_distributionset_tag", "sp_target_info", "sp_target", "sp_sw_metadata",
                "sp_ds_metadata", "sp_ds_module", "sp_distribution_set", "sp_base_software_module",
                "sp_ds_type_element", "sp_distribution_set_type", "sp_software_module_type" };

        /**
         * delete all entries from the DB tables.
         */
        @Transactional
        @Modifying
        public void cleanupDB(final String database) {
            LOG.debug("Data clean up is started...");
            final boolean isMySql = "MYSQL".equals(database);
            if (isMySql) {
                // disable foreign key check because otherwise mysql cannot
                // delete sp_active_actions due
                // the self constraint within the table, stupid MySql
                entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
            }
            try {
                final String[] dbCmds = new String[] { "delete from" };
                for (final String dbCmd : dbCmds) {
                    for (final String table : CLEAN_UP_SQLS) {
                        final String sql = String.format("%s %s", dbCmd, table);
                        final Query query = entityManager.createNativeQuery(sql);
                        try {
                            LOG.debug("cleanup table: {}", sql);
                            LOG.debug("cleaned table: {}; deleted {} records", sql, query.executeUpdate());
                        } catch (final Exception ex) {
                            LOG.error(String.format("error on executing cleanup statement '%s'", sql), ex);
                            throw ex;
                        }
                    }
                }

                LOG.debug("Data clean up is finished...");
            } finally {
                if (isMySql) {
                    // enable foreign key check again!
                    entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
                }
            }
        }
    }

    /**
     * @return a generated IPv4 address string.
     */
    private static URI generateIPAddress() {
        final Random r = new Random();
        return IpUtil
                .createHttpUri(r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(256));
    }

    private RepositoryDataGenerator() {
        super();
    }

}
