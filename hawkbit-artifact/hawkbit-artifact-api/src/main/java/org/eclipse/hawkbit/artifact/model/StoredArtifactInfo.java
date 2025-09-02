/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.artifact.model;

import java.util.Objects;

import lombok.Data;

/**
 * Info for an  imported artifact binary.
 */
@Data
public class StoredArtifactInfo {

    private final String contentType;
    private final long size;
    private final ArtifactHashes hashes;

    public StoredArtifactInfo(final String contentType, final long size, final ArtifactHashes hashes) {
        this.hashes = Objects.requireNonNull(hashes, "Hashes cannot be null");
        this.contentType = contentType;
        this.size = size;
    }
}