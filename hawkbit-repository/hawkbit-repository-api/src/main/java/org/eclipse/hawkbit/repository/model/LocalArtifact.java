/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import org.eclipse.hawkbit.artifact.repository.ArtifactRepository;

/**
 * Tenant specific locally stored artifact representation that is used by
 * {@link SoftwareModule}s . It contains all information that is provided by the
 * user while all update server generated information related to the artifact
 * (hash, length) is stored directly with the binary itself in the
 * {@link ArtifactRepository}.
 *
 */
public interface LocalArtifact extends Artifact {

    /**
     * @return the filename that was provided during upload.
     */
    String getFilename();

}
