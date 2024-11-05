/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.api;

import java.net.URI;
import java.util.List;

/**
 * Interface declaration of the {@link ArtifactUrlHandler} which generates the
 * URLs to specific artifacts.
 */
public interface ArtifactUrlHandler {

    /**
     * Returns a generated download URL for a given artifact parameters for a
     * specific protocol.
     *
     * @param placeholder data for URL generation
     * @param api given protocol that URL needs to support
     * @return an URL for the given artifact parameters in a given protocol
     */
    List<ArtifactUrl> getUrls(URLPlaceholder placeholder, ApiType api);

    /**
     * Returns a generated download URL for a given artifact parameters for a
     * specific protocol.
     *
     * @param placeholder data for URL generation
     * @param api given protocol that URL needs to support
     * @param requestUri of the request that allows the handler to align the generated
     *         URL to the original request.
     * @return an URL for the given artifact parameters in a given protocol
     */
    List<ArtifactUrl> getUrls(URLPlaceholder placeholder, ApiType api, URI requestUri);
}
