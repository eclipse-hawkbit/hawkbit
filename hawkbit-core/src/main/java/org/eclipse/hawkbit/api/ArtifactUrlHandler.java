/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.api;

import java.util.List;

/**
 * Interface declaration of the {@link ArtifactUrlHandler} which generates the
 * URLs to specific artifacts.
 *
 */
public interface ArtifactUrlHandler {

    /**
     * Returns a generated download URL for a given artifact parameters for a
     * specific protocol.
     *
     * @param controllerId
     *            the authenticated controller id
     * @param softwareModuleId
     *            the softwareModuleId belonging to the artifact
     * @param filename
     *            the filename of the artifact *
     * @param sha1Hash
     *            the sha1Hash of the artifact
     * @param artifactid
     *            the ID of the artifact
     * 
     * @return an URL for the given artifact parameters in a given protocol
     */
    List<ArtifactUrl> getUrls(String controllerId, Long softwareModuleId, String filename, String sha1Hash,
            Long artifactid, APIType api);
}
