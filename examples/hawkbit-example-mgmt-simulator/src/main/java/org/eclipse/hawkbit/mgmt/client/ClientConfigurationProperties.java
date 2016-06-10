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

    public static class Scenario {
        private int targets = 100;
        private int distributionSets = 10;
        private int appModulesPerDistributionSet = 2;
        private String dsName = "Package";
        private String smSwName = "Application";
        private String smFwName = "Firmware";
        private String targetName = "Device";

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

    }

    public List<Scenario> getScenarios() {
        return scenarios;
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
