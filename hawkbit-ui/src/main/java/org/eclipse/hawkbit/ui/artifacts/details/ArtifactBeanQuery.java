/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.details;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;

/**
 * Simple implementation of generics bean query which dynamically loads artifact
 * beans.
 */
public class ArtifactBeanQuery extends AbstractBeanQuery<Artifact> {
    private static final long serialVersionUID = 1L;
    private Sort sort = new Sort(Direction.DESC, "filename");
    private transient ArtifactManagement artifactManagement;
    private transient Page<Artifact> firstPagetArtifacts;
    private Long baseSwModuleId;

    /**
     * Parametric Constructor.
     *
     * @param definition
     *            as Def
     * @param queryConfig
     *            as Config
     * @param sortIds
     *            as sort
     * @param sortStates
     *            as Sort status
     */
    public ArtifactBeanQuery(final QueryDefinition definition, final Map<String, Object> queryConfig,
            final Object[] sortIds, final boolean[] sortStates) {

        super(definition, queryConfig, sortIds, sortStates);

        if (HawkbitCommonUtil.isNotNullOrEmpty(queryConfig)) {
            baseSwModuleId = (Long) queryConfig.get(SPUIDefinitions.BY_BASE_SOFTWARE_MODULE);
        }

        if (!ArrayUtils.isEmpty(sortStates)) {
            sort = new Sort(sortStates[0] ? Direction.ASC : Direction.DESC, (String) sortIds[0]);

            for (int targetId = 1; targetId < sortIds.length; targetId++) {
                sort.and(new Sort(sortStates[targetId] ? Direction.ASC : Direction.DESC, (String) sortIds[targetId]));
            }
        }
    }

    @Override
    protected Artifact constructBean() {
        return null;
    }

    @Override
    protected List<Artifact> loadBeans(final int startIndex, final int count) {
        Page<Artifact> artifactBeans;
        if (startIndex == 0 && firstPagetArtifacts != null) {
            artifactBeans = firstPagetArtifacts;
        } else {
            artifactBeans = getArtifactManagement()
                    .findBySoftwareModule(new OffsetBasedPageRequest(startIndex, count, sort), baseSwModuleId);
        }

        return artifactBeans.getContent();
    }

    @Override
    protected void saveBeans(final List<Artifact> addedTargets, final List<Artifact> modifiedTargets,
            final List<Artifact> removedTargets) {
        // CRUD operations on Target will be done through repository methods
    }

    @Override
    public int size() {
        long size = 0;
        if (baseSwModuleId != null) {
            firstPagetArtifacts = getArtifactManagement()
                    .findBySoftwareModule(new PageRequest(0, SPUIDefinitions.PAGE_SIZE, sort), baseSwModuleId);
            size = firstPagetArtifacts.getTotalElements();
        }
        if (size > Integer.MAX_VALUE) {
            size = Integer.MAX_VALUE;
        }

        return (int) size;
    }

    private ArtifactManagement getArtifactManagement() {
        if (artifactManagement == null) {
            artifactManagement = SpringContextHelper.getBean(ArtifactManagement.class);
        }
        return artifactManagement;
    }
}
