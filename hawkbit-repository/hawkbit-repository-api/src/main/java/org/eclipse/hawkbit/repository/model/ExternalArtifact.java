/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.net.URL;

/**
 * External artifact representation with all the necessary information to
 * generate an artifact {@link URL} at runtime.
 *
 */
public interface ExternalArtifact extends Artifact {

    /**
     * @return {@link ExternalArtifactProvider} of this {@link Artifact}.
     */
    ExternalArtifactProvider getExternalArtifactProvider();

    /**
     * @return generated download {@link URL}.
     */
    String getUrl();

    /**
     * @return suffix for {@link URL} generation.
     */
    String getUrlSuffix();

    /**
     * @param urlSuffix
     *            the urlSuffix to set
     */
    void setUrlSuffix(String urlSuffix);

}
