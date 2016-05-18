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

import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.jpa.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
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
public class SoftwareModuleTypeBeanQuery extends AbstractBeanQuery<SoftwareModuleType> {
    private static final long serialVersionUID = 7824925429198339644L;
    private final Sort sort = new Sort(Direction.ASC, "name");
    private transient Page<SoftwareModuleType> firstPageSwModuleType = null;
    private transient SoftwareManagement softwareManagement;

    /**
     * Parametric constructor.
     * 
     * @param definition
     * @param queryConfig
     * @param sortIds
     * @param sortStates
     */
    public SoftwareModuleTypeBeanQuery(final QueryDefinition definition, final Map<String, Object> queryConfig,
            final Object[] sortIds, final boolean[] sortStates) {
        super(definition, queryConfig, sortIds, sortStates);
    }

    @Override
    protected SoftwareModuleType constructBean() {
        return new SoftwareModuleType();
    }

    @Override
    public int size() {
        firstPageSwModuleType = getSoftwareManagement()
                .findSoftwareModuleTypesAll(new OffsetBasedPageRequest(0, SPUIDefinitions.PAGE_SIZE, sort));
        long size = firstPageSwModuleType.getTotalElements();
        if (size > Integer.MAX_VALUE) {
            size = Integer.MAX_VALUE;
        }
        return (int) size;
    }

    private SoftwareManagement getSoftwareManagement() {
        if (softwareManagement == null) {
            softwareManagement = SpringContextHelper.getBean(SoftwareManagement.class);
        }
        return softwareManagement;
    }

    @Override
    protected List<SoftwareModuleType> loadBeans(final int startIndex, final int count) {
        Page<SoftwareModuleType> swModuleTypeBeans;
        if (startIndex == 0 && firstPageSwModuleType != null) {
            swModuleTypeBeans = firstPageSwModuleType;
        } else {
            swModuleTypeBeans = getSoftwareManagement()
                    .findSoftwareModuleTypesAll(new OffsetBasedPageRequest(startIndex, count, sort));
        }
        return swModuleTypeBeans.getContent();
    }

    @Override
    protected void saveBeans(final List<SoftwareModuleType> addedBeans, final List<SoftwareModuleType> modifiedBeans,
            final List<SoftwareModuleType> removedBeans) {
        // CRUD operations on Target will be done through repository methods
    }

}
