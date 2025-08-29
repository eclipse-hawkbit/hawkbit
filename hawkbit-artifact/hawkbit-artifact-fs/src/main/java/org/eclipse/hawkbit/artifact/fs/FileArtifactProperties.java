/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.artifact.fs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the file-system repository, e.g. the base-path to store the files.
 */
@Data
@ConfigurationProperties("org.eclipse.hawkbit.repository.file")
public class FileArtifactProperties {

    /**
     * The base-path of the directory to store the artifacts.
     */
    private String path = "./artifactrepo";
}