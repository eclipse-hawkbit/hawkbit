/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.ui.management.tag.ProxyTag;
import org.eclipse.hawkbit.ui.management.tag.TagIdName;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;

/**
 *
 *
 */
public class TargetTagBeanQuery extends AbstractBeanQuery<ProxyTag> {

    private static final long serialVersionUID = -4791426170440663033L;
    private final Sort sort = new Sort(Direction.ASC, "name");
    private transient Page<TargetTag> firstPageTargetTag = null;
    private transient TargetTagManagement tagManagementService;

    /**
     * Parametric constructor.
     * 
     * @param definition
     * @param queryConfig
     * @param sortIds
     * @param sortStates
     */
    public TargetTagBeanQuery(final QueryDefinition definition, final Map<String, Object> queryConfig,
            final Object[] sortIds, final boolean[] sortStates) {
        super(definition, queryConfig, sortIds, sortStates);
    }

    @Override
    protected ProxyTag constructBean() {
        return new ProxyTag();
    }

    @Override
    public int size() {
        firstPageTargetTag = getTagManagement()
                .findAll(new OffsetBasedPageRequest(0, SPUIDefinitions.PAGE_SIZE, sort));
        long size = firstPageTargetTag.getTotalElements();
        if (size > Integer.MAX_VALUE) {
            size = Integer.MAX_VALUE;
        }
        return (int) size;
    }

    private TargetTagManagement getTagManagement() {
        if (tagManagementService == null) {
            tagManagementService = SpringContextHelper.getBean(TargetTagManagement.class);
        }
        return tagManagementService;
    }

    @Override
    protected List<ProxyTag> loadBeans(final int startIndex, final int count) {
        Page<TargetTag> targetTagBeans;
        final List<ProxyTag> targetTagList = new ArrayList<>();
        if (startIndex == 0 && firstPageTargetTag != null) {
            targetTagBeans = firstPageTargetTag;
        } else {
            targetTagBeans = getTagManagement().findAll(new OffsetBasedPageRequest(startIndex, count, sort));
        }
        for (final TargetTag tag : targetTagBeans.getContent()) {
            final ProxyTag proxyTargetTag = new ProxyTag();
            proxyTargetTag.setColour(tag.getColour());
            proxyTargetTag.setDescription(tag.getDescription());
            proxyTargetTag.setName(tag.getName());
            proxyTargetTag.setId(tag.getId());
            final TagIdName targetTagIdName = new TagIdName(tag.getName(), tag.getId());
            proxyTargetTag.setTagIdName(targetTagIdName);
            targetTagList.add(proxyTargetTag);
        }

        return targetTagList;
    }

    @Override
    protected void saveBeans(final List<ProxyTag> addedBeans, final List<ProxyTag> modifiedBeans,
            final List<ProxyTag> removedBeans) {
        // CRUD operations on Target will be done through repository methods
    }

}
