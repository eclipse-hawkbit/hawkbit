/**
 * Copyright (c) Siemens AG, 2017
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.offline.update.util;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Auto-configuration which loads the property configuration for offline
 * updates extension for hawkBit.
 */
@Configuration
@PropertySource("classpath:/offlineUpdate.properties")
@ConfigurationProperties("offline.update")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PropertiesAutoConfiguration {

    /**
     * Default prefix for Distribution set name for offline software update.
     * This is an identifier to distinguish the Distribution set uploaded
     * through Offline Update API.
     */
    private String distributionSetPrefix = "DS_";

    /**
     * Default version for a Distribution set for offline software update.
     */
    private String distributionSetVersion = "1.0.0";

    /**
     * Returns the distribution set prefix.
     *
     * @return distributionSetPrefix
     */
    public String getDistributionSetPrefix() {
        return distributionSetPrefix;
    }

    /**
     * Sets the distribution set prefix.
     *
     * @param distributionSetPrefix.
     */
    public void setDistributionSetPrefix(String distributionSetPrefix) {
        this.distributionSetPrefix = distributionSetPrefix;
    }

    /**
     * Returns the default distribution set version.
     *
     * @return distributionSetVersion.
     */
    public String getDistributionSetVersion() {
        return distributionSetVersion;
    }

    /**
     * Sets the default distribution set version.
     *
     * @param distributionSetVersion.
     */
    public void setDistributionSetVersion(String distributionSetVersion) {
        this.distributionSetVersion = distributionSetVersion;
    }
}
