/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

/**
 * External repositories for artifact storage. The update server provides URLs
 * for the targets to download from these external resources but does not access
 * them itself.
 *
 */
public interface ExternalArtifactProvider extends NamedEntity {

    /**
     * @return prefix for url generation
     */
    String getBasePath();

    /**
     * @return default for {@link ExternalArtifact#getUrlSuffix()}.
     */
    String getDefaultSuffix();

    /**
     * @param basePath
     *            prefix for url generation
     */
    void setBasePath(String basePath);

    /**
     * @param defaultSuffix
     *            for {@link ExternalArtifact#getUrlSuffix()}.
     */
    void setDefaultSuffix(String defaultSuffix);

}
