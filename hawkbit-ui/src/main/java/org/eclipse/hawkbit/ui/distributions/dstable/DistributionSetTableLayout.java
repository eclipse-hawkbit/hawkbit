/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.dstable;

import javax.annotation.PostConstruct;

import org.eclipse.hawkbit.ui.common.table.AbstractTableLayout;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;

/**
 * DistributionSet table layout
 */
@SpringComponent
@UIScope
public class DistributionSetTableLayout extends AbstractTableLayout {

    private static final long serialVersionUID = 6464291374980641235L;

    /**
     * Details to be autowired before table as details listens to value change
     * of table.
     */
    @Autowired
    private DistributionSetDetails distributionDetails;

    @Autowired
    private DistributionSetTableHeader dsTableHeader;

    @Autowired
    private DistributionSetTable dsTable;

    /**
     * Initialize the filter layout.
     */
    @PostConstruct
    void init() {
        super.init(dsTableHeader, dsTable, distributionDetails);
    }

}
