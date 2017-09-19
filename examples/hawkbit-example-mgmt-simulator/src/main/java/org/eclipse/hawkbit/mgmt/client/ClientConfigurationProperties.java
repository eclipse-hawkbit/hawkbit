/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration bean which holds the configuration of the client e.g. the base
 * URL of the hawkbit-server and the credentials to use the RESTful Management
 * API.
 */
@ConfigurationProperties(prefix = "hawkbit")
public class ClientConfigurationProperties {

    /**
     * Update server URI.
     */
    private String url = "localhost:8080";

    /**
     * Update server user name.
     */
    private String username = "admin";

    /**
     * Update server password.
     */
    private String password = "admin"; // NOSONAR this password is only used for
                                       // examples

    private final List<Scenario> scenarios = new ArrayList<>();

    /**
     * Simulation {@link Scenario}.
     *
     */
    public static class Scenario {
        private boolean cleanRepository;
        private boolean waitTillRolloutComplete = true;
        private int targets = 100;
        private int distributionSets = 10;
        private int appModulesPerDistributionSet = 2;
        private String dsName = "Package";
        private String smSwName = "Application";
        private String smFwName = "Firmware";
        private String targetName = "Device";
        private int artifactsPerSM = 1;
        private String targetAddress = "amqp:/simulator.replyTo";
        private boolean runRollouts = true;
        private boolean runSemiAutomaticRollouts = true;
        private short rolloutSuccessThreshold = 80;
        private int rolloutDeploymentGroups = 4;
        private List<String> deviceGroups = Arrays.asList("EU", "AM", "APAC");

        /**
         * Targets tags per page.
         */
        private int targetTags;

        /**
         * Distribution Set tags per set
         */
        private int dsTags = 5;

        /**
         * Artifact size. Values can use the suffixed "MB" or "KB" to indicate a
         * Megabyte or Kilobyte size.
         */
        private String artifactSize = "1MB";

        public boolean isRunSemiAutomaticRollouts() {
            return runSemiAutomaticRollouts;
        }

        public void setRunSemiAutomaticRollouts(final boolean runSemiAutomaticRollouts) {
            this.runSemiAutomaticRollouts = runSemiAutomaticRollouts;
        }

        public List<String> getDeviceGroups() {
            return deviceGroups;
        }

        public void setDeviceGroups(final List<String> deviceGroups) {
            this.deviceGroups = deviceGroups;
        }

        public boolean isCleanRepository() {
            return cleanRepository;
        }

        public void setCleanRepository(final boolean cleanRepository) {
            this.cleanRepository = cleanRepository;
        }

        public boolean isWaitTillRolloutComplete() {
            return waitTillRolloutComplete;
        }

        public void setWaitTillRolloutComplete(final boolean waitTillRolloutComplete) {
            this.waitTillRolloutComplete = waitTillRolloutComplete;
        }

        public int getRolloutDeploymentGroups() {
            return rolloutDeploymentGroups;
        }

        public void setRolloutDeploymentGroups(final int rolloutDeploymentGroups) {
            this.rolloutDeploymentGroups = rolloutDeploymentGroups;
        }

        public boolean isRunRollouts() {
            return runRollouts;
        }

        public void setRunRollouts(final boolean runRollouts) {
            this.runRollouts = runRollouts;
        }

        public String getTargetAddress() {
            return targetAddress;
        }

        public void setTargetAddress(final String targetAddress) {
            this.targetAddress = targetAddress;
        }

        public int getArtifactsPerSM() {
            return artifactsPerSM;
        }

        public void setArtifactsPerSM(final int artifactsPerSM) {
            this.artifactsPerSM = artifactsPerSM;
        }

        public String getArtifactSize() {
            return artifactSize;
        }

        public void setArtifactSize(final String artifactSize) {
            this.artifactSize = artifactSize;
        }

        public String getTargetName() {
            return targetName;
        }

        public void setTargetName(final String targetName) {
            this.targetName = targetName;
        }

        public String getDsName() {
            return dsName;
        }

        public void setDsName(final String dsName) {
            this.dsName = dsName;
        }

        public String getSmSwName() {
            return smSwName;
        }

        public void setSmSwName(final String smSwName) {
            this.smSwName = smSwName;
        }

        public String getSmFwName() {
            return smFwName;
        }

        public void setSmFwName(final String smFwName) {
            this.smFwName = smFwName;
        }

        public int getTargets() {
            return targets;
        }

        public int getDistributionSets() {
            return distributionSets;
        }

        public int getAppModulesPerDistributionSet() {
            return appModulesPerDistributionSet;
        }

        public void setTargets(final int targets) {
            this.targets = targets;
        }

        public void setDistributionSets(final int distributionSets) {
            this.distributionSets = distributionSets;
        }

        public void setAppModulesPerDistributionSet(final int appModulesPerDistributionSet) {
            this.appModulesPerDistributionSet = appModulesPerDistributionSet;
        }

        public void setTargetTags(final int targetTags) {
            this.targetTags = targetTags;
        }

        public int getTargetTags() {
            return targetTags;
        }

        public int getDsTags() {
            return dsTags;
        }

        public void setDsTags(final int dsTags) {
            this.dsTags = dsTags;
        }

        public short getRolloutSuccessThreshold() {
            return rolloutSuccessThreshold;
        }

        public void setRolloutSuccessThreshold(final short rolloutSuccessThreshold) {
            this.rolloutSuccessThreshold = rolloutSuccessThreshold;
        }

    }

    public List<Scenario> getScenarios() {
        return scenarios;
    }

    public void addScenarios(final Scenario scenario) {
        scenarios.add(scenario);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }
}
