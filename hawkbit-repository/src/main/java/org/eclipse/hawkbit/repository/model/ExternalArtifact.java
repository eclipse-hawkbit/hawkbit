/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

public interface ExternalArtifact extends Artifact {

    ExternalArtifactProvider getExternalArtifactProvider();

    String getUrl();

    String getUrlSuffix();

    void setExternalArtifactProvider(ExternalArtifactProvider externalArtifactProvider);

    /**
     * @param urlSuffix
     *            the urlSuffix to set
     */
    void setUrlSuffix(String urlSuffix);

}