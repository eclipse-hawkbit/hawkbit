/**
 * Copyright (c) 2025 Bosch Digital GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.repository.artifact.urlhandler;

import java.net.URI;
import java.util.List;

/**
 * Interface declaration of the {@link ArtifactUrlHandler} which generates the URLs to specific artifacts.
 */
public interface ArtifactUrlHandler {

    /**
     * Returns a generated download URL for a given artifact parameters for a specific protocol.
     *
     * @param placeholder data for URL generation
     * @param api given protocol that URL needs to support
     * @return a URL for the given artifact parameters in a given protocol
     */
    List<ArtifactUrl> getUrls(URLPlaceholder placeholder, ApiType api);

    /**
     * Returns a generated download URL for a given artifact parameters for a
     * specific protocol.
     *
     * @param placeholder data for URL generation
     * @param api given protocol that URL needs to support
     * @param requestUri of the request that allows the handler to align the generated URL to the original request.
     * @return a URL for the given artifact parameters in a given protocol
     */
    List<ArtifactUrl> getUrls(URLPlaceholder placeholder, ApiType api, URI requestUri);
}