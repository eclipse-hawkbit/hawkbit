/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.providers;

import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.common.data.mappers.ArtifactToProxyArtifactMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyArtifact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

/**
 * Data provider for {@link Artifact}, which dynamically loads a batch of
 * {@link Artifact} entities from backend and maps them to corresponding
 * {@link ProxyArtifact} entities.The filter is used for master-details
 * relationship with {@link SoftwareModule}, using its id.
 */
public class ArtifactDataProvider extends AbstractProxyDataProvider<ProxyArtifact, Artifact, Long> {
    private static final long serialVersionUID = 1L;

    private final transient ArtifactManagement artifactManagement;

    /**
     * Constructor for ArtifactDataProvider
     *
     * @param artifactManagement
     *          ArtifactManagement
     * @param entityMapper
     *          ArtifactToProxyArtifactMapper
     */
    public ArtifactDataProvider(final ArtifactManagement artifactManagement,
            final ArtifactToProxyArtifactMapper entityMapper) {
        super(entityMapper, Sort.by(Direction.DESC, "filename"));

        this.artifactManagement = artifactManagement;
    }

    @Override
    protected Page<Artifact> loadBackendEntities(final PageRequest pageRequest, final Long smId) {
        if (smId == null) {
            return Page.empty(pageRequest);
        }

        return artifactManagement.findBySoftwareModule(pageRequest, smId);
    }

    @Override
    protected long sizeInBackEnd(final PageRequest pageRequest, final Long smId) {
        if (smId == null) {
            return 0L;
        }

        return loadBackendEntities(pageRequest, smId).getTotalElements();
    }
}
