/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common;

import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;

/**
 * Distribution Set Type Bean query.
 * 
 *
 *
 * 
 */
public class DistributionSetTypeBeanQuery extends AbstractBeanQuery<DistributionSetType> {

    private static final long serialVersionUID = 1329137292955215629L;

    private static final Logger LOG = LoggerFactory.getLogger(DistributionSetTypeBeanQuery.class);

    private final Sort sort = new Sort(Direction.ASC, "name");
    private transient DistributionSetTypeManagement distributionSetTypeManagement;

    /**
     * Parametric constructor.
     * 
     * @param definition
     * @param queryConfig
     * @param sortIds
     * @param sortStates
     */
    public DistributionSetTypeBeanQuery(final QueryDefinition definition, final Map<String, Object> queryConfig,
            final Object[] sortIds, final boolean[] sortStates) {
        super(definition, queryConfig, sortIds, sortStates);
    }

    @Override
    protected DistributionSetType constructBean() {
        return null;
    }

    @Override
    public int size() {

        long size = getDistributionSetManagement().count();
        if (size > Integer.MAX_VALUE) {
            size = Integer.MAX_VALUE;
        }
        return (int) size;
    }

    private DistributionSetTypeManagement getDistributionSetManagement() {
        if (distributionSetTypeManagement == null) {
            distributionSetTypeManagement = SpringContextHelper.getBean(DistributionSetTypeManagement.class);
        }
        return distributionSetTypeManagement;
    }

    @Override
    protected List<DistributionSetType> loadBeans(final int startIndex, final int count) {
        return getDistributionSetManagement().findAll(new OffsetBasedPageRequest(startIndex, count, sort)).getContent();
    }

    @Override
    protected void saveBeans(final List<DistributionSetType> arg0, final List<DistributionSetType> arg1,
            final List<DistributionSetType> arg2) {
        LOG.info("in side of save Bean()");
    }

}
