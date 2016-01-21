/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.util;

import javax.servlet.MultipartConfigElement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Bean which contains all informations about the SP software, e.g. like
 * version, built time etc. from the environment.
 * 
 *
 *
 *
 */
@Component
public class SPInfo implements EnvironmentAware {

    // package private for testing purposes
    static final String UNKNOWN_VERSION = "unknown";

    static final String UNKNOWN_CREDENTIAL = "unknown credential";

    private Environment environmentData;

    @Autowired
    private MultipartConfigElement configElement;

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.context.EnvironmentAware#setEnvironment(org.
     * springframework.core.env. Environment)
     */
    @Override
    public void setEnvironment(final Environment environment) {
        this.environmentData = environment;
    }

    /**
     * @return the version in string format, e.g. 1.0.0 or {@code "UNKNOWN"} in
     *         case the SP version info cannot be determined.
     */
    public String getVersion() {
        if (environmentData != null) {
            return environmentData.getProperty("info.build.version", UNKNOWN_VERSION);
        }
        return UNKNOWN_VERSION;
    }

    public String getSupportEmail() {
        if (environmentData != null) {
            return environmentData.getProperty("hawkbit.server.email.support");
        }
        return "";
    }

    public String getRequestAccountEmail() {
        if (environmentData != null) {
            return environmentData.getProperty("hawkbit.server.email.request.account");
        }
        return "";
    }

    public String getDemoTenant() {
        if (environmentData != null) {
            return environmentData.getProperty("hawkbit.server.demo.tenant");
        }
        return UNKNOWN_CREDENTIAL;
    }

    public String getDemoUser() {
        if (environmentData != null) {
            return environmentData.getProperty("hawkbit.server.demo.user");
        }
        return UNKNOWN_CREDENTIAL;

    }

    public String getDemoPassword() {
        if (environmentData != null) {
            return environmentData.getProperty("hawkbit.server.demo.password");
        }
        return UNKNOWN_CREDENTIAL;

    }

    /**
     * @return the max file size to upload artifact files in bytes which has
     *         been configured.
     */
    public long getMaxArtifactFileSize() {
        return configElement.getMaxFileSize();
    }
}
