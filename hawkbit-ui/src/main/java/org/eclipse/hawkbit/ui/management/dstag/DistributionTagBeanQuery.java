/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstag;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.ui.management.tag.ProxyTag;
import org.eclipse.hawkbit.ui.management.tag.TagIdName;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;

/**
 *
 *
 */
public class DistributionTagBeanQuery extends AbstractBeanQuery<ProxyTag> {

    private static final long serialVersionUID = -4791426170440663033L;
    private final Sort sort = new Sort(Direction.ASC, "name");

    private transient DistributionSetTagManagement distributionSetTagManagement;

    /**
     * Parametric Constructor.
     * 
     * @param definition
     * @param queryConfig
     * @param sortIds
     * @param sortStates
     */
    public DistributionTagBeanQuery(final QueryDefinition definition, final Map<String, Object> queryConfig,
            final Object[] sortIds, final boolean[] sortStates) {
        super(definition, queryConfig, sortIds, sortStates);
    }

    @Override
    protected ProxyTag constructBean() {
        return new ProxyTag();
    }

    @Override
    public int size() {
        long size = getTagManagement().count();
        if (size > Integer.MAX_VALUE) {
            size = Integer.MAX_VALUE;
        }
        return (int) size;
    }

    private DistributionSetTagManagement getTagManagement() {
        if (distributionSetTagManagement == null) {
            distributionSetTagManagement = SpringContextHelper.getBean(DistributionSetTagManagement.class);
        }
        return distributionSetTagManagement;
    }

    @Override
    protected List<ProxyTag> loadBeans(final int startIndex, final int count) {
        final Slice<DistributionSetTag> dsTagBeans = getTagManagement()
                .findAll(new OffsetBasedPageRequest(startIndex, count, sort));

        return dsTagBeans.getContent().stream().map(tag -> {
            final ProxyTag proxyTargetTag = new ProxyTag();
            proxyTargetTag.setColour(tag.getColour());
            proxyTargetTag.setDescription(tag.getDescription());
            proxyTargetTag.setName(tag.getName());
            proxyTargetTag.setId(tag.getId());
            final TagIdName tagIdName = new TagIdName(tag.getName(), tag.getId());
            proxyTargetTag.setTagIdName(tagIdName);

            return proxyTargetTag;

        }).collect(Collectors.toList());
    }

    @Override
    protected void saveBeans(final List<ProxyTag> addedBeans, final List<ProxyTag> modifiedBeans,
            final List<ProxyTag> removedBeans) {
        // CRUD operations on Target will be done through repository methods
    }

}
