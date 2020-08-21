/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.mappers;

import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyArtifact;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;

/**
 * Maps {@link Artifact} entities, fetched from backend, to the
 * {@link ProxyArtifact} entities.
 */
public class ArtifactToProxyArtifactMapper
        implements IdentifiableEntityToProxyIdentifiableEntityMapper<ProxyArtifact, Artifact> {

    @Override
    public ProxyArtifact map(final Artifact artifact) {
        final ProxyArtifact proxyArtifact = new ProxyArtifact();

        proxyArtifact.setId(artifact.getId());
        proxyArtifact.setFilename(artifact.getFilename());
        proxyArtifact.setMd5Hash(artifact.getMd5Hash());
        proxyArtifact.setSha1Hash(artifact.getSha1Hash());
        proxyArtifact.setSha256Hash(artifact.getSha256Hash());
        proxyArtifact.setSize(artifact.getSize());
        proxyArtifact.setModifiedDate(SPDateTimeUtil.getFormattedDate(artifact.getLastModifiedAt()));

        return proxyArtifact;
    }
}
