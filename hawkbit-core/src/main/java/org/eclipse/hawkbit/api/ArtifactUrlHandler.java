/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.api;

/**
 * Interface declaration of the {@link ArtifactUrlHandler} which generates the
 * URLs to specific artifacts.
 *
 */
@FunctionalInterface
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
     *            the filename of the artifact
     * @param sha1Hash
     *            the sha1Hash of the artifact
     * @param protocol
     *            the protocol the URL should be generated
     * @return an URL for the given artifact parameters in a given protocol
     */
    String getUrl(String controllerId, final Long softwareModuleId, final String filename, final String sha1Hash,
            final UrlProtocol protocol);
}
