/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.url;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for the server e.g. the server's URL which must be configured.
 * 
 *
 */
@ConfigurationProperties("hawkbit.server")
public class ServerProperties {

    private String url = "http://localhost:8080";

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }
}
