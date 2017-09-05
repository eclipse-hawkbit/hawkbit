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

import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;

/**
 *
 *
 */
public class SoftwareModuleTypeBeanQuery extends AbstractBeanQuery<SoftwareModuleType> {
    private static final long serialVersionUID = 7824925429198339644L;

    private final Sort sort = new Sort(Direction.ASC, "name");
    private transient SoftwareModuleTypeManagement softwareModuleTypeManagement;

    /**
     * Parametric constructor.
     */
    public SoftwareModuleTypeBeanQuery(final QueryDefinition definition, final Map<String, Object> queryConfig,
            final Object[] sortIds, final boolean[] sortStates) {
        super(definition, queryConfig, sortIds, sortStates);
    }

    @Override
    protected SoftwareModuleType constructBean() {
        return null;
    }

    @Override
    public int size() {
        long size = getSoftwareModuleTypeManagement().count();
        if (size > Integer.MAX_VALUE) {
            size = Integer.MAX_VALUE;
        }
        return (int) size;
    }

    private SoftwareModuleTypeManagement getSoftwareModuleTypeManagement() {
        if (softwareModuleTypeManagement == null) {
            softwareModuleTypeManagement = SpringContextHelper.getBean(SoftwareModuleTypeManagement.class);
        }
        return softwareModuleTypeManagement;
    }

    @Override
    protected List<SoftwareModuleType> loadBeans(final int startIndex, final int count) {

        return getSoftwareModuleTypeManagement().findAll(new OffsetBasedPageRequest(startIndex, count, sort))
                .getContent();
    }

    @Override
    protected void saveBeans(final List<SoftwareModuleType> addedBeans, final List<SoftwareModuleType> modifiedBeans,
            final List<SoftwareModuleType> removedBeans) {
        // CRUD operations on Target will be done through repository methods
    }

}
