/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.artifact.urlresolver;

import java.net.URI;
import java.util.List;

/**
 * Interface declaration of the {@link ArtifactUrlResolver} which generates the URLs to specific artifacts.
 */
public interface ArtifactUrlResolver {

    /**
     * hawkBit API type.
     */
    enum ApiType {

        /**
         * Support for Device Management Federation API.
         */
        DMF,

        /**
         * Support for Direct Device Integration API.
         */
        DDI,

        /**
         * Support for Management API.
         */
        MGMT
    }

    /**
     * Returns a generated download URL for a given artifact parameters for a specific protocol.
     *
     * @param downloadDescriptor data for URL generation
     * @param api given protocol that URL needs to support
     * @return a URL for the given artifact parameters in a given protocol
     */
    List<ArtifactUrl> getUrls(DownloadDescriptor downloadDescriptor, ApiType api);

    /**
     * Returns a generated download URL for a given artifact parameters for a specific protocol.
     *
     * @param downloadDescriptor data for URL generation
     * @param api given protocol that URL needs to support
     * @param requestUri of the request that allows the handler to align the generated URL to the original request.
     * @return a URL for the given artifact parameters in a given protocol
     */
    List<ArtifactUrl> getUrls(DownloadDescriptor downloadDescriptor, ApiType api, URI requestUri);

    /**
     * Container for variables available to the {@link ArtifactUrlResolver}.
     */
    record DownloadDescriptor(String tenant, String controllerId, Long softwareModuleId, String filename, String sha1) {}
}