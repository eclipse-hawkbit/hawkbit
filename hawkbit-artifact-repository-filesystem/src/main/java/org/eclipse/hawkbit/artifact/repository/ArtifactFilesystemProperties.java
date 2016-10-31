/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the file-system repository, e.g. the base-path
 * to store the files.
 */
@ConfigurationProperties("org.eclipse.hawkbit.repository.file")
public class ArtifactFilesystemProperties {

    /**
     * The base-path of the directory to store the artifacts.
     */
    private String path = "./artifactrepo";

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }
}
