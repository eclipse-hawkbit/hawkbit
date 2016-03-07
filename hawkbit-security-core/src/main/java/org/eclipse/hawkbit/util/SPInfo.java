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
import org.springframework.stereotype.Component;

/**
 * Bean which contains all informations about the SP software, e.g. like
 * version, built time etc. from the environment.
 * 
 */
@Component
public class SPInfo {

    // package private for testing purposes
    static final String UNKNOWN_VERSION = "unknown";

    static final String UNKNOWN_CREDENTIAL = "unknown credential";

    @Autowired
    private MultipartConfigElement configElement;

    /**
     * @return the max file size to upload artifact files in bytes which has
     *         been configured.
     */
    public long getMaxArtifactFileSize() {
        return configElement.getMaxFileSize();
    }
}
